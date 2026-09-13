package com.opal.app.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp

/**
 * Android: GlassPane — yuqori unumdorlikdagi obsidian glass.
 * GPU overdraw'siz, shader jank'siz — 120fps silliq rendering.
 */
@Composable
actual fun GlassPane(
    modifier: Modifier,
    radius: Dp,
    base: Float,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF141722).copy(alpha = 0.88f),
                        Color(0xFF090A10).copy(alpha = 0.94f)
                    )
                )
            )
            .border(
                BorderStroke(
                    0.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                ),
                shape
            )
    ) {
        // Specular sheen lit from above
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.07f),
                        0.22f to Color.Transparent
                    )
                )
        )
        content()
    }
}

/**
 * Android: tab bar — Compose glass (gradient + specular + hairline).
 * 120fps silliq animatsiya.
 */
@Composable
actual fun GlassTabBar(selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
    ComposeGlassTabBar(
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
    )
}

/** Android'da tab almashtirishda qotish bo'lmasligi uchun Veil olib tashlangan (nol re-render). */
@Composable
actual fun GlassVeil(alpha: Float, modifier: Modifier) {
    // No-op: eliminates Android tab lag
}



