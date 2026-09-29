package com.hp.novatv.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.hp.novatv.R
import com.hp.novatv.core.model.Channel
import com.hp.novatv.ui.PlayerActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * On plan oynatma servisi.
 *
 * Android 12+ icin foregroundServiceType="mediaPlayback" zorunlu.
 * Ekran kapansa bile yayin surer ve kumandadan kontrol edilir.
 */
class PlaybackService : LifecycleService() {

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }

            else -> {
                val channelName = intent?.getStringExtra(EXTRA_CHANNEL_NAME)
                if (channelName != null) {
                    startForeground(NOTIF_ID, buildNotification(channelName))
                    acquireWakeLock()
                }
            }
        }
        return START_STICKY
    }

    /** Oynatilan kanali bildirime yansitir. */
    fun updateChannel(channel: Channel) {
        _currentChannel.value = channel
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIF_ID, buildNotification(channel.name))
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    // ------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                setShowBadge(false)
                enableVibration(false)
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                RECORDING_CHANNEL_ID,
                getString(R.string.notification_recording_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) },
        )
    }

    private fun buildNotification(channelName: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, PlayerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_channel, channelName))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NovaTV::Playback",
        ).apply {
            setReferenceCounted(false)
            runCatching { acquire(12 * 60 * 60 * 1000L) } // 12 saat ust sinir
        }
    }

    private fun releaseWakeLock() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    companion object {
        private const val CHANNEL_ID = "novatv_playback"
        private const val RECORDING_CHANNEL_ID = "novatv_recording"
        private const val NOTIF_ID = 1001

        const val ACTION_STOP = "com.hp.novatv.action.STOP"
        const val EXTRA_CHANNEL_NAME = "channel_name"

        fun start(context: Context, channelName: String) {
            val intent = Intent(context, PlaybackService::class.java)
                .putExtra(EXTRA_CHANNEL_NAME, channelName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, PlaybackService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
