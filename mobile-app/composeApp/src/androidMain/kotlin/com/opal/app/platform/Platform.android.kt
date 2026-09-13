package com.opal.app.platform

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

@Composable
actual fun rememberSafePadding(): PaddingValues = WindowInsets.safeDrawing.asPaddingValues()
