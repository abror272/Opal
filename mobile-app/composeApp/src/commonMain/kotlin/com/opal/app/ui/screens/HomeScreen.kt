package com.opal.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.computeScores
import com.opal.app.glass.GlassCard
import com.opal.app.glass.Pressable
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalGradient
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.AllowedPill
import com.opal.app.ui.components.GlassButton
import com.opal.app.ui.components.OpalGem
import com.opal.app.ui.components.ScoreGauge
import com.opal.app.ui.components.StatRingPill

@Composable
fun HomeScreen(
    onStartFocus: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenStats: () -> Unit
) {
    val repo = remember { AppGraph.repo }
    val profile by repo.profile.collectAsState()
    val stats by repo.stats.collectAsState()
    val apps by repo.apps.collectAsState()
    val sessions by repo.sessions.collectAsState()

    val scores = remember(profile, stats, sessions) { computeScores(profile, stats, sessions) }
    val allowed = apps.filter { !it.blocked }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = OpalSpacing.xl)
    ) {
        Spacer(Modifier.height(OpalSpacing.sm))

        // ---- Header ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Opal wordmark
            Row(verticalAlignment = Alignment.CenterVertically) {
                OpalIcons(OpalIcon.Ring, OpalColors.TextPrimary, Modifier.size(19.dp))
                Text(
                    "pal",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpalColors.TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // streak
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OpalIcons(OpalIcon.Flame, OpalColors.Amber, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${profile.streakDays}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpalColors.TextPrimary
                    )
                }
                Spacer(Modifier.width(OpalSpacing.md))
                // profile
                Pressable(onClick = onOpenProfile) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OpalColors.AccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        OpalIcons(OpalIcon.Person, OpalColors.Accent, Modifier.size(19.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.sm))

        // ---- Gem + Score ----
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            OpalGem(size = 210.dp)
        }

        ScoreGauge(score = scores.overall, improving = scores.improving, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(OpalSpacing.sm))
        ScoreBracket(Modifier.fillMaxWidth().height(20.dp))
        Spacer(Modifier.height(OpalSpacing.xs))

        // ---- Sleep / Focus / Rest ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)
        ) {
            StatRingPill(OpalIcon.Moon, scores.sleep, "Sleep", scores.sleep / 100f, Modifier.weight(1f))
            StatRingPill(OpalIcon.Hourglass, scores.focus, "Focus", scores.focus / 100f, Modifier.weight(1f))
            StatRingPill(OpalIcon.Plant, scores.rest, "Rest", scores.rest / 100f, Modifier.weight(1f))
        }

        Spacer(Modifier.height(OpalSpacing.xl))

        // ---- Tavsiya kartasi ----
        RecommendationCard(
            scores = scores,
            onAction = onStartFocus,
            onDetails = onOpenStats
        )

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- Allowed pill ----
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AllowedPill(
                count = allowed.size,
                emojis = allowed.map { it.emoji },
                onClick = onOpenStats
            )
        }

        Spacer(Modifier.height(OpalSpacing.xxxl))
    }
}

/** Score'ni pill markazlariga bog'lovchi ingichka qavs. */
@Composable
private fun ScoreBracket(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val third = w / 6f
        val col = Color.White.copy(alpha = 0.16f)
        val midY = h * 0.6f
        drawLine(col, Offset(w / 2f, 0f), Offset(w / 2f, midY), strokeWidth = 1.4f)
        drawLine(col, Offset(third, midY), Offset(w - third, midY), strokeWidth = 1.4f)
        drawLine(col, Offset(third, midY), Offset(third, h), strokeWidth = 1.4f)
        drawLine(col, Offset(w / 2f, midY), Offset(w / 2f, h), strokeWidth = 1.4f)
        drawLine(col, Offset(w - third, midY), Offset(w - third, h), strokeWidth = 1.4f)
    }
}

@Composable
private fun RecommendationCard(
    scores: com.opal.app.data.OpalScores,
    onAction: () -> Unit,
    onDetails: () -> Unit
) {
    val lowest = minOf(scores.sleep, scores.focus, scores.rest)
    val (title, desc, action, icon) = when (lowest) {
        scores.sleep -> Tip("Uxlash qiyinmi?", "Meditatsiya bilan tinch uxlashga tayyorlaning", "Meditatsiya va uyqu", OpalIcon.Moon)
        scores.focus -> Tip("Diqqatni jamlang", "Chalg'ituvchilarni o'chirib, chuqur fokusga o'ting", "Chuqur fokus", OpalIcon.Hourglass)
        else -> Tip("Dam oling", "Ekrandan tanaffus qiling va quvvat yig'ing", "Dam olish", OpalIcon.Plant)
    }
    val topLabel = if (lowest == scores.sleep) "Uyqu" else if (lowest == scores.focus) "Fokus" else "Dam"
    val topIcon = icon
    val secondIcon = OpalIcon.Clock
    val secondLabel = "Oxirgi olish"

    GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.xl, padding = 18.dp) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OpalIcons(topIcon, OpalColors.Accent, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(topLabel, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                    Text("  /  ", fontSize = 13.sp, color = OpalColors.TextTertiary)
                    OpalIcons(secondIcon, OpalColors.TextSecondary, Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(secondLabel, fontSize = 13.5.sp, color = OpalColors.TextSecondary)
                }
                Text("•••", fontSize = 15.sp, color = OpalColors.TextTertiary)
            }

            Spacer(Modifier.height(OpalSpacing.md))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
                    Text(
                        desc,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = OpalColors.TextSecondary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.width(OpalSpacing.md))
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(OpalColors.AccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    OpalIcons(icon, OpalColors.MintLight, Modifier.size(34.dp))
                }
            }

            Spacer(Modifier.height(OpalSpacing.lg))

            // asosiy CTA
            GlassButton(
                text = action,
                modifier = Modifier.fillMaxWidth(),
                icon = OpalIcon.Play,
                onClick = onAction
            )

            Spacer(Modifier.height(OpalSpacing.sm))

            Pressable(onClick = onDetails, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Batafsil statistikani ko'rish",
                    fontSize = 12.5.sp,
                    color = OpalColors.TextTertiary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private data class Tip(val title: String, val desc: String, val action: String, val icon: OpalIcon)
