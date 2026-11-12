package com.example.media

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Imports one user-selected, licensed adhan recording into app-private storage.
 * Storage Access Framework is used by the UI, so broad shared-audio permissions are unnecessary.
 */
object AdhanAudioStorage {
    private const val PREFS = "alhadithi_adhan_audio"
    private const val KEY_AUDIO_PATH = "audio_path"
    private const val MAX_AUDIO_BYTES = 80L * 1024L * 1024L
    private const val MIN_AUDIO_BYTES = 4_096L

    fun importAudio(context: Context, source: Uri): Result<File> = runCatching {
        val appContext = context.applicationContext
        val mimeType = appContext.contentResolver.getType(source)
        require(mimeType == null || mimeType.startsWith("audio/") || mimeType == "application/octet-stream") {
            "الملف المحدد ليس ملفاً صوتياً."
        }

        val directory = File(appContext.filesDir, "prayer_audio")
        require(directory.exists() || directory.mkdirs()) { "تعذر إنشاء مجلد الصوتيات." }
        val temporary = File(directory, "adhan_audio.importing")
        val target = File(directory, "adhan_audio")

        try {
            var copiedBytes = 0L
            val input = appContext.contentResolver.openInputStream(source)
                ?: error("تعذر فتح الملف الصوتي المحدد.")
            input.use { sourceStream ->
                FileOutputStream(temporary).use { destination ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val read = sourceStream.read(buffer)
                        if (read < 0) break
                        copiedBytes += read
                        require(copiedBytes <= MAX_AUDIO_BYTES) {
                            "حجم ملف الأذان أكبر من الحد المسموح (80 ميغابايت)."
                        }
                        destination.write(buffer, 0, read)
                    }
                    destination.fd.sync()
                }
            }
            require(copiedBytes >= MIN_AUDIO_BYTES) { "الملف فارغ أو قصير جداً ولا يبدو أنه تسجيل أذان صالح." }

            val backup = File(directory, "adhan_audio.previous")
            backup.delete()
            val hadPrevious = target.exists()
            if (hadPrevious && !target.renameTo(backup)) {
                error("تعذر حفظ النسخة السابقة قبل استبدالها.")
            }
            try {
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = false)
                    temporary.delete()
                }
                backup.delete()
            } catch (replaceError: Exception) {
                target.delete()
                if (hadPrevious && backup.exists()) backup.renameTo(target)
                throw replaceError
            }
            appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_AUDIO_PATH, target.absolutePath)
                .apply()
            target
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }
    }

    fun currentAudioFile(context: Context): File? {
        val path = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_AUDIO_PATH, null)
            ?: return null
        return File(path).takeIf { it.isFile && it.length() >= MIN_AUDIO_BYTES }
    }

    fun hasAudio(context: Context): Boolean = currentAudioFile(context) != null

    /** Returns the user override, or atomically extracts the bundled default into private storage. */
    fun resolvePlaybackFile(context: Context): File? {
        currentAudioFile(context)?.let { return it }
        val appContext = context.applicationContext
        val directory = File(appContext.filesDir, "prayer_audio")
        if (!directory.exists() && !directory.mkdirs()) return null
        val target = File(directory, "adhan_builtin.mp3")
        if (target.isFile && target.length() >= MIN_AUDIO_BYTES) return target
        val temporary = File(directory, "adhan_builtin.importing")
        return runCatching {
            appContext.assets.open("adhan_default.mp3").use { input ->
                FileOutputStream(temporary).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            require(temporary.length() >= MIN_AUDIO_BYTES)
            if (target.exists()) target.delete()
            check(temporary.renameTo(target)) { "تعذر تجهيز ملف الأذان المرفق" }
            target
        }.getOrElse {
            temporary.delete()
            null
        }
    }

    fun hasPlayableAudio(context: Context): Boolean =
        currentAudioFile(context) != null || runCatching { context.applicationContext.assets.open("adhan_default.mp3").use { it.read() >= 0 } }.getOrDefault(false)

    fun clear(context: Context) {
        val appContext = context.applicationContext
        currentAudioFile(appContext)?.delete()
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_AUDIO_PATH)
            .apply()
    }
}
