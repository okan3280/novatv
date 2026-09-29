package com.hp.novatv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.Text
import com.hp.novatv.AppContainer
import com.hp.novatv.NovaTvApp
import com.hp.novatv.R
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.theme.NovaOnSurfaceVariant
import kotlinx.coroutines.flow.first

/**
 * Ekran 11: PIN ekrani (ebeveyn kilidi).
 *
 * RC833A kumandada rakam tusu olmayabilir; sanal rakam izgarasi sunulur.
 */
@Composable
fun PinScreen(
    onSuccess: () -> Unit,
    onSkip: () -> Unit,
    container: AppContainer = (LocalContext.current.applicationContext as NovaTvApp).container,
) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var expectedPin by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        expectedPin = container.settings.parentalPin.first().ifBlank { null }
        if (expectedPin == null) onSkip()
    }

    val maxLength = remember(expectedPin) { expectedPin?.length?.coerceAtLeast(4) ?: 4 }

    fun submit() {
        val pin = expectedPin ?: return
        if (entered == pin) {
            onSuccess()
        } else {
            error = true
            entered = ""
        }
    }

    LaunchedEffect(entered, expectedPin) {
        error = false
        val pin = expectedPin ?: return@LaunchedEffect
        if (entered.length == pin.length && entered.isNotEmpty()) {
            kotlinx.coroutines.delay(120)
            submit()
        }
    }

    BackHandler { onSkip() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.pin_title),
            color = Color.White,
            fontSize = (LocalBaseSp.current * 0.9f).sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (error) {
                stringResource(R.string.pin_wrong)
            } else {
                stringResource(R.string.pin_enter)
            },
            color = if (error) Color(0xFFFF6B6B) else NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.6f).sp,
            modifier = Modifier.padding(top = 8.dp),
        )

        // Nokta gostergesi
        Row(
            modifier = Modifier.padding(vertical = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            repeat(maxLength) { index ->
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < entered.length) {
                                Color(0xFF00A8E8)
                            } else {
                                Color.White.copy(alpha = 0.25f)
                            },
                        ),
                )
            }
        }

        // Sanal rakam izgarasi
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "F"),
        )

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { key ->
                        when (key) {
                            "" -> Box(Modifier.size(96.dp))
                            "F" -> Box(
                                Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .then(
                                        Modifier.focusableCircle {
                                            entered = entered.dropLast(1)
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("F", color = Color.White, fontSize = 28.sp)
                            }

                            else -> Box(
                                Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .then(
                                        Modifier.focusableCircle {
                                            if (entered.length < maxLength) {
                                                entered += key
                                            }
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    key,
                                    color = Color.White,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberCoroutineScopeCompat() =
    androidx.compose.runtime.rememberCoroutineScope()

/** D-pad ile odaklanabilir daire buton. */
private fun Modifier.focusableCircle(onClick: () -> Unit): Modifier =
    this.then(
        androidx.compose.foundation.clickable(onClick = onClick),
    )
