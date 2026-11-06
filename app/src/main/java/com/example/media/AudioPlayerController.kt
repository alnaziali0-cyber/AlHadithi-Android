package com.example.media

import android.content.Context
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class AudioQueueItem(
    val idKey: String,
    val title: String,
    val performer: String,
    val category: String,
    val primaryUrl: String,
    val fallbackUrl: String? = null,
    val offlineRawResName: String? = null
)

data class AudioPlaybackState(
    val currentTrack: AudioQueueItem? = null,
    val queue: List<AudioQueueItem> = emptyList(),
    val currentIndex: Int = 0,
    val isPreparing: Boolean = false,
    val isPlaying: Boolean = false,
    val isRepeatOne: Boolean = false,
    val speed: Float = 1.0f,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val errorMessage: String? = null,
    val isOfflineSourceActive: Boolean = false,
    val downloadingKeys: Set<String> = emptySet(),
    /** Values are 0f..1f when Content-Length is known, or -1f while size is unknown. */
    val downloadProgress: Map<String, Float> = emptyMap(),
    val downloadedKeys: Set<String> = emptySet(),
    val isSpeakingTts: Boolean = false,
    val ttsTitle: String? = null
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    fun formattedPosition(): String = formatMillis(currentPositionMs)
    fun formattedDuration(): String = formatMillis(durationMs)

    companion object {
        fun formatMillis(ms: Int): String {
            val totalSec = (ms / 1000).coerceAtLeast(0)
            val m = totalSec / 60
            val s = totalSec % 60
            return String.format(Locale.US, "%02d:%02d", m, s)
        }
    }
}

object AudioPlayerController {
    private const val TAG = "AlHadithiAudio"
    private const val MIN_VALID_MP3_BYTES = 4_096L
    private const val MAX_DOWNLOAD_BYTES = 150L * 1024L * 1024L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var appContext: Context? = null
    private var exoPlayer: ExoPlayer? = null
    private var tickerJob: Job? = null
    private var nextSourceStageOnError: Int = 3
    private val activeDownloadCalls = ConcurrentHashMap<String, Call>()
    private var ttsEngine: TextToSpeech? = null
    private var ttsReady = false

    private val downloadHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(false)
            .build()
    }

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    fun init(context: Context) {
        val ctx = context.applicationContext
        if (appContext == null) {
            appContext = ctx
            initializePlayer(ctx)
            refreshDownloadedKeys(ctx)
        }
    }

    /** One application-scoped ExoPlayer is reused across Compose screens. */
    private fun initializePlayer(context: Context) {
        if (exoPlayer != null) return
        val player = ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(
                Media3AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> _state.update {
                        it.copy(isPreparing = true, isPlaying = false)
                    }
                    Player.STATE_READY -> _state.update {
                        it.copy(
                            isPreparing = false,
                            durationMs = player.duration.takeIf { d -> d != C.TIME_UNSET }
                                ?.coerceAtLeast(0L)?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0,
                            errorMessage = null
                        )
                    }
                    Player.STATE_ENDED -> handleTrackEnded()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying, isPreparing = if (isPlaying) false else it.isPreparing) }
                if (isPlaying) startProgressTicker() else tickerJob?.cancel()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w(TAG, "Media3 playback failed", error)
                val track = _state.value.currentTrack
                val retryStage = nextSourceStageOnError
                if (track != null && retryStage <= 2) {
                    startTrackInternal(track, attemptStage = retryStage)
                } else {
                    _state.update {
                        it.copy(
                            isPreparing = false,
                            isPlaying = false,
                            errorMessage = "تعذر تشغيل هذا الملف الصوتي. تحقق من الرابط أو جرّب مقطعاً آخر."
                        )
                    }
                }
            }
        })
        exoPlayer = player
    }

    private fun handleTrackEnded() {
        val current = _state.value
        val player = exoPlayer ?: return
        if (current.isRepeatOne && current.currentTrack != null) {
            player.seekTo(0L)
            player.play()
        } else if (current.currentIndex + 1 < current.queue.size) {
            val nextIndex = current.currentIndex + 1
            _state.update { it.copy(currentIndex = nextIndex) }
            startTrackInternal(current.queue[nextIndex], attemptStage = 0)
        } else {
            _state.update {
                it.copy(isPlaying = false, isPreparing = false, currentPositionMs = it.durationMs)
            }
        }
    }

    private fun offlineAudioDir(context: Context): File {
        val dir = File(context.filesDir, "offline_audio")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun safeFileName(idKey: String): String =
        idKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_") + ".mp3"

    private fun refreshDownloadedKeys(context: Context) {
        val directory = offlineAudioDir(context)
        val files = directory.listFiles().orEmpty()

        // Remove interrupted temporary downloads; they are never considered offline audio.
        files.filter { it.isFile && it.name.endsWith(".mp3.part") }.forEach { it.delete() }

        // Recover a previous valid download if the app was interrupted during replacement.
        files.filter { it.isFile && it.name.endsWith(".mp3.bak") }.forEach { backup ->
            val target = File(directory, backup.name.removeSuffix(".bak"))
            when {
                !target.exists() && isValidMp3File(backup) -> backup.renameTo(target)
                target.exists() && !isValidMp3File(target) && isValidMp3File(backup) -> {
                    if (target.delete()) backup.renameTo(target)
                }
                else -> backup.delete()
            }
        }

        val keys = directory.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".mp3") && isValidMp3File(it) }
            .map { it.name.removeSuffix(".mp3") }
            .toSet()
        _state.update { it.copy(downloadedKeys = keys) }
    }

    /** Returns true only when a real, readable MP3 is available locally. */
    fun isTrackAvailableOffline(idKey: String, offlineRawResName: String?): Boolean {
        val ctx = appContext ?: return false
        if (!offlineRawResName.isNullOrBlank() && hasValidBundledAudioResource(ctx, offlineRawResName)) {
            return true
        }
        val safeKey = idKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return _state.value.downloadedKeys.contains(safeKey)
    }

    private fun hasValidBundledAudioResource(context: Context, resourceName: String): Boolean {
        val resourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (resourceId == 0) return false

        return try {
            // Compressed raw resources do not expose a file descriptor; fall back to a stream.
            val descriptor = runCatching {
                context.resources.openRawResourceFd(resourceId)
            }.getOrNull()
            if (descriptor != null) {
                descriptor.use { afd ->
                    if (afd.length >= 0L && afd.length < MIN_VALID_MP3_BYTES) return false
                    afd.createInputStream().use(::hasMp3Signature)
                }
            } else {
                context.resources.openRawResource(resourceId).use(::hasMp3Signature)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bundled audio resource is not readable: $resourceName", e)
            false
        }
    }

    private fun isValidMp3File(file: File): Boolean {
        if (!file.isFile || file.length() < MIN_VALID_MP3_BYTES) return false
        return try {
            FileInputStream(file).use(::hasMp3Signature)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to inspect local audio file: ${file.name}", e)
            false
        }
    }

    /**
     * Confirms an MPEG audio frame instead of treating an ID3 tag or file extension as proof.
     * Memory use is capped at 64 KB; large ID3 tags are skipped from the stream.
     */
    private fun hasMp3Signature(input: java.io.InputStream): Boolean {
        val prefix = ByteArray(10)
        val prefixCount = readAtMost(input, prefix, 0, prefix.size)
        if (prefixCount < 4) return false

        val startsWithId3 = prefixCount == prefix.size &&
            prefix[0] == 'I'.code.toByte() &&
            prefix[1] == 'D'.code.toByte() &&
            prefix[2] == '3'.code.toByte()

        val sample = ByteArray(64 * 1024)
        var count = 0
        if (startsWithId3) {
            // ID3 sizes use four 7-bit synchsafe bytes.
            val tagSize = ((prefix[6].toInt() and 0x7F) shl 21) or
                ((prefix[7].toInt() and 0x7F) shl 14) or
                ((prefix[8].toInt() and 0x7F) shl 7) or
                (prefix[9].toInt() and 0x7F)
            // ID3v2.4 includes a possible footer in its synchsafe tag-size field.
            if (!skipFully(input, tagSize.toLong())) return false
            count = readAtMost(input, sample, 0, sample.size)
        } else {
            System.arraycopy(prefix, 0, sample, 0, prefixCount)
            count = prefixCount + readAtMost(input, sample, prefixCount, sample.size - prefixCount)
        }
        if (count < 4) return false

        for (index in 0 until count - 3) {
            val b0 = sample[index].toInt() and 0xFF
            val b1 = sample[index + 1].toInt() and 0xFF
            val b2 = sample[index + 2].toInt() and 0xFF
            if (b0 != 0xFF || (b1 and 0xE0) != 0xE0) continue

            val mpegVersion = (b1 shr 3) and 0x03
            val layer = (b1 shr 1) and 0x03
            val bitrateIndex = (b2 shr 4) and 0x0F
            val sampleRateIndex = (b2 shr 2) and 0x03
            if (mpegVersion != 0x01 && layer != 0 && bitrateIndex in 1..14 && sampleRateIndex != 0x03) {
                return true
            }
        }
        return false
    }

    private fun readAtMost(
        input: java.io.InputStream,
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Int {
        var total = 0
        while (total < length) {
            val read = input.read(buffer, offset + total, length - total)
            if (read <= 0) break
            total += read
        }
        return total
    }

    private fun skipFully(input: java.io.InputStream, bytes: Long): Boolean {
        var remaining = bytes
        val scratch = ByteArray(8 * 1024)
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else {
                val read = input.read(scratch, 0, minOf(scratch.size.toLong(), remaining).toInt())
                if (read < 0) return false
                if (read > 0) remaining -= read.toLong()
            }
        }
        return true
    }

    fun downloadTrackForOffline(
        idKey: String,
        streamUrl: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val ctx = appContext
        if (ctx == null || streamUrl.isBlank()) {
            onComplete(false, "تعذر بدء التنزيل المحلي")
            return
        }
        val parsedUrl = runCatching { Uri.parse(streamUrl) }.getOrNull()
        if (parsedUrl?.scheme != "https" || parsedUrl.host.isNullOrBlank()) {
            onComplete(false, "رابط الصوت غير صالح أو غير آمن")
            return
        }

        val safeKey = idKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        if (_state.value.downloadingKeys.contains(safeKey)) {
            onComplete(false, "هذا المقطع قيد التنزيل حالياً")
            return
        }

        _state.update {
            it.copy(
                downloadingKeys = it.downloadingKeys + safeKey,
                downloadProgress = it.downloadProgress + (safeKey to -1f)
            )
        }
        scope.launch {
            var wasCancelled = false
            val ok = withContext(Dispatchers.IO) {
                val directory = offlineAudioDir(ctx)
                val target = File(directory, safeFileName(safeKey))
                val temp = File(directory, "${safeFileName(safeKey)}.part")
                try {
                    temp.delete()
                    val request = Request.Builder()
                        .url(streamUrl)
                        .header("Accept", "audio/mpeg, audio/*;q=0.9, application/octet-stream;q=0.8")
                        .build()
                    val call = downloadHttpClient.newCall(request)
                    activeDownloadCalls[safeKey] = call
                    call.execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Audio server returned HTTP ${response.code}")
                        if (response.request.url.scheme != "https") {
                            throw IOException("Audio download was redirected to an insecure connection")
                        }
                        val body = response.body ?: throw IOException("Empty audio response")
                        val contentType = body.contentType()?.let { "${it.type}/${it.subtype}" }.orEmpty()
                        if (contentType.startsWith("text/") || contentType.contains("json", ignoreCase = true) ||
                            contentType.contains("html", ignoreCase = true)
                        ) {
                            throw IOException("The server did not return an audio file")
                        }
                        val contentLength = body.contentLength()
                        if (contentLength > MAX_DOWNLOAD_BYTES) {
                            throw IOException("Audio file exceeds the download size limit")
                        }

                        body.byteStream().use { input ->
                            temp.outputStream().buffered().use { output ->
                                val buffer = ByteArray(16 * 1024)
                                var total = 0L
                                var lastUiUpdate = 0L
                                while (true) {
                                    if (call.isCanceled()) throw IOException("Download cancelled")
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    total += read.toLong()
                                    if (total > MAX_DOWNLOAD_BYTES) {
                                        throw IOException("Audio file exceeds the download size limit")
                                    }
                                    output.write(buffer, 0, read)
                                    val now = System.currentTimeMillis()
                                    if (now - lastUiUpdate >= 250L || (contentLength > 0L && total >= contentLength)) {
                                        val progress = if (contentLength > 0L) {
                                            (total.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                                        } else {
                                            -1f
                                        }
                                        _state.update { state ->
                                            state.copy(downloadProgress = state.downloadProgress + (safeKey to progress))
                                        }
                                        lastUiUpdate = now
                                    }
                                }
                                output.flush()
                            }
                        }
                    }

                    if (!isValidMp3File(temp)) {
                        throw IOException("Downloaded file is empty or is not a valid MP3")
                    }

                    // Preserve the previous download until the replacement is complete and verified.
                    val backup = File(directory, "${safeFileName(safeKey)}.bak")
                    backup.delete()
                    val hadPreviousFile = target.exists()
                    if (hadPreviousFile && !target.renameTo(backup)) {
                        throw IOException("Unable to preserve the previous audio file")
                    }
                    if (!temp.renameTo(target)) {
                        if (hadPreviousFile) backup.renameTo(target)
                        throw IOException("Unable to store the downloaded audio file")
                    }
                    backup.delete()
                    true
                } catch (e: Exception) {
                    wasCancelled = activeDownloadCalls[safeKey]?.isCanceled() == true || e.message == "Download cancelled"
                    temp.delete()
                    Log.w(TAG, "Offline audio download failed for $idKey", e)
                    false
                } finally {
                    activeDownloadCalls.remove(safeKey)
                }
            }
            refreshDownloadedKeys(ctx)
            _state.update {
                it.copy(
                    downloadingKeys = it.downloadingKeys - safeKey,
                    downloadProgress = it.downloadProgress - safeKey
                )
            }
            when {
                ok -> onComplete(true, "تم حفظ المقطع للاستماع بدون إنترنت")
                wasCancelled -> onComplete(false, "تم إلغاء التنزيل")
                else -> onComplete(false, "تعذر تنزيل ملف صوتي صالح. تحقق من الرابط والاتصال ثم حاول مجدداً")
            }
        }
    }

    fun cancelDownload(idKey: String) {
        val safeKey = idKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        activeDownloadCalls[safeKey]?.cancel()
    }

    fun deleteDownloadedTrack(idKey: String): Boolean {
        val ctx = appContext ?: return false
        val safeKey = idKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        if (safeKey in _state.value.downloadingKeys) return false
        val file = File(offlineAudioDir(ctx), safeFileName(safeKey))
        val deleted = file.exists() && file.delete()
        if (deleted) refreshDownloadedKeys(ctx)
        return deleted
    }

    fun speakArabicTextOffline(title: String, arabicText: String) {
        val ctx = appContext ?: return
        stopAndDismiss()
        if (ttsEngine == null) {
            ttsEngine = TextToSpeech(ctx) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    ttsEngine?.language = Locale.forLanguageTag("ar")
                    ttsEngine?.setSpeechRate(0.92f)
                    ttsEngine?.speak(
                        arabicText.take(3800),
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "alhadithi_tts"
                    )
                    _state.update { it.copy(isSpeakingTts = true, ttsTitle = title) }
                }
            }
        } else {
            ttsEngine?.language = Locale.forLanguageTag("ar")
            ttsEngine?.setSpeechRate(0.92f)
            ttsEngine?.speak(
                arabicText.take(3800),
                TextToSpeech.QUEUE_FLUSH,
                null,
                "alhadithi_tts"
            )
            _state.update { it.copy(isSpeakingTts = true, ttsTitle = title) }
        }
    }

    fun stopArabicTts() {
        try {
            ttsEngine?.stop()
        } catch (_: Exception) {
        }
        _state.update { it.copy(isSpeakingTts = false, ttsTitle = null) }
    }

    fun playQueue(
        items: List<AudioQueueItem>,
        startIndex: Int = 0,
        preferredSpeed: Float = _state.value.speed
    ) {
        if (items.isEmpty()) return
        stopArabicTts()
        val safeIdx = startIndex.coerceIn(0, items.lastIndex)
        _state.update {
            it.copy(
                queue = items,
                currentIndex = safeIdx,
                speed = preferredSpeed
            )
        }
        startTrackInternal(items[safeIdx], attemptStage = 0)
    }

    fun playSingle(item: AudioQueueItem, preferredSpeed: Float = _state.value.speed) {
        playQueue(listOf(item), 0, preferredSpeed)
    }

    /**
     * Stage 0: A validated local download or a validated bundled `res/raw` MP3.
     * Stage 1: Primary direct HTTPS stream
     * Stage 2: Fallback HTTPS stream
     */
    private fun startTrackInternal(track: AudioQueueItem, attemptStage: Int) {
        val ctx = appContext
        val safeKey = track.idKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

        if (attemptStage == 0 && ctx != null) {
            // Check user-downloaded offline MP3 first
            val downloadedFile = File(offlineAudioDir(ctx), safeFileName(safeKey))
            if (isValidMp3File(downloadedFile)) {
                playFromUriOrUrl(
                    track = track,
                    localUri = Uri.fromFile(downloadedFile),
                    remoteUrl = null,
                    isOffline = true,
                    nextStageOnError = 1
                )
                return
            }

            // Check bundled res/raw offline MP3
            val rawName = track.offlineRawResName
            if (!rawName.isNullOrBlank()) {
                val resId = ctx.resources.getIdentifier(rawName, "raw", ctx.packageName)
                if (resId != 0 && hasValidBundledAudioResource(ctx, rawName)) {
                    val rawUri = Uri.parse("android.resource://${ctx.packageName}/$resId")
                    playFromUriOrUrl(
                        track = track,
                        localUri = rawUri,
                        remoteUrl = null,
                        isOffline = true,
                        nextStageOnError = 1
                    )
                    return
                }
            }
        }

        if (attemptStage <= 1 && track.primaryUrl.isNotBlank()) {
            playFromUriOrUrl(
                track = track,
                localUri = null,
                remoteUrl = track.primaryUrl,
                isOffline = false,
                nextStageOnError = 2
            )
            return
        }

        if (attemptStage <= 2 && !track.fallbackUrl.isNullOrBlank()) {
            playFromUriOrUrl(
                track = track,
                localUri = null,
                remoteUrl = track.fallbackUrl,
                isOffline = false,
                nextStageOnError = 3
            )
            return
        }

        _state.update {
            it.copy(
                isPreparing = false,
                isPlaying = false,
                errorMessage = "هذا المقطع يحتاج إلى اتصال بالإنترنت. يمكنك تنزيله للاستماع إليه لاحقاً دون اتصال."
            )
        }
    }

    private fun playFromUriOrUrl(
        track: AudioQueueItem,
        localUri: Uri?,
        remoteUrl: String?,
        isOffline: Boolean,
        nextStageOnError: Int
    ) {
        val ctx = appContext
        val player = exoPlayer ?: ctx?.let { initializePlayer(it); exoPlayer } ?: return
        nextSourceStageOnError = nextStageOnError
        tickerJob?.cancel()
        _state.update {
            it.copy(
                currentTrack = track,
                isPreparing = true,
                isPlaying = false,
                currentPositionMs = 0,
                durationMs = 0,
                errorMessage = null,
                isOfflineSourceActive = isOffline
            )
        }

        try {
            val sourceUri = when {
                localUri != null && ctx != null -> localUri
                !remoteUrl.isNullOrBlank() -> {
                    val parsed = Uri.parse(remoteUrl)
                    require(parsed.scheme == "https" && !parsed.host.isNullOrBlank()) {
                        "Audio stream URL must use HTTPS"
                    }
                    parsed
                }
                else -> throw IllegalArgumentException("Missing audio source")
            }
            player.stop()
            player.clearMediaItems()
            player.setPlaybackParameters(PlaybackParameters(_state.value.speed))
            val mediaItem = MediaItem.Builder()
                .setUri(sourceUri)
                .setMimeType("audio/mpeg")
                .setMediaId(track.idKey)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.performer)
                        .build()
                )
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "Audio playback setup failed", e)
            if (nextStageOnError <= 2) {
                startTrackInternal(track, attemptStage = nextStageOnError)
            } else {
                _state.update {
                    it.copy(
                        isPreparing = false,
                        isPlaying = false,
                        errorMessage = "تعذر تشغيل الصوت حالياً"
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer
        val current = _state.value
        if (player == null) {
            current.currentTrack?.let { startTrackInternal(it, attemptStage = 0) }
            return
        }
        if (current.isPreparing) return
        runCatching {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
                player.play()
            }
        }.onFailure { Log.w(TAG, "togglePlayPause failed", it) }
    }

    fun seekToFraction(fraction: Float) {
        val player = exoPlayer ?: return
        val duration = _state.value.durationMs
        if (duration <= 0) return
        val target = (duration * fraction.coerceIn(0f, 1f)).toLong()
        runCatching {
            player.seekTo(target)
            _state.update { it.copy(currentPositionMs = target.toInt()) }
        }
    }

    fun setSpeed(newSpeed: Float) {
        val clamped = newSpeed.coerceIn(0.75f, 1.5f)
        _state.update { it.copy(speed = clamped) }
        runCatching { exoPlayer?.setPlaybackParameters(PlaybackParameters(clamped)) }
            .onFailure { Log.w(TAG, "Playback speed adjustment failed", it) }
    }

    fun toggleRepeatOne() {
        _state.update { it.copy(isRepeatOne = !it.isRepeatOne) }
    }

    fun skipNext() {
        val current = _state.value
        if (current.queue.isEmpty()) return
        val next = (current.currentIndex + 1) % current.queue.size
        _state.update { it.copy(currentIndex = next) }
        startTrackInternal(current.queue[next], attemptStage = 0)
    }

    fun skipPrevious() {
        val current = _state.value
        if (current.queue.isEmpty()) return
        val previous = if (current.currentIndex > 0) current.currentIndex - 1 else current.queue.lastIndex
        _state.update { it.copy(currentIndex = previous) }
        startTrackInternal(current.queue[previous], attemptStage = 0)
    }

    fun stopAndDismiss() {
        clearPlayerSource()
        stopArabicTts()
        _state.update {
            AudioPlaybackState(
                speed = it.speed,
                downloadingKeys = it.downloadingKeys,
                downloadProgress = it.downloadProgress,
                downloadedKeys = it.downloadedKeys
            )
        }
    }

    private fun startProgressTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (_state.value.isPlaying) {
                val player = exoPlayer
                if (player != null) {
                    val position = player.currentPosition.coerceAtLeast(0L)
                    val duration = player.duration.takeIf { it != C.TIME_UNSET }
                        ?.coerceAtLeast(0L)?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0
                    _state.update {
                        it.copy(
                            currentPositionMs = position.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                            durationMs = duration
                        )
                    }
                }
                delay(500L)
            }
        }
    }

    private fun clearPlayerSource() {
        tickerJob?.cancel()
        runCatching {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        }.onFailure { Log.w(TAG, "Unable to clear current audio source", it) }
    }
}
