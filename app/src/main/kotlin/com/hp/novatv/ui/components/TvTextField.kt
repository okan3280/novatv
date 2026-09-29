package com.hp.novatv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.theme.NovaOnSurfaceVariant
import com.hp.novatv.core.theme.outline

/**
 * TV metin giris alani.
 *
 * androidx.tv.material3'te TextField bulunmuyor (TV'de dokunmatik
 * klavye yok). Bu yuzden foundation'in BasicTextField'i uzerine kendi
 * gorunumumuzu kuruyoruz.
 *
 * NOT: Compose 1.12'de BasicTextField'in `value: String` overload'i
 * `DeprecationLevel.HIDDEN` oldugu icin cagrilamiyor. State tabanli
 * API (rememberTextFieldState) kullanilir; harici deger degisimleri
 * LaunchedEffect ile iceriye yazilir.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val base = LocalBaseSp.current

    // State tabanli BasicTextField
    val state = rememberTextFieldState(
        initialText = value,
    )

    // Harici deger degisince (orn. duzenleme ekrani) alani guncelle
    LaunchedEffect(value) {
        val current = state.text.toString()
        if (current != value) {
            state.edit { setTextAndSelection(TextFieldValue(value)) }
        }
    }

    // Kullanici girdisini disariye bildir
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .collect { text -> if (text != value) onValueChange(text) }
    }

    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = label,
            color = NovaOnSurfaceVariant,
            fontSize = (base * 0.46f).sp,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (state.text.isEmpty()) {
                Text(
                    text = "…",
                    color = NovaOnSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = (base * 0.58f).sp,
                )
            }

            BasicTextField(
                state = state,
                singleLine = singleLine,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (base * 0.58f).sp,
                ),
                visualTransformation = if (isPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) {
                        KeyboardType.Password
                    } else {
                        KeyboardType.Text
                    },
                    imeAction = imeAction,
                ),
                keyboardActions = keyboardActions,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
