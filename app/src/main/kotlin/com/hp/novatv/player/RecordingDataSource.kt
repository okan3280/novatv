package com.hp.novatv.player

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.File
import java.io.FileOutputStream

/**
 * Veri kaynagina "tee" ekleyen DataSource: okunan tum baytlar ayni anda
 * dosyaya yazilir.
 *
 * Neden bu yol:
 *  - Media3'un genel API'si canli yayin -> MP4 kaydini desteklemiyor
 *    (media3-transformer yalnizca hazir dosyalar icin)
 *  - Muxer tabanli kayit, canli HLS/TS akisinda zamanlama ve
 *    kesinti yonetimi gerektirir; pratikte calismaz
 *  - IPTV yayinlarinin ezici cogu zaten MPEG-TS: segmentleri sirayla
 *    dosyaya yazmak dogrudan oynatilabilir bir .ts uretir
 *
 * Sonuc: MPEG-TS dosya. VLC, ExoPlayer, mpv hepsi acar.
 */
@OptIn(UnstableApi::class)
class RecordingDataSource(
    private val upstream: DataSource,
    private val sink: FileOutputStream,
) : BaseDataSource(/* isNetwork = */ true) {

    private var totalBytes = 0L

    val bytesWritten: Long get() = totalBytes

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val length = upstream.open(dataSpec)
        transferStarted(dataSpec)
        return length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val read = upstream.read(buffer, offset, length)
        if (read > 0) {
            // Sink hatasi oynatmayi bozmasin
            runCatching {
                sink.write(buffer, offset, read)
                totalBytes += read
            }.onFailure { Log.w(TAG, "kayit yazilamadi", it) }
            bytesTransferred(read)
        } else if (read == C.RESULT_END_OF_INPUT) {
            transferEnded()
        }
        return read
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        runCatching { sink.flush() }
        upstream.close()
    }

    companion object {
        private const val TAG = "NovaTV/RecordingDS"
    }
}
