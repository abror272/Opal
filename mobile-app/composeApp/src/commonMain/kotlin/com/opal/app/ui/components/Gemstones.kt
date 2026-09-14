package com.opal.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.UserProfileDto
import com.opal.app.glass.GlassCard
import com.opal.app.theme.OpalColors
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons

/**
 * GEMSTONES — haqiqiy Opal profil to'plami (web bilan bir xil 8 tosh).
 * Har tosh CSS radial-gradient blob o'rniga Canvas radial gradient bilan chiziladi.
 * Unlock shartlari web `gemsFor()` bilan aynan sinhron.
 */

data class OpalGem(
    val key: String,
    val name: String,
    val ownedPct: Int,
    val light: Color,
    val mid: Color,
    val deep: Color,
    val unlocked: Boolean,
    val desc: String
)

fun gemsFor(profile: UserProfileDto, hadSleepSession: Boolean): List<OpalGem> = listOf(
    OpalGem(
        "first", "First", 98,
        Color(0xFFA5F3FC), Color(0xFF67E8F9), Color(0xFF0E7490),
        profile.totalSessions >= 1, "Birinchi sessiya"
    ),
    OpalGem(
        "motivated", "Motivated", 95,
        Color(0xFFFBCFE8), Color(0xFFF0ABFC), Color(0xFFA21CAF),
        profile.totalSessions >= 5, "5 sessiya"
    ),
    OpalGem(
        "night-owl", "Night Owl", 41,
        Color(0xFFC7D2FE), Color(0xFFA5B4FC), Color(0xFF4338CA),
        hadSleepSession, "Uyqu sessiyasi"
    ),
    OpalGem(
        "pride", "Pride", 23,
        Color(0xFFFED7AA), Color(0xFFFDBA74), Color(0xFFC2410C),
        profile.streakDays >= 7, "7 kunlik streak"
    ),
    OpalGem(
        "iron-will", "Iron Will", 12,
        Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF334155),
        profile.totalSessions >= 50, "50 sessiya"
    ),
    OpalGem(
        "opal-plus", "Opal Plus", 9,
        Color(0xFFFDE68A), Color(0xFFFCD34D), Color(0xFF92400E),
        profile.plan == "PLUS", "Premium a'zo"
    ),
    OpalGem(
        "time-lord", "Time Lord", 7,
        Color(0xFFBBF7D0), Color(0xFF86EFAC), Color(0xFF15803D),
        profile.totalSavedMinutes >= 3000, "50 soat tejaldi"
    ),
    OpalGem(
        "century", "Century", 3,
        Color(0xFFFECACA), Color(0xFFF87171), Color(0xFF7F1D1D),
        profile.streakDays >= 100, "100 kunlik streak"
    )
)

/** Tosh — radial gradient "jewel" blob (yorug'lik nuqtasi bilan). */
@Composable
private fun GemBlob(gem: OpalGem, sizeDp: Dp) {
    Box(Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(sizeDp)) {
            val light = if (gem.unlocked) gem.light else gem.mid
            val mid = if (gem.unlocked) gem.mid else gem.deep.copy(alpha = 0.8f)
            val deep = if (gem.unlocked) gem.deep else Color(0xFF151B22)

            // asosiy tosh — diagonal gradient
            drawCircle(
                brush = Brush.linearGradient(
                    listOf(mid, deep),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                radius = size.minDimension / 2f
            )
            // yuqori yorug'lik nuqtasi
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(light.copy(alpha = if (gem.unlocked) 0.95f else 0.35f), Color.Transparent),
                    center = Offset(size.width * 0.34f, size.height * 0.28f),
                    radius = size.minDimension * 0.42f
                ),
                radius = size.minDimension / 2f
            )
            // qirga Fresnel halosi
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Transparent, light.copy(alpha = 0.25f)),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.minDimension * 0.52f
                ),
                radius = size.minDimension / 2f
            )
        }
        if (!gem.unlocked) {
            OpalIcons(OpalIcon.Lock, Color.White.copy(alpha = 0.75f), Modifier.size(sizeDp * 0.38f))
        }
    }
}

/** Gemstones karuseli — Profil ekrani uchun (web karuseli bilan bir xil tartib). */
@Composable
fun GemstonesCarousel(gems: List<OpalGem>, modifier: Modifier = Modifier) {
    val unlockedCount = gems.count { it.unlocked }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gemstones", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
            Text(
                "$unlockedCount/${gems.size} to'plandi",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = OpalColors.TextTertiary
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            gems.forEach { gem ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(84.dp)
                        .alpha(if (gem.unlocked) 1f else 0.55f)
                ) {
                    // halo
                    Box(contentAlignment = Alignment.Center) {
                        if (gem.unlocked) {
                            Box(
                                Modifier
                                    .size(66.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.radialGradient(
                                            listOf(gem.mid.copy(alpha = 0.28f), Color.Transparent)
                                        )
                                    )
                            )
                        }
                        GemBlob(gem, 54.dp)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        gem.name,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OpalColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Owned by ${gem.ownedPct}%",
                        fontSize = 9.5.sp,
                        color = OpalColors.TextTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

/** Profil ekraniga to'liq bo'lim — Gemstones kartasi ichida. */
@Composable
fun GemstonesSection(profile: UserProfileDto, hadSleepSession: Boolean, modifier: Modifier = Modifier) {
    val gems = remember(profile, hadSleepSession) { gemsFor(profile, hadSleepSession) }
    GlassCard(modifier.fillMaxWidth(), radius = 26.dp, padding = 16.dp) {
        GemstonesCarousel(gems)
    }
}
