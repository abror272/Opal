package com.opal.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Opal mobile — zamonaviy qorong'u tema.
 * Chuqur tungi fon + binafsha→pushti gradient aksent + shisha (glass) yuzalar.
 */
object OpalColors {
    val Bg = Color(0xFF05060B)
    val BgElevated = Color(0xFF0B0C16)

    val Accent = Color(0xFFB18CFF)
    val AccentDeep = Color(0xFF7C5CFF)
    val Pink = Color(0xFFFF7AD9)
    val Success = Color(0xFF3DDC97)
    val Danger = Color(0xFFFF6B8B)
    val Amber = Color(0xFFFFC163)

    val TextPrimary = Color(0xFFF4F4FA)
    val TextSecondary = Color(0x99FFFFFF)
    val TextTertiary = Color(0x5CFFFFFF)

    val Card = Color(0x12FFFFFF)
    val CardBorder = Color(0x22FFFFFF)
}

val OpalGradient = Brush.linearGradient(
    listOf(Color(0xFF7C5CFF), Color(0xFFB36BFF), Color(0xFFFF7AD9))
)

val OpalSweep = Brush.sweepGradient(
    listOf(Color(0xFF7C5CFF), Color(0xFFB36BFF), Color(0xFFFF7AD9), Color(0xFF7C5CFF))
)

private val appPalettes = listOf(
    listOf(Color(0xFF7C5CFF), Color(0xFFB36BFF)),
    listOf(Color(0xFFFF7AD9), Color(0xFFFF9F5A)),
    listOf(Color(0xFF3DDC97), Color(0xFF7CE8FF)),
    listOf(Color(0xFFFF6B8B), Color(0xFFB36BFF)),
    listOf(Color(0xFFFFC163), Color(0xFFFF7AD9)),
    listOf(Color(0xFF6BD6FF), Color(0xFFB36BFF))
)

/** Ilova indeksiga qarab barqaror gradient (avatar doiralari uchun). */
fun accentBrushFor(index: Int): Brush {
    val p = appPalettes[((index % appPalettes.size) + appPalettes.size) % appPalettes.size]
    return Brush.linearGradient(p)
}

@Composable
fun OpalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = OpalColors.Accent,
            onPrimary = Color.White,
            secondary = OpalColors.Pink,
            background = OpalColors.Bg,
            onBackground = OpalColors.TextPrimary,
            surface = OpalColors.BgElevated,
            onSurface = OpalColors.TextPrimary,
            surfaceVariant = OpalColors.Card,
            error = OpalColors.Danger
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(34.dp)
        ),
        content = content
    )
}
