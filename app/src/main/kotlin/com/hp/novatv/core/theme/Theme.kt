package com.hp.novatv.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.hp.novatv.data.prefs.AppTheme
import com.hp.novatv.data.prefs.UiDensity
import com.hp.novatv.data.prefs.UiSettings

/** 10-feot icin 48-56sp taban; ayarlardan gelen olcek ile carpilir. */
val LocalNovaScale = staticCompositionLocalOf { 1f }
val LocalBaseSp = staticCompositionLocalOf { 48f }
val LocalDensityMode = staticCompositionLocalOf { 1f }

@Composable
fun NovaTvTheme(
    settings: UiSettings,
    content: @Composable () -> Unit,
) {
    val palette = AccentPalette.from(settings.accent)

    val colorScheme = if (settings.theme == AppTheme.LIGHT && !isSystemInDarkTheme()) {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.secondary,
            background = androidx.compose.ui.graphics.Color(0xFFF7F7FA),
            onBackground = androidx.compose.ui.graphics.Color(0xFF101014),
            surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            onSurface = androidx.compose.ui.graphics.Color(0xFF101014),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8E8EF),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF4A4A57),
            outline = androidx.compose.ui.graphics.Color(0xFFC4C4CF),
        )
    } else {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.secondary,
            background = NovaBackground,
            onBackground = NovaOnSurface,
            surface = NovaSurface,
            onSurface = NovaOnSurface,
            surfaceVariant = NovaSurfaceVariant,
            onSurfaceVariant = NovaOnSurfaceVariant,
            outline = NovaOutline,
        )
    }

    val base = settings.baseSp
    val scale = settings.fontScale

    val typography = Typography(
        displayLarge = TextStyle(fontSize = (base * 1.6).sp, fontWeight = FontWeight.Bold),
        displayMedium = TextStyle(fontSize = (base * 1.35).sp, fontWeight = FontWeight.Bold),
        displaySmall = TextStyle(fontSize = (base * 1.15).sp, fontWeight = FontWeight.SemiBold),
        headlineLarge = TextStyle(fontSize = (base * 1.0).sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = TextStyle(fontSize = (base * 0.9).sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = TextStyle(fontSize = (base * 0.8).sp, fontWeight = FontWeight.Medium),
        titleLarge = TextStyle(fontSize = (base * 0.78).sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontSize = (base * 0.68).sp, fontWeight = FontWeight.Medium),
        titleSmall = TextStyle(fontSize = (base * 0.6).sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = (base * 0.66).sp),
        bodyMedium = TextStyle(fontSize = (base * 0.6).sp),
        bodySmall = TextStyle(fontSize = (base * 0.54).sp),
        labelLarge = TextStyle(fontSize = (base * 0.6).sp, fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontSize = (base * 0.54).sp, fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontSize = (base * 0.48).sp),
    )

    // Yogunluk: boslugu daraltir/genisletir
    val densityFactor = when (settings.density) {
        UiDensity.COMPACT -> 0.8f
        UiDensity.NORMAL -> 1.0f
        UiDensity.COMFORTABLE -> 1.2f
    }

    CompositionLocalProvider(
        LocalNovaScale provides scale,
        LocalBaseSp provides base,
        LocalDensityMode provides densityFactor,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

/** 4K/1080p farkini yonetmek icin olcekli spacings. */
@Composable
fun scaledDp(value: Int): androidx.compose.ui.unit.Dp =
    (value * LocalDensityMode.current).dp
