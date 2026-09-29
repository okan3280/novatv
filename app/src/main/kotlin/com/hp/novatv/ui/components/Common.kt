package com.hp.novatv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import com.hp.novatv.ui.components.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.theme.NovaOnSurfaceVariant
import com.hp.novatv.core.theme.NovaSurfaceVariant

/** Kanal rozeti: HD / 4K / STEREO / LIVE. */
@Composable
fun ChannelBadge(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.primary,
    foreground: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(background)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            color = foreground,
            fontSize = (LocalBaseSp.current * 0.36f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Kanal numarasi rozeti. */
@Composable
fun ChannelNumber(
    number: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 34.dp, height = 24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.44f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Kanal logosu; yoksa bas harf rozeti. */
@Composable
fun ChannelLogo(
    logoUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)

    if (logoUrl.isNotBlank()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = name,
            modifier = modifier.clip(shape).background(NovaSurfaceVariant),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(2).uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (LocalBaseSp.current * 0.42f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Bolum basligi. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.72f).sp,
            fontWeight = FontWeight.SemiBold,
        )
        Box(Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Bos durum. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.9f).sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            color = NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.6f).sp,
            modifier = Modifier.padding(top = 10.dp, start = 60.dp, end = 60.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(actionLabel, fontSize = (LocalBaseSp.current * 0.6f).sp)
            }
        }
    }
}

/** Yukleme gostergesi. */
@Composable
fun LoadingState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.66f).sp,
        )
    }
}

/** Hata mesaji. */
@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            fontSize = (LocalBaseSp.current * 0.72f).sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (onRetry != null) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = 20.dp)) {
                Text("Tekrar Dene", fontSize = (LocalBaseSp.current * 0.6f).sp)
            }
        }
    }
}

/** Yatay ayirici. */
@Composable
fun HLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    )
}

/** Baslik + deger satiri (ayarlar listesi). */
@Composable
fun SettingRow(
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 32.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.64f).sp,
        )
        if (value != null) {
            Box(Modifier.weight(1f))
            Text(
                text = value,
                color = NovaOnSurfaceVariant,
                fontSize = (LocalBaseSp.current * 0.58f).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
