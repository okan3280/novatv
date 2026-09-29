package com.hp.novatv.player

import android.content.Context
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

/**
 * libVLC yedek motoru. ExoPlayer'in cozemedigi formatlar icin:
 * ham .ts, rtmp, rtsp, acik codec'ler, bazi "canli" HLS varyantlari.
 *
 * Dogrulanmis imzalar (libvlc-all 3.7.6):
 *   LibVLC(Context, List<String>)
 *   MediaPlayer(ILibVLC)
 *   Media(ILibVLC, String)
 *   MediaPlayer.vout : IVLCVout
 *   IVLCVout.setVideoSurface(Surface, SurfaceHolder)
 *   MediaPlayer.setVideoScale(ScaleType)
 */
class VlcEngine(
    private val context: Context,
) {

    private var libVlc: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var surfaceView: SurfaceView? = null

    val isPlaying: Boolean get() = mediaPlayer?.isPlaying == true
    val isAvailable: Boolean get() = libVlc != null

    fun initialize(): Boolean {
        if (libVlc != null) return true
        val options = listOf(
            "--avcodec-caching=2000",
            "--network-caching=2500",
            "--file-caching=1500",
            "--clock-jitter=0",
            "--clock-synchro=-1",
            "--no-drop-late-frames",
            "--drop-late-frames",
            "--audio-time-stretch",
            "--http-reconnect=true",
        )
        return runCatching { LibVLC(context, options) }
            .onFailure { Log.e(TAG, "VLC baslatilamadi", it) }
            .fold(
                onSuccess = { libVlc = it; true },
                onFailure = { false },
            )
    }

    /**
     * Video yuzeyini baglar.
     *
     * Dogrulanmis API (libvlc-all 3.7.6):
     *   MediaPlayer.getVLCVout() : IVLCVout
     *   IVLCVout.setVideoSurface(Surface, SurfaceHolder)
     *
     * `MediaPlayer.vout` alani Kotlin tarafindan cozumlenmiyor;
     * AWindow#setSurface ise private. Bu yuzden getVLCVout() kullanilir.
     */
    private fun bindSurface(holder: SurfaceHolder?) {
        val player = mediaPlayer ?: return
        runCatching {
            player.vlcVout?.setVideoSurface(holder?.surface, holder)
        }.onFailure { Log.w(TAG, "yuzey baglanamadi", it) }
    }

    fun attach(view: SurfaceView) {
        surfaceView = view
        view.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) = bindSurface(holder)

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) = bindSurface(holder)

            override fun surfaceDestroyed(holder: SurfaceHolder) = bindSurface(null)
        })
    }

    fun detach() {
        bindSurface(null)
        surfaceView = null
    }

    fun play(url: String): Boolean {
        if (!initialize()) return false
        val vlc = libVlc ?: return false

        releasePlayer()

        val player = runCatching { MediaPlayer(vlc) }.getOrElse {
            Log.e(TAG, "VLC medya oyuncusu olusturulamadi", it)
            return false
        }
        mediaPlayer = player

        val media = runCatching {
            Media(vlc, url).apply {
                addOption(":network-caching=2000")
                addOption(":http-user-agent=Mozilla/5.0 (Linux; Android TV) NovaTV/1.0")
            }
        }.getOrElse {
            Log.e(TAG, "VLC medya olusturulamadi", it)
            return false
        }

        runCatching {
            player.setMedia(media)
            player.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT)
            player.setAspectRatio(null)
        }.onFailure { Log.e(TAG, "VLC ayarlanamadi", it) }

        surfaceView?.holder?.let { bindSurface(it) }

        runCatching { player.play() }
            .onFailure { Log.e(TAG, "VLC oynatma baslamadi", it); return false }

        return true
    }

    /** Mevcut oynaticiyi yeni URL ile degistirir (zap icin). */
    fun switchSource(url: String) {
        val vlc = libVlc
        val player = mediaPlayer
        if (vlc == null || player == null) {
            play(url)
            return
        }
        runCatching {
            player.setMedia(Media(vlc, url))
            player.play()
        }.onFailure { Log.e(TAG, "VLC kaynak degistirilemedi", it) }
    }

    fun stop() {
        runCatching { mediaPlayer?.stop() }
    }

    fun pause() {
        runCatching { mediaPlayer?.pause() }
    }

    fun resume() {
        runCatching { mediaPlayer?.play() }
    }

    fun releasePlayer() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
    }

    fun release() {
        releasePlayer()
        runCatching { libVlc?.release() }
        libVlc = null
    }

    private companion object {
        const val TAG = "NovaTV/VlcEngine"
    }
}
