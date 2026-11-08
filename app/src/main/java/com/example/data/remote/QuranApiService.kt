package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.AlHadithiDao
import com.example.data.local.CachedAyahEntity
import com.example.domain.SurahCatalog
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class AlQuranSurahResponse(
    val code: Int = 200,
    val data: AlQuranSurahData? = null
)

data class AlQuranSurahData(
    val number: Int = 1,
    val name: String = "",
    val ayahs: List<AlQuranAyahDto> = emptyList()
)

data class AlQuranAyahDto(
    val number: Int = 1,
    val text: String = "",
    val numberInSurah: Int = 1,
    val juz: Int = 1,
    val hizbQuarter: Int = 1,
    val sajda: Any? = null
)

data class AlQuranSearchResponse(
    val code: Int = 200,
    val data: AlQuranSearchData? = null
)

data class AlQuranSearchData(
    val count: Int = 0,
    val matches: List<AlQuranSearchMatch> = emptyList()
)

data class AlQuranSearchMatch(
    val number: Int = 1,
    val text: String = "",
    val numberInSurah: Int = 1,
    val surah: AlQuranMatchSurah? = null
)

data class AlQuranMatchSurah(
    val number: Int = 1,
    val name: String = ""
)

interface QuranApiService {
    @GET("surah/{number}/quran-uthmani")
    suspend fun getSurahUthmani(@Path("number") surahNumber: Int): AlQuranSurahResponse

    @GET("search/{keyword}/all/quran-uthmani")
    suspend fun searchQuranKeyword(@Path("keyword") keyword: String): AlQuranSearchResponse
}

object QuranNetworkRepository {
    private const val TAG = "AlHadithiQuran"
    private const val BUNDLED_QURAN_ASSET = "quran_uthmani_full.json"
    private const val EXPECTED_SURAH_COUNT = 114
    private const val EXPECTED_AYAH_COUNT = 6_236

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: QuranApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.alquran.cloud/v1/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(QuranApiService::class.java)
    }

    private val assetMutex = Mutex()
    @Volatile
    private var bundledSurahsMap: Map<Int, List<CachedAyahEntity>>? = null

    private val arabicDiacriticsRegex = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\uFEFF]")

    private fun normalizeArabicForSearch(input: String): String {
        return input
            .replace(arabicDiacriticsRegex, "")
            .replace('ٱ', 'ا')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .trim()
    }

    /**
     * Loads the bundled Quran only after checking all 114 surahs and 6,236 ayahs.
     * Empty, truncated, duplicated, or structurally incorrect assets are rejected.
     */
    suspend fun ensureBundledQuranLoaded(context: Context): Map<Int, List<CachedAyahEntity>> {
        bundledSurahsMap?.let { return it }
        return assetMutex.withLock {
            bundledSurahsMap?.let { return@withLock it }
            val parsed = withContext(Dispatchers.IO) {
                try {
                    val rawJson = context.applicationContext.assets
                        .open(BUNDLED_QURAN_ASSET)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    require(rawJson.isNotBlank()) { "Bundled Quran asset is empty" }

                    val rootArray = JSONArray(rawJson)
                    require(rootArray.length() == EXPECTED_SURAH_COUNT) {
                        "Bundled Quran must contain exactly $EXPECTED_SURAH_COUNT surahs"
                    }

                    val map = LinkedHashMap<Int, List<CachedAyahEntity>>(EXPECTED_SURAH_COUNT)
                    var expectedGlobalAyah = 1

                    for (i in 0 until rootArray.length()) {
                        val surahObj = rootArray.getJSONObject(i)
                        val surahNumber = surahObj.getInt("s")
                        require(surahNumber == i + 1) { "Surah sequence is invalid at index $i" }
                        require(!map.containsKey(surahNumber)) { "Duplicate surah number: $surahNumber" }

                        val meta = SurahCatalog.byNumber(surahNumber)
                        val ayahsArr = surahObj.getJSONArray("a")
                        require(ayahsArr.length() == meta.ayahCount) {
                            "Surah $surahNumber has ${ayahsArr.length()} ayahs; expected ${meta.ayahCount}"
                        }

                        val list = ArrayList<CachedAyahEntity>(meta.ayahCount)
                        for (j in 0 until ayahsArr.length()) {
                            val tuple = ayahsArr.getJSONArray(j)
                            require(tuple.length() >= 6) { "Ayah tuple is incomplete in surah $surahNumber" }

                            val ayahInSurah = tuple.getInt(0)
                            val globalNum = tuple.getInt(1)
                            val juzNum = tuple.getInt(2)
                            val hizbQ = tuple.getInt(3)
                            val sajdaValue = tuple.getInt(4)
                            val rawText = tuple.getString(5).removePrefix("\uFEFF").trim()

                            require(ayahInSurah == j + 1) { "Ayah order is invalid in surah $surahNumber" }
                            require(globalNum == expectedGlobalAyah) { "Global ayah numbering is invalid at $expectedGlobalAyah" }
                            require(juzNum in 1..30) { "Invalid juz number for ayah $globalNum" }
                            require(hizbQ in 1..240) { "Invalid hizb quarter for ayah $globalNum" }
                            require(sajdaValue == 0 || sajdaValue == 1) { "Invalid sajda flag for ayah $globalNum" }
                            require(rawText.isNotBlank()) { "Empty Quran text at ayah $globalNum" }
                            require(rawText.any { it in '\u0600'..'\u06FF' }) {
                                "Non-Arabic text found at ayah $globalNum"
                            }

                            val cleanedText = if (surahNumber != 1 && surahNumber != 9 && ayahInSurah == 1) {
                                rawText.removePrefix("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ")
                                    .removePrefix("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ")
                                    .trim()
                            } else {
                                rawText
                            }
                            require(cleanedText.isNotBlank()) { "Ayah text became empty after normalization" }

                            list.add(
                                CachedAyahEntity(
                                    surahNumber = surahNumber,
                                    surahNameArabic = meta.arabicName,
                                    ayahNumberInSurah = ayahInSurah,
                                    globalAyahNumber = globalNum,
                                    uthmaniText = cleanedText,
                                    juz = juzNum,
                                    hizbQuarter = hizbQ,
                                    hasSajdah = sajdaValue == 1,
                                    tafsirSummary = defaultShiaTafsirReferenceNote(meta.arabicName, ayahInSurah),
                                    tafsirSource = "الميزان في تفسير القرآن للطباطبائي · مجمع البيان للطبرسي · الأمثل للشيرازي"
                                )
                            )
                            expectedGlobalAyah++
                        }
                        map[surahNumber] = list
                    }

                    require(map.size == EXPECTED_SURAH_COUNT) { "Bundled Quran does not contain all surahs" }
                    require(expectedGlobalAyah == EXPECTED_AYAH_COUNT + 1) {
                        "Bundled Quran must contain exactly $EXPECTED_AYAH_COUNT ayahs"
                    }
                    map
                } catch (e: Exception) {
                    Log.w(TAG, "No complete verified 114-surah offline Quran corpus is packaged; the app will use its network source and local per-surah cache", e)
                    emptyMap()
                }
            }
            if (parsed.size == EXPECTED_SURAH_COUNT) {
                bundledSurahsMap = parsed
            }
            parsed
        }
    }

    /**
     * Loads a verified bundled Quran only when the full 114-surah corpus is present.
     * Otherwise it downloads the requested Uthmani surah and stores it in Room for later offline reading.
     */
    suspend fun loadOrFetchSurah(
        surahNumber: Int,
        dao: AlHadithiDao,
        context: Context? = null
    ): Result<List<CachedAyahEntity>> = withContext(Dispatchers.IO) {
        if (surahNumber !in 1..EXPECTED_SURAH_COUNT) {
            return@withContext Result.failure(IllegalArgumentException("رقم السورة غير صالح"))
        }
        val meta = SurahCatalog.byNumber(surahNumber)
        val existing = dao.getCachedSurahAyahsOnce(surahNumber)
        if (existing.size >= meta.ayahCount) {
            return@withContext Result.success(existing)
        }

        val existingMap = existing.associateBy { it.ayahNumberInSurah }

        // 1. Use a bundled mushaf only when a complete, validated corpus exists.
        if (context != null) {
            val bundledMap = ensureBundledQuranLoaded(context)
            val bundledSurah = bundledMap[surahNumber]
            if (!bundledSurah.isNullOrEmpty()) {
                val merged = bundledSurah.map { ayah ->
                    val prev = existingMap[ayah.ayahNumberInSurah]
                    if (prev != null && prev.tafsirSummary.isNotBlank()) {
                        ayah.copy(
                            tafsirSummary = prev.tafsirSummary,
                            tafsirSource = prev.tafsirSource.ifBlank { ayah.tafsirSource }
                        )
                    } else {
                        ayah
                    }
                }
                dao.insertCachedAyahs(merged)
                return@withContext Result.success(merged)
            }
        }

        // 2. Network fallback only if context was omitted or asset unavailable
        try {
            val res = api.getSurahUthmani(surahNumber)
            val dtos = res.data?.ayahs.orEmpty()
            if (dtos.size == meta.ayahCount && dtos.indices.all { index ->
                    dtos[index].numberInSurah == index + 1 && dtos[index].text.isNotBlank() && dtos[index].number > 0
                }
            ) {
                val mapped = dtos.map { dto ->
                    val prev = existingMap[dto.numberInSurah]
                    val cleanedText = if (surahNumber != 1 && surahNumber != 9 && dto.numberInSurah == 1) {
                        dto.text.removePrefix("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ").trim()
                    } else {
                        dto.text
                    }
                    val isSajda = when (val s = dto.sajda) {
                        is Boolean -> s
                        is Map<*, *> -> true
                        else -> false
                    }
                    CachedAyahEntity(
                        surahNumber = surahNumber,
                        surahNameArabic = meta.arabicName,
                        ayahNumberInSurah = dto.numberInSurah,
                        globalAyahNumber = dto.number,
                        uthmaniText = cleanedText,
                        juz = dto.juz,
                        hizbQuarter = dto.hizbQuarter,
                        hasSajdah = isSajda,
                        tafsirSummary = prev?.tafsirSummary ?: defaultShiaTafsirReferenceNote(meta.arabicName, dto.numberInSurah),
                        tafsirSource = prev?.tafsirSource ?: "الميزان في تفسير القرآن للطباطبائي · مجمع البيان للطبرسي · الأمثل للشيرازي"
                    )
                }
                dao.insertCachedAyahs(mapped)
                return@withContext Result.success(mapped)
            }
        } catch (e: Exception) {
            if (existing.isNotEmpty()) {
                return@withContext Result.success(existing)
            }
            return@withContext Result.failure(e)
        }

        if (existing.isNotEmpty()) Result.success(existing)
        else Result.failure(IllegalStateException("تعذر تحميل السورة"))
    }

    /**
     * Searches cached/local Quran text first and the network's Uthmani script when needed.
     * Full offline search is available only after a complete verified Quran corpus is bundled.
     */
    suspend fun searchQuran(
        keyword: String,
        dao: AlHadithiDao,
        context: Context? = null
    ): List<CachedAyahEntity> = withContext(Dispatchers.IO) {
        val cleanQuery = normalizeArabicForSearch(keyword)
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val localMatches = dao.searchCachedAyahs(keyword)
        if (context != null) {
            val bundledMap = ensureBundledQuranLoaded(context)
            if (bundledMap.isNotEmpty()) {
                val offlineHits = ArrayList<CachedAyahEntity>(50)
                outer@ for ((_, ayahs) in bundledMap) {
                    for (ayah in ayahs) {
                        if (normalizeArabicForSearch(ayah.uthmaniText).contains(cleanQuery) ||
                            normalizeArabicForSearch(ayah.surahNameArabic).contains(cleanQuery)
                        ) {
                            offlineHits.add(ayah)
                            if (offlineHits.size >= 50) break@outer
                        }
                    }
                }
                return@withContext (localMatches + offlineHits)
                    .distinctBy { "${it.surahNumber}:${it.ayahNumberInSurah}" }
                    .take(50)
            }
        }

        try {
            val remote = api.searchQuranKeyword(keyword.trim())
            val remoteMapped = remote.data?.matches.orEmpty().take(35).map { m ->
                val sNum = m.surah?.number ?: 1
                val sMeta = SurahCatalog.byNumber(sNum)
                CachedAyahEntity(
                    surahNumber = sNum,
                    surahNameArabic = sMeta.arabicName,
                    ayahNumberInSurah = m.numberInSurah,
                    globalAyahNumber = m.number,
                    uthmaniText = m.text,
                    juz = sMeta.juzStart,
                    hizbQuarter = sMeta.hizbStart
                )
            }
            (localMatches + remoteMapped).distinctBy { "${it.surahNumber}:${it.ayahNumberInSurah}" }
        } catch (_: Exception) {
            localMatches
        }
    }

    private fun defaultShiaTafsirReferenceNote(surahName: String, ayahNum: Int): String =
        "يُراجع في بيان معاني ومقاصد الآية ($ayahNum) من $surahName: الميزان في تفسير القرآن للعلامة الطباطبائي، ومجمع البيان للشيخ الطبرسي، والأمثل في تفسير كتاب الله المنزل للشيخ ناصر مكارم الشيرازي."
}
