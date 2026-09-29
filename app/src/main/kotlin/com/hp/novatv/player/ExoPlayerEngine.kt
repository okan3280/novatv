package com.hp.novatv.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.hp.novatv.core.model.Channel
import com.hp.novatv.core.model.StreamType
import com.hp.novatv.data.prefs.UiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Oynatici motoru secimi. */
enum class Engine { EXOPLAYER, VLC }

/**
 * ExoPlayer motoru. Multiview icin birden fazla ornek yonetilir.
 * Her ornek kendi LoadControl'ine sahiptir; SD akislar icin tampon
 * kucultulur.
 */
@OptIn(UnstableApi::class)
class ExoPlayerEngine(
    private val context: Context,
    private val client: OkHttpClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {

    private val players = mutableMapOf<Int, ExoPlayer>()

    val defaultPlayer: ExoPlayer get() = getOrCreate(0)

    fun getOrCreate(slot: Int): ExoPlayer = players.getOrPut(slot) { buildPlayer(slot) }

    /** Multiview icin yeni slot. */
    fun acquireSlot(): Int {
        val free = (1..MAX_SLOTS).firstOrNull { it !in players }
        if (free != null) {
            getOrCreate(free)
            return free
        }
        // Tum slotlar dolu: 0'i geri dondur (ana oynatici)
        return 0
    }

    fun releaseSlot(slot: Int) {
        if (slot == 0) return
        players.remove(slot)?.release()
    }

    fun releaseAll() {
        players.values.forEach { runCatching { it.release() } }
        players.clear()
    }

    fun release() {
        releaseAll()
        scope.cancel()
    }

    /** Ayarlari mevcut oynaticilara uygular. */
    fun setSettings(settings: UiSettings) {
        // Tampon ayari bir sonraki oynatmada gecerli olur.
        players.values.forEach { runCatching { it.setPlaybackSpeed(1f) } }
    }

    /** Kanal ac. */
    fun play(player: Player, channel: Channel, url: String) {
        val item = buildMediaItem(channel, url)
        player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = true
    }

    /** URL degistirmeden yeniden oynat (ayni kanal, farkli segment). */
    fun reload(player: Player, channel: Channel, url: String) {
        val current = player.currentMediaItem?.localConfiguration?.uri?.toString()
        if (current == url) {
            player.prepare()
            player.play()
        } else {
            play(player, channel, url)
        }
    }

    fun buildMediaItem(channel: Channel, url: String): MediaItem {
        val mime = when (StreamType.fromUrl(url)) {
            StreamType.HLS -> MimeTypes.APPLICATION_M3U8
            StreamType.DASH -> MimeTypes.APPLICATION_MPD
            StreamType.PROGRESSIVE -> guessMime(url)
            else -> null
        }

        return MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMimeType(mime)
            .setMediaId(channel.id.toString())
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(channel.name)
                    .setArtworkUri(channel.logo.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
                    .build(),
            )
            .build()
    }

    private fun guessMime(url: String): String? = when {
        url.endsWith(".mp4", true) -> MimeTypes.VIDEO_MP4
        url.endsWith(".mkv", true) -> "video/x-matroska"
        url.endsWith(".webm", true) -> MimeTypes.VIDEO_WEBM
        url.endsWith(".avi", true) -> "video/x-msvideo"
        else -> null
    }

    private fun buildPlayer(slot: Int): ExoPlayer {
        val bufferMs = if (slot == 0) 15_000 else 5_000
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ bufferMs / 3,
                /* maxBufferMs = */ bufferMs * 4,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 4_000,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            OkHttpDataSource.Factory(client)
                .setDefaultRequestProperties(
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (Linux; Android TV) NovaTV/1.0",
                    ),
                ),
        )

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setSeekForwardIncrementMs(30_000)
            .setSeekBackIncrementMs(15_000)
            .build()
    }

    companion object {
        const val MAX_SLOTS = 4
        private const val TAG = "NovaTV/ExoEngine"

        /** Paylasilan OkHttp istemcisi. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply { maxRequests = 24; maxRequestsPerHost = 12 })
            .build()
    }
}
