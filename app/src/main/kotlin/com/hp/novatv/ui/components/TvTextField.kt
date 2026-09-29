package com.hp.novatv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.theme.NovaOnSurfaceVariant

/**
 * TV metin giriş alani.
 *
 * androidx.tv.material3'te TextField bulunmuyor (TV'de dokunmatik klavye
 * yok). Bu yuzden foundation'in BasicTextField'i uzerine kendi
 * gorunumumuzu kuruyoruz: kumandada D-pad ile gezinir, yazmak için
 * uzun basma / onay tusu ile sistem klavyesi acilir.
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
                .then(
                    focusRequester?.let { Modifier.focusRequester(it) }
                        ?: Modifier,
                )
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
            if (value.isEmpty()) {
                Text(
                    text = "…",
                    color = NovaOnSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = (base * 0.58f).sp,
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (base * 0.58f).sp,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
