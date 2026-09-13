package com.opal.app.platform

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberSafePadding(): PaddingValues {
    val window = remember {
        UIApplication.sharedApplication.keyWindow
            ?: UIApplication.sharedApplication.windows.firstOrNull()
    }
    val insets = window?.safeAreaInsets
    return PaddingValues(
        top = (insets?.top ?: 47.0).dp,
        bottom = (insets?.bottom ?: 34.0).dp
    )
}
