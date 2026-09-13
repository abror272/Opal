package com.opal.app.platform

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

/**
 * Safe area (status bar + home indicator) — har platforma o'z usulida oladi.
 * Android: Compose WindowInsets.safeDrawing
 * iOS: UIWindow.safeAreaInsets
 */
@Composable
expect fun rememberSafePadding(): PaddingValues
