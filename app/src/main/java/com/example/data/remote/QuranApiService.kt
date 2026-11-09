package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.AlHadithiDao
import com.example.data.local.CachedAyahEntity
import com.example.domain.SurahCatalog
import com.example.domain.QuranCacheIntegrity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
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
    private const val BUNDLED_QURAN_ASSET = "quran_simple_full.json"
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
    @Volatile
    private var bundledQuranChecked: Boolean = false

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
     * Loads the bundled Tanzil Simple Quran only after checking all 114 surahs and 6,236 ayahs.
     * Empty, truncated, duplicated, or structurally incorrect assets are rejected.
     */
    suspend fun ensureBundledQuranLoaded(context: Context): Map<Int, List<CachedAyahEntity>> {
        bundledSurahsMap?.let { return it }
        if (bundledQuranChecked) return emptyMap()
        return assetMutex.withLock {
            bundledSurahsMap?.let { return@withLock it }
            if (bundledQuranChecked) return@withLock emptyMap()
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
                            require(tuple.length() == 6) { "Ayah tuple schema is invalid in surah $surahNumber" }

                            val ayahInSurah = tuple.getInt(0)
                            val globalNum = tuple.getInt(1)
                            val juzNum = tuple.getInt(2)
                            val hizbQ = tuple.getInt(3)
                            val sajdaValue = tuple.getInt(4)
                            val rawText = tuple.getString(5)

                            require(ayahInSurah == j + 1) { "Ayah order is invalid in surah $surahNumber" }
                            require(globalNum == expectedGlobalAyah) { "Global ayah numbering is invalid at $expectedGlobalAyah" }
                            require(juzNum in 1..30) { "Invalid juz number for ayah $globalNum" }
                            require(hizbQ in 1..240) { "Invalid hizb quarter for ayah $globalNum" }
                            require(sajdaValue == 0 || sajdaValue == 1) { "Invalid sajda flag for ayah $globalNum" }
                            require(rawText.isNotBlank()) { "Empty Quran text at ayah $globalNum" }
                            require(rawText.any { it in '\u0600'..'\u06FF' }) {
                                "Non-Arabic text found at ayah $globalNum"
                            }

                            list.add(
                                CachedAyahEntity(
                                    surahNumber = surahNumber,
                                    surahNameArabic = meta.arabicName,
                                    ayahNumberInSurah = ayahInSurah,
                                    globalAyahNumber = globalNum,
                                    uthmaniText = rawText,
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
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    Log.w(TAG, "No complete verified 114-surah offline Quran corpus is packaged; the app will use its network source and local per-surah cache", e)
                    emptyMap()
                }
            }
            if (parsed.size == EXPECTED_SURAH_COUNT) {
                bundledSurahsMap = parsed
            }
            // Packaged assets are immutable for the lifetime of this process. Remember
            // a missing/invalid asset too, avoiding repeated disk reads and log spam.
            bundledQuranChecked = true
            parsed
        }
    }

    /**
     * Loads a verified bundled Quran only when the full 114-surah corpus is present.
     * Otherwise it downloads the requested Al Quran Cloud Uthmani surah and stores it in Room for later offline reading.
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
        val existingMap = existing.associateBy { it.ayahNumberInSurah }

        // 1. A complete bundled corpus is the source of truth and must take precedence
        // over an older online cache already stored on the user's device.
        if (context != null) {
            val bundledMap = ensureBundledQuranLoaded(context)
            val bundledSurah = bundledMap[surahNumber]
            if (!bundledSurah.isNullOrEmpty() &&
                QuranCacheIntegrity.isCompleteSurah(surahNumber, bundledSurah)
            ) {
                val merged = bundledSurah.map { ayah ->
                    val previous = existingMap[ayah.ayahNumberInSurah]
                    if (previous != null && previous.tafsirSummary.isNotBlank()) {
                        ayah.copy(
                            tafsirSummary = previous.tafsirSummary,
                            tafsirSource = previous.tafsirSource.ifBlank { ayah.tafsirSource }
                        )
                    } else {
                        ayah
                    }
                }
                dao.insertCachedAyahs(merged)
                return@withContext Result.success(merged)
            }
        }

        // 2. Use an existing cache only when its numbering, content, and metadata are valid.
        if (QuranCacheIntegrity.isCompleteSurah(surahNumber, existing)) {
            return@withContext Result.success(existing.sortedBy { it.ayahNumberInSurah })
        }

        // 3. Download and validate a full surah. A partial response is never stored or
        // returned as a successful read operation.
        try {
            val response = api.getSurahUthmani(surahNumber)
            val data = response.data
            val expectedGlobalStart = SurahCatalog.allSurahs
                .asSequence()
                .takeWhile { it.number < surahNumber }
                .sumOf { it.ayahCount } + 1
            val dtos = data?.ayahs.orEmpty()
            val responseIsValid = response.code == 200 &&
                data?.number == surahNumber &&
                dtos.size == meta.ayahCount &&
                dtos.indices.all { index ->
                    val ayah = dtos[index]
                    ayah.numberInSurah == index + 1 &&
                        ayah.number == expectedGlobalStart + index &&
                        ayah.juz in 1..30 &&
                        ayah.hizbQuarter in 1..240 &&
                        ayah.text.isNotBlank() &&
                        ayah.text.any { it in '\u0600'..'\u06FF' }
                }

            if (responseIsValid) {
                val mapped = dtos.map { dto ->
                    val previous = existingMap[dto.numberInSurah]
                    val isSajda = when (val sajda = dto.sajda) {
                        is Boolean -> sajda
                        is Map<*, *> -> true
                        else -> false
                    }
                    CachedAyahEntity(
                        surahNumber = surahNumber,
                        surahNameArabic = meta.arabicName,
                        ayahNumberInSurah = dto.numberInSurah,
                        globalAyahNumber = dto.number,
                        uthmaniText = dto.text,
                        juz = dto.juz,
                        hizbQuarter = dto.hizbQuarter,
                        hasSajdah = isSajda,
                        tafsirSummary = previous?.tafsirSummary
                            ?: defaultShiaTafsirReferenceNote(meta.arabicName, dto.numberInSurah),
                        tafsirSource = previous?.tafsirSource
                            ?: "الميزان في تفسير القرآن للطباطبائي · مجمع البيان للطبرسي · الأمثل للشيرازي"
                    )
                }
                if (QuranCacheIntegrity.isCompleteSurah(surahNumber, mapped)) {
                    dao.insertCachedAyahs(mapped)
                    return@withContext Result.success(mapped)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            // A complete validated cache is still useful offline. An incomplete one is not.
            if (QuranCacheIntegrity.isCompleteSurah(surahNumber, existing)) {
                return@withContext Result.success(existing.sortedBy { it.ayahNumberInSurah })
            }
            return@withContext Result.failure(e)
        }

        if (QuranCacheIntegrity.isCompleteSurah(surahNumber, existing)) {
            Result.success(existing.sortedBy { it.ayahNumberInSurah })
        } else {
            Result.failure(IllegalStateException("تعذر تحميل السورة كاملة؛ تحقق من الاتصال ثم أعد المحاولة"))
        }
    }

    /**
     * Searches the bundled Tanzil Simple corpus first, then local cache and the network Uthmani edition when needed.
     * When the verified 114-surah asset is present, search is fully offline and prioritizes its source text over older cached editions.
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
                return@withContext (offlineHits + localMatches)
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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            localMatches
        }
    }

    private fun defaultShiaTafsirReferenceNote(surahName: String, ayahNum: Int): String =
        "يُراجع في بيان معاني ومقاصد الآية ($ayahNum) من $surahName: الميزان في تفسير القرآن للعلامة الطباطبائي، ومجمع البيان للشيخ الطبرسي، والأمثل في تفسير كتاب الله المنزل للشيخ ناصر مكارم الشيرازي."
}
