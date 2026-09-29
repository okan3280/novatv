package com.hp.novatv.ui

import android.os.Bundle
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
import com.hp.novatv.ui.nav.NovaNavHost
import com.hp.novatv.ui.nav.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ana ekran. Ebeveyn kilidi acikse PIN ekrani ile baslar.
 */
class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as NovaTvApp).container

    /** null = ayarlar okunuyor, Compose bos durur. */
    private val startRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycleScope.launch {
            val hasPin = container.settings.settings.first().hasPin
            startRoute.value = if (hasPin) Routes.PIN else Routes.PLAYLISTS
        }

        setContent {
            val settings by container.settings.settings.collectAsState(initial = UiSettings())
            val route by startRoute.collectAsState()

            NovaTvTheme(settings) {
                route?.let { NovaNavHost(startDestination = it) }
            }
        }
    }
}
