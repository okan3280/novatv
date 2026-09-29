package com.hp.novatv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.hp.novatv.AppContainer
import com.hp.novatv.NovaTvApp
import com.hp.novatv.core.theme.NovaTvTheme
import com.hp.novatv.data.prefs.UiSettings
import com.hp.novatv.ui.screens.PinScreen
import kotlinx.coroutines.launch

/**
 * Ebeveyn kilidi ekrani. Manifeste kayitli: kanala giris veya
 * uygulama acilisinda dogrudan bu Activity baslatilabilir.
 */
class PinActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as NovaTvApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settings by container.settings.settings.collectAsState(initial = UiSettings())

            NovaTvTheme(settings) {
                PinScreen(
                    container = container,
                    onSuccess = { finish() },
                    onSkip = { finish() },
                )
            }
        }
    }
}
