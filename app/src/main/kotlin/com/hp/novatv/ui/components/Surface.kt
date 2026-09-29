package com.hp.novatv.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import com.hp.novatv.ui.components.SurfaceDefaults

/**
 * tv-material uyumlu Surface sarmalayicisi.
 *
 * androidx.tv.material3.Surface `color: Color` degil
 * `colors: SurfaceColors` kabul eder ve tiklanabilir varyantinda
 * `shape: ClickableSurfaceShape` ister. Bu sarmalayici Material3
 * aliskanligindaki `Surface(color = ...)` yazimini destekler, boylece
 * cagri kodlari degismez.
 */
@Composable
fun Surface(
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    if (onClick == null) {
        androidx.tv.material3.Surface(
            modifier = modifier,
            shape = shape,
            colors = SurfaceDefaults.colors(containerColor = color),
            content = content,
        )
    } else {
        androidx.tv.material3.Surface(
            onClick = onClick,
            onLongClick = onLongClick,
            enabled = enabled,
            modifier = modifier,
            shape = ClickableSurfaceDefaults.shape(shape),
            colors = ClickableSurfaceDefaults.colors(containerColor = color),
            content = content,
        )
    }
}
