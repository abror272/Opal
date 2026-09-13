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
 * Opal mobile — HAQIQIY Opal (Apple Design Award 2025) mint dizayn tili.
 * Web ilovadagi palitra bilan aynan sinhron: chuqur tungi fon (#05060F),
 * mint-yashil aksent (#86EFAC → #5EEAD4), oltin olov (streak), shisha yuzalar.
 */
object OpalColors {
    val Bg = Color(0xFF05060F)
    val BgElevated = Color(0xFF0A0F16)

    val Accent = Color(0xFF86EFAC)      // mint — asosiy aksent (web #86efac)
    val AccentDeep = Color(0xFF5EEAD4)  // teal-mint — gradient ikkinchi rangi
    val MintLight = Color(0xFFC9FBDC)   // yorqin mint matn (web #c9fbdc)
    val Success = Color(0xFF9FE8B5)     // ijobiy ko'rsatkichlar (web #9fe8b5)
    val Danger = Color(0xFFFF6B8B)      // rose — erta chiqish / o'chirish
    val Amber = Color(0xFFFFC46B)       // oltin olov — streak (web #ffc46b)

    val TextPrimary = Color(0xFFF4F7F5)
    val TextSecondary = Color(0x99FFFFFF)
    val TextTertiary = Color(0x5CFFFFFF)

    val Card = Color(0x12FFFFFF)
    val CardBorder = Color(0x22FFFFFF)
}

/** Haqiqiy Opal mint gradienti (CTA tugmalar, progress ringlar) */
val OpalGradient = Brush.linearGradient(
    listOf(Color(0xFF86EFAC), Color(0xFF5EEAD4), Color(0xFFB7F5CD))
)

/** Countdown ring sweep — mint oila */
val OpalSweep = Brush.sweepGradient(
    listOf(Color(0xFF86EFAC), Color(0xFF5EEAD4), Color(0xFFB7F5CD), Color(0xFF86EFAC))
)

/**
 * Ilovalar uchun barqaror avatar gradientlari.
 * Mint birinchi (brend), qolgani ilova ranglariga mos xilma-xil juftliklar.
 */
private val appPalettes = listOf(
    listOf(Color(0xFF86EFAC), Color(0xFF5EEAD4)),  // mint (brend)
    listOf(Color(0xFFFF8A5C), Color(0xFFFFC46B)),  // olov/oltin
    listOf(Color(0xFF6EE7B7), Color(0xFF38BDF8)),  // emerald-sky
    listOf(Color(0xFFFF6B8B), Color(0xFFFFC46B)),  // rose-amber
    listOf(Color(0xFF5EEAD4), Color(0xFF9FE8B5)),  // teal duo
    listOf(Color(0xFF8EF0E0), Color(0xFFC9FBDC))   // yengil mint
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
            onPrimary = Color(0xFF04120A),
            secondary = OpalColors.AccentDeep,
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
