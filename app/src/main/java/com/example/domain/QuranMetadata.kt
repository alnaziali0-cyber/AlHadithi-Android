package com.example.domain

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** موضع في المصحف: رقم السورة ورقم الآية داخلها (يبدأ من 1). */
data class QuranPosition(val sura: Int, val aya: Int)

/**
 * شكل ملف `assets/quran_metadata.json`، وهو مستخرج من بيانات تنزيل (tanzil.net) بترخيص CC BY 3.0.
 * كل موضع مصفوفة [سورة، آية]، والتسلسل يطابق المصحف المطبوع.
 */
data class QuranMetadataJson(
    val suras: List<QuranSuraJson> = emptyList(),
    val juz: List<List<Int>> = emptyList(),
    val manzil: List<List<Int>> = emptyList(),
    val pages: List<List<Int>> = emptyList(),
    val quarters: List<List<Int>> = emptyList(),
    val rukus: List<List<Int>> = emptyList()
)

data class QuranSuraJson(val ayas: Int = 0, val start: Int = 0)

/**
 * تقسيم المصحف: 604 صفحة، و30 جزءاً، و60 حزباً، و240 ربعاً، و7 منازل، و556 ركوعاً.
 *
 * الحزب هو كل أربعة أرباع متتالية، فبداية الحزب n هي الربع 4(n-1)+1، والجزء يبدأ عند الربع 8(n-1)+1.
 */
class QuranMetadata internal constructor(private val data: QuranMetadataJson) {

    init {
        require(
            data.suras.size == 114 && data.pages.size == 604 && data.juz.size == 30 && data.quarters.size == 240
        ) { "quran_metadata.json does not match the Quran structure" }
    }

    val pageCount: Int
        get() = data.pages.size

    /** أول آية في الصفحة (1..604). */
    fun pageStart(page: Int): QuranPosition = position(data.pages[page.coerceIn(1, pageCount) - 1])

    /** أول آية في الجزء (1..30). */
    fun juzStart(juz: Int): QuranPosition = position(data.juz[juz.coerceIn(1, 30) - 1])

    /** أول آية في الحزب (1..60). */
    fun hizbStart(hizb: Int): QuranPosition = position(data.quarters[(hizb.coerceIn(1, 60) - 1) * 4])

    /** رقم الصفحة التي تحتوي الآية. */
    fun pageOf(sura: Int, aya: Int): Int = lastStartAtOrBefore(data.pages, globalIndex(sura, aya)) + 1

    /** رقم الجزء الذي تحتوي الآية. */
    fun juzOf(sura: Int, aya: Int): Int = lastStartAtOrBefore(data.juz, globalIndex(sura, aya)) + 1

    /** الترتيب العام للآية في المصحف (يبدأ من 0)، وهو المرجع للمقارنة بين المواضع. */
    private fun globalIndex(sura: Int, aya: Int): Int =
        data.suras[sura.coerceIn(1, 114) - 1].start + aya - 1

    private fun position(pair: List<Int>): QuranPosition = QuranPosition(pair[0], pair[1])

    /** أكبر فهرس يكون فيه موضع البداية قبل الآية أو عندها (بحث ثنائي على قائمة مرتبة). */
    private fun lastStartAtOrBefore(starts: List<List<Int>>, global: Int): Int {
        var low = 0
        var high = starts.size - 1
        var answer = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            val midGlobal = globalIndex(starts[mid][0], starts[mid][1])
            if (midGlobal <= global) {
                answer = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return answer
    }

    companion object {
        private const val ASSET = "quran_metadata.json"

        private val adapter by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
                .adapter(QuranMetadataJson::class.java)
        }

        @Volatile
        private var cached: QuranMetadata? = null

        /** يحلّل نص الملف؛ يستخدمه التحميل من الأصول والاختبارات على حد سواء. */
        fun parse(json: String): QuranMetadata =
            QuranMetadata(requireNotNull(adapter.fromJson(json)) { "quran_metadata.json is empty" })

        /** يُحمَّل الملف مرة واحدة ويُعاد استخدامه. */
        fun load(context: Context): QuranMetadata {
            cached?.let { return it }
            synchronized(this) {
                cached?.let { return it }
                val json = context.applicationContext.assets.open(ASSET)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                return parse(json).also { cached = it }
            }
        }
    }
}
