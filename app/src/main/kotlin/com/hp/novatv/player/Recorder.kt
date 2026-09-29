package com.hp.novatv.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.hp.novatv.core.model.Channel
import com.hp.novatv.core.model.Recording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * DVR: yayin verisini [RecordingDataSource] ile dosyaya akitir.
 *
 * Uretilen dosya MPEG-TS'dir ve dogrudan oynatilabilir (ExoPlayer, VLC, mpv).
 *
 * Neden muxer degil: media3-transformer yalnizca hazir dosyalari isler,
 * canli akistan MP4 muxer ile kayit genel API ile desteklenmiyor.
 * Ham .ts yazmak hem dogru hem de hatasiz.
 *
 * DRM/HDCP korumali yayinlarda dosya anlamli icerik barindirmaz.
 */
@OptIn(UnstableApi::class)
class Recorder(
    private val context: Context,
    private val client: OkHttpClient,
) {

    private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private var activeChannel: Channel? = null
    private var activeFile: File? = null
    private var sink: FileOutputStream? = null
    private var startedAt: Long = 0L

    val isRecording: Boolean get() = sink != null

    fun recordingsDir(): File {
        val base = context.getExternalFilesDir(RECORDINGS_DIR) ?: context.filesDir
        return File(base, RECORDINGS_DIR).apply { if (!exists()) mkdirs() }
    }

    fun hasSpace(minimumBytes: Long = MIN_FREE_BYTES): Boolean =
        recordingsDir().usableSpace >= minimumBytes

    fun freeSpaceBytes(): Long = runCatching { recordingsDir().usableSpace }.getOrDefault(0L)

    /**
     * Oynaticiya verilecek veri kaynagi fabrikasini dondurur.
     * Kayit aktifse baytlar dosyaya yazilir, degilse dogrudan okunur.
     */
    fun dataSourceFactory(): DataSource.Factory {
        val output = sink
        val upstreamFactory = DataSource.Factory { plainDataSource() }
        if (output == null) return upstreamFactory

        return DataSource.Factory {
            RecordingDataSource(
                upstream = plainDataSource(),
                sink = FileOutputStream(output.fd),
            )
        }
    }

    private fun plainDataSource(): DataSource = DefaultDataSource(
        context,
        OkHttpDataSource.Factory(client).setDefaultRequestProperties(
            mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android TV) NovaTV/1.0"),
        ),
    )

    /** Kayit baslatir. [dataSourceFactory] cagrisindan SONRA cagrilmalidir. */
    fun start(channel: Channel): Boolean {
        if (isRecording) return false

        if (!hasSpace()) {
            _state.value = RecorderState.Error("Yeterli depolama alanı yok")
            return false
        }

        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(java.util.Date())
        val safeName = channel.name
            .replace(Regex("[^\\p{L}\\p{N} _-]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(40)
        val file = File(recordingsDir(), "${safeName}_$stamp.ts")

        val out = runCatching { FileOutputStream(file) }.getOrElse {
            Log.e(TAG, "Kayit dosyasi acilamadi", it)
            _state.value = RecorderState.Error("Kayıt dosyası açılamadı")
            return false
        }

        sink = out
        activeFile = file
        activeChannel = channel
        startedAt = System.currentTimeMillis()
        _state.value = RecorderState.Recording(channel.name, startedAt)
        return true
    }

    /** Kaydi durdurur ve kaydeder. */
    fun stop(): Recording? {
        val channel = activeChannel ?: return null
        val file = activeFile ?: return null

        runCatching { sink?.flush() }
        runCatching { sink?.close() }
        sink = null

        val recording = Recording(
            channelId = channel.id,
            playlistId = channel.playlistId,
            channelName = channel.name,
            filePath = file.absolutePath,
            startedAt = startedAt,
            durationMs = System.currentTimeMillis() - startedAt,
            sizeBytes = file.length(),
            isLocal = true,
        )

        _state.value = RecorderState.Stopped(recording)
        activeChannel = null
        activeFile = null
        return recording
    }

    /** Iptal: dosya silinir. */
    fun cancel() {
        val file = activeFile
        runCatching { sink?.close() }
        sink = null
        runCatching { file?.delete() }
        activeChannel = null
        activeFile = null
        _state.value = RecorderState.Idle
    }

    fun reset() {
        _state.value = RecorderState.Idle
    }

    sealed interface RecorderState {
        data object Idle : RecorderState
        data class Recording(val channelName: String, val startedAt: Long) : RecorderState
        data class Stopped(val recording: Recording) : RecorderState
        data class Error(val message: String) : RecorderState
    }

    private companion object {
        const val TAG = "NovaTV/Recorder"
        const val RECORDINGS_DIR = "recordings"
        const val MIN_FREE_BYTES = 500L * 1024 * 1024
    }
}
