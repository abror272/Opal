package com.opal.app.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.UserProfileDto
import com.opal.app.glass.GlassCard
import com.opal.app.theme.OpalColors

/**
 * GEMSTONES — haqiqiy Opal profil to'plami (web `gemsFor()` bilan bir xil).
 * Endi har bir tosh AI-generatsiya qilingan haqiqiy rasm bilan ko'rsatiladi.
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
    OpalGem("first", "First", 98, Color(0xFFA5F3FC), Color(0xFF67E8F9), Color(0xFF0E7490), profile.totalSessions >= 1, "Birinchi sessiya"),
    OpalGem("motivated", "Motivated", 95, Color(0xFFFBCFE8), Color(0xFFF0ABFC), Color(0xFFA21CAF), profile.totalSessions >= 5, "5 sessiya"),
    OpalGem("night-owl", "Night Owl", 41, Color(0xFFC7D2FE), Color(0xFFA5B4FC), Color(0xFF4338CA), hadSleepSession, "Uyqu sessiyasi"),
    OpalGem("pride", "Pride", 23, Color(0xFFFED7AA), Color(0xFFFDBA74), Color(0xFFC2410C), profile.streakDays >= 7, "7 kunlik streak"),
    OpalGem("iron-will", "Iron Will", 12, Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF334155), profile.totalSessions >= 50, "50 sessiya"),
    OpalGem("opal-plus", "Opal Plus", 9, Color(0xFFFDE68A), Color(0xFFFCD34D), Color(0xFF92400E), profile.plan == "PLUS", "Premium a'zo"),
    OpalGem("time-lord", "Time Lord", 7, Color(0xFFBBF7D0), Color(0xFF86EFAC), Color(0xFF15803D), profile.totalSavedMinutes >= 3000, "50 soat tejaldi"),
    OpalGem("century", "Century", 3, Color(0xFFFECACA), Color(0xFFF87171), Color(0xFF7F1D1D), profile.streakDays >= 100, "100 kunlik streak")
)

/** Gemstones karuseli — Profil ekrani uchun. */
@Composable
fun GemstonesCarousel(gems: List<OpalGem>, modifier: Modifier = Modifier) {
    val unlockedCount = gems.count { it.unlocked }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Toshlar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
            Text(
                "$unlockedCount/${gems.size} to'plandi",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = OpalColors.TextTertiary
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            gems.forEach { gem ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(84.dp).alpha(if (gem.unlocked) 1f else 0.6f)
                ) {
                    Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
                        if (gem.unlocked) {
                            Box(
                                Modifier
                                    .size(66.dp)
                                    .background(Brush.radialGradient(listOf(gem.mid.copy(alpha = 0.30f), Color.Transparent)))
                            )
                        }
                        GemImage(gem.key, 56.dp, gem.unlocked)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        gem.name,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OpalColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "${gem.ownedPct}% egalik",
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

/** Profil ekraniga to'liq bo'lim. */
@Composable
fun GemstonesSection(profile: UserProfileDto, hadSleepSession: Boolean, modifier: Modifier = Modifier) {
    val gems = remember(profile, hadSleepSession) { gemsFor(profile, hadSleepSession) }
    GlassCard(modifier.fillMaxWidth(), radius = 26.dp, padding = 16.dp) {
        GemstonesCarousel(gems)
    }
}
