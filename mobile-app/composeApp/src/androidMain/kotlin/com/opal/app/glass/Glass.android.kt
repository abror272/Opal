package com.opal.app.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Android: tab bar — Compose glass imitatsiya (gradient + specular + hairline).
 * iOS'dagi haqiqiy blur o'rniga silliq Material yuzalar.
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

/** Android'da real backdrop blur yo'q — qora yarim shaffof parda bilan his qilindiriladi. */
@Composable
actual fun GlassVeil(alpha: Float, modifier: Modifier) {
    if (alpha <= 0.02f) return
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .background(Color.Black.copy(alpha = 0.42f))
    )
}

@Composable
actual fun rememberSafePadding(): PaddingValues = WindowInsets.safeDrawing.asPaddingValues()
