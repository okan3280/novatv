package com.hp.novatv.core.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text

// ColorScheme.outline uzantisi ayni paket icinde
import com.hp.novatv.data.prefs.AccentColor

/**
 * androidx.tv.material3.ColorScheme'de 'outline' alani YOK; 'border' var.
 * Material3 aliskanligini korumak icin uzantili erisim.
 */
val ColorScheme.outline: Color get() = border

// NovaTV marka renkleri
val NovaBackground = Color(0xFF0B0B0F)
val NovaSurface = Color(0xFF15151C)
val NovaSurfaceVariant = Color(0xFF1F1F28)
val NovaOnSurface = Color(0xFFF2F2F5)
val NovaOnSurfaceVariant = Color(0xFFA8A8B8)
val NovaOutline = Color(0xFF3A3A46)
val NovaError = Color(0xFFFF6B6B)
val NovaLive = Color(0xFFFF3B3B)
val NovaRecord = Color(0xFFE53935)

/** Vurgu rengi + koyu tema icin turetilmis tonlar. */
data class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val live: Color = NovaLive,
) {
    companion object {
        fun from(accent: AccentColor): AccentPalette {
            val base = Color(accent.hex)
            return AccentPalette(
                primary = base,
                onPrimary = readableOn(base),
                primaryContainer = base.copy(alpha = 0.22f),
                onPrimaryContainer = base.lighten(0.25f),
                secondary = base.rotate(0.08f),
            )
        }

        /** Yansima orani: koyu mu arka plan uzerinde koyu mu yazi? */
        private fun readableOn(bg: Color): Color {
            val luminance = 0.2126 * bg.red + 0.7152 * bg.green + 0.0722 * bg.blue
            return if (luminance > 0.55) Color(0xFF0B0B0F) else Color.White
        }

        private fun Color.lighten(f: Float) = Color(
            red = red + (1f - red) * f,
            green = green + (1f - green) * f,
            blue = blue + (1f - blue) * f,
            alpha = alpha,
        )

        private fun Color.rotate(f: Float): Color = Color(
            red = (red + f).coerceIn(0f, 1f),
            green = (green + f * 0.4f).coerceIn(0f, 1f),
            blue = (blue + f * 0.8f).coerceIn(0f, 1f),
            alpha = alpha,
        )
    }
}

/** Ayarlarda kullanilan renk kure. */
@Composable
fun AccentSwatch(
    accent: AccentColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.tv.material3.Surface(
        onClick = onClick,
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
            RoundedCornerShape(8.dp),
        ),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = Color(accent.hex),
        ),
        modifier = modifier.size(52.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Text(
                    text = "OK",
                    color = Color.White,
                    fontSize = 18.sp,
                )
            }
        }
    }
}
