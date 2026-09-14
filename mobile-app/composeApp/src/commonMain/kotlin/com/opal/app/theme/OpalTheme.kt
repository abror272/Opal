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
 * Opal — haqiqiy Opal (Apple Design Award) dizayn tili.
 * Chuqur tungi fon + yumshoq mint-yashil aksent + porlab turgan shisha yuzalar.
 */
object OpalColors {
    val Bg = Color(0xFF05080A)          // obsidian, sal yashil tusli
    val BgElevated = Color(0xFF0A0F12)

    val Accent = Color(0xFFA9E8B8)      // yumshoq mint (asosiy)
    val AccentDeep = Color(0xFF6FD98F)
    val MintLight = Color(0xFFD8F8E0)
    val Success = Color(0xFF86EFAC)
    val Danger = Color(0xFFFF5A76)
    val Amber = Color(0xFFFFB454)       // olov / streak
    val Cyan = Color(0xFF7FE7D0)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xB3FFFFFF)
    val TextTertiary = Color(0x66FFFFFF)

    val Card = Color(0x0AFFFFFF)
    val CardBorder = Color(0x1AFFFFFF)
    val Glow = Color(0x3398FFD9)

    // Yumshoq semantik tuslar
    val AccentSoft = Color(0x1FA9E8B8)
    val SuccessSoft = Color(0x1F86EFAC)
    val DangerSoft = Color(0x1FFF5A76)
    val AmberSoft = Color(0x1FFFB454)
    val Track = Color(0x0DFFFFFF)
    val Divider = Color(0x0FFFFFFF)
}

/** Izchil vertical/gorizontal oraliq tizimi. */
object OpalSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

/** Burchak radiusi tizimi. */
object OpalRadius {
    val sm = 14.dp
    val md = 20.dp
    val lg = 26.dp
    val xl = 32.dp
    val pill = 999.dp
}

/** Asosiy mint gradient. */
val OpalGradient = Brush.linearGradient(
    listOf(Color(0xFFA9E8B8), Color(0xFF6FD98F))
)

/** Gemstone rainbow gradienti (faceted effekt uchun). */
val GemRainbowGradient = Brush.linearGradient(
    listOf(
        Color(0xFF86EFAC),
        Color(0xFF38BDF8),
        Color(0xFFC084FC),
        Color(0xFFFB7185),
        Color(0xFFFBBF24)
    )
)

/** Score ring sweep — mint oila. */
val OpalSweep = Brush.sweepGradient(
    listOf(Color(0xFF86EFAC), Color(0xFF6FD98F), Color(0xFFD8F8E0), Color(0xFF86EFAC))
)

/** Ring pill yoyi uchun gradient (chapdan o'ngga). */
val RingSweep = Brush.sweepGradient(
    listOf(Color(0xFFD8F8E0), Color(0xFF6FD98F), Color(0xFF86EFAC), Color(0xFFD8F8E0))
)

/** Streak olov gradienti. */
val FlameGradient = Brush.linearGradient(
    listOf(Color(0xFFFFD166), Color(0xFFFF9A3C))
)

/**
 * Ilovalar uchun barqaror avatar gradientlari.
 */
private val appPalettes = listOf(
    listOf(Color(0xFF86EFAC), Color(0xFF6FD98F)),  // mint (brend)
    listOf(Color(0xFFFF8A5C), Color(0xFFFFC46B)),  // olov/oltin
    listOf(Color(0xFF6EE7B7), Color(0xFF38BDF8)),  // emerald-sky
    listOf(Color(0xFFFF6B8B), Color(0xFFFFC46B)),  // rose-amber
    listOf(Color(0xFF6FD98F), Color(0xFF9FE8B5)),  // teal duo
    listOf(Color(0xFF8EF0E0), Color(0xFFD8F8E0))   // yengil mint
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
