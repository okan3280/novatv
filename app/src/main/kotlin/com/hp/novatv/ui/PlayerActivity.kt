package com.hp.novatv.ui

import android.content.Intent
import android.os.Bundle
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.hp.novatv.AppContainer
import com.hp.novatv.NovaTvApp
import com.hp.novatv.core.theme.NovaTvTheme
import com.hp.novatv.data.prefs.UiSettings
import com.hp.novatv.player.PlaybackService
import com.hp.novatv.ui.screens.PlayerScreen
import kotlinx.coroutines.launch

/**
 * Ekran 5: Oynatici. Tam ekran, kendi Activity'si.
 * Girdi: channelId, istege bagli startEpoch/duration (catch-up).
 */
class PlayerActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as NovaTvApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        val channelId = intent.getLongExtra(EXTRA_CHANNEL_ID, 0L)
        val startEpoch = intent.getLongExtra(EXTRA_START, 0L).takeIf { it > 0 }
        val durationMs = intent.getLongExtra(EXTRA_DURATION, 0L).takeIf { it > 0 }

        lifecycleScope.launch {
            val channel = container.repository.channel(channelId)
            if (channel != null) {
                PlaybackService.start(this@PlayerActivity, channel.name)
            }
        }

        setContent {
            val settings by container.settings.settings.collectAsState(initial = UiSettings())

            NovaTvTheme(settings) {
                PlayerScreen(
                    channelId = channelId,
                    startEpoch = startEpoch,
                    durationMs = durationMs,
                    container = container,
                    onExit = { finish() },
                    onOpenCatchup = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val EXTRA_START = "start_epoch"
        const val EXTRA_DURATION = "duration_ms"

        fun intent(
            context: android.content.Context,
            channelId: Long,
            startEpoch: Long? = null,
            durationMs: Long? = null,
        ): Intent = Intent(context, PlayerActivity::class.java)
            .putExtra(EXTRA_CHANNEL_ID, channelId)
            .putExtra(EXTRA_START, startEpoch ?: 0L)
            .putExtra(EXTRA_DURATION, durationMs ?: 0L)
    }
}
