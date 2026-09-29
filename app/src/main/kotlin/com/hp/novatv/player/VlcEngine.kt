package com.hp.novatv.player

import android.content.Context
import android.util.Log
import android.view.Surface
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
 *   LibVLC(Context, String[])
 *   MediaPlayer(ILibVLC)
 *   Media(ILibVLC, String)
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
        val options = arrayOf(
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

    fun attach(view: SurfaceView) {
        surfaceView = view
        view.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) = bind(holder)
            override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) =
                bind(holder)

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mediaPlayer?.setVideoSurface(null, null)
            }
        })
    }

    private fun bind(holder: SurfaceHolder) {
        mediaPlayer?.setVideoSurface(holder.surface, holder)
    }

    fun detach() {
        mediaPlayer?.setVideoSurface(null, null)
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

        player.setMedia(media)
        player.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT)
        player.setAspectRatio(null)

        surfaceView?.holder?.let { player.setVideoSurface(it.surface, it) }

        player.play()
        return true
    }

    /** Mevcut oynaticiyi yeni URL ile devam ettir (zap icin). */
    fun switchSource(url: String) {
        val vlc = libVlc
        val player = mediaPlayer
        if (vlc == null || player == null) {
            play(url)
            return
        }
        runCatching {
            val media = Media(vlc, url)
            player.setMedia(media)
            player.play()
        }.onFailure { Log.e(TAG, "VLC kaynak degistirilemedi", it) }
    }

    fun stop() = runCatching { mediaPlayer?.stop() }.let { }

    fun pause() = runCatching { mediaPlayer?.pause() }.let { }

    fun resume() = runCatching { mediaPlayer?.play() }.let { }

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
