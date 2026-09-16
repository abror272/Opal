package com.opal.app.platform

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSafePadding(): PaddingValues = WindowInsets.safeDrawing.asPaddingValues()

actual val isIosPlatform: Boolean = false

@Composable
actual fun rememberOnboarded(): Boolean {
    val ctx = LocalContext.current
    return remember { ctx.getSharedPreferences("opal_prefs", 0).getBoolean("onboarded", false) }
}

@Composable
actual fun rememberMarkOnboarded(): () -> Unit {
    val ctx = LocalContext.current
    return remember { { ctx.getSharedPreferences("opal_prefs", 0).edit().putBoolean("onboarded", true).apply() } }
}
