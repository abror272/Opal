package com.opal.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.glass.GlassCard
import com.opal.app.glass.Pressable
import com.opal.app.platform.rememberSafePadding
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.GemstonesSection

@Composable
fun ProfileScreen(onClose: () -> Unit) {
    val repo = remember { AppGraph.repo }
    val insets = rememberSafePadding()
    val profile by repo.profile.collectAsState()
    val sessions by repo.sessions.collectAsState()
    val hadSleep = remember(sessions) { sessions.any { it.type == "SLEEP" } }

    val focusHours = profile.totalSavedMinutes / 60
    val topPercent = (100 - (focusHours + profile.streakDays) * 2).coerceIn(3, 50)

    Box(
        Modifier
            .fillMaxSize()
            .background(OpalColors.Bg)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OpalSpacing.xl)
        ) {
            Spacer(Modifier.height(insets.calculateTopPadding() + OpalSpacing.md))

            // ---- Header ----
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(OpalIcon.ChevronLeft, onClose)
                RoundIconButton(OpalIcon.Gear, {})
            }

            Spacer(Modifier.height(OpalSpacing.xl))

            // ---- Emblem + avatar ----
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Emblem(Modifier.size(150.dp)) {
                    Box(
                        Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(OpalColors.AccentSoft)
                            .border(1.5.dp, OpalColors.Accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        OpalIcons(OpalIcon.Person, OpalColors.Accent, Modifier.size(38.dp))
                    }
                }
            }

            Spacer(Modifier.height(OpalSpacing.lg))

            Text(
                profile.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OpalColors.TextPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                profile.handle,
                fontSize = 13.sp,
                color = OpalColors.TextTertiary,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp)
            )

            Spacer(Modifier.height(OpalSpacing.xxl))

            // ---- 3 stat ----
            Row(Modifier.fillMaxWidth()) {
                ProfileStat("$focusHours", "FOCUS HOURS", OpalIcon.Hourglass, OpalColors.Accent, Modifier.weight(1f))
                ProfileStat("${profile.streakDays}", "DAY STREAK", OpalIcon.Flame, OpalColors.Amber, Modifier.weight(1f))
                ProfileStat("Top $topPercent%", "WORLDWIDE", OpalIcon.Globe, OpalColors.MintLight, Modifier.weight(1f))
            }

            Spacer(Modifier.height(OpalSpacing.xxl))

            // ---- Gemstones ----
            GemstonesSection(profile = profile, hadSleepSession = hadSleep)

            Spacer(Modifier.height(OpalSpacing.lg))

            // ---- Time saved / avg screen ----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
                MiniStat("Tejalgan vaqt", "${profile.totalSavedMinutes / 60}s ${profile.totalSavedMinutes % 60}d", Modifier.weight(1f))
                MiniStat("Sessiyalar", "${profile.totalSessions}", Modifier.weight(1f))
            }

            Spacer(Modifier.height(OpalSpacing.xxl))

            Text(
                "Sayohatingiz to'liq birinchi haftadan boshlanadi.",
                fontSize = 13.sp,
                color = OpalColors.TextTertiary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(insets.calculateBottomPadding() + OpalSpacing.xxxl))
        }
    }
}

@Composable
private fun RoundIconButton(icon: OpalIcon, onClick: () -> Unit) {
    Pressable(onClick = onClick) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(0.5.dp, Color.White.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            OpalIcons(icon, OpalColors.TextPrimary, Modifier.size(19.dp))
        }
    }
}

@Composable
private fun Emblem(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f - 6f
            drawCircle(OpalColors.Accent.copy(alpha = 0.10f), radius = r, center = Offset(cx, cy))
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(OpalColors.Accent.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
            drawCircle(OpalColors.Accent.copy(alpha = 0.25f), radius = r, center = Offset(cx, cy), style = Stroke(1.4f))
        }
        content()
    }
}

@Composable
private fun ProfileStat(value: String, label: String, icon: OpalIcon, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        OpalIcons(icon, tint, Modifier.size(22.dp))
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OpalColors.TextPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = OpalColors.TextTertiary,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier, radius = OpalRadius.md, padding = 14.dp) {
        Column {
            Text(label, fontSize = 11.5.sp, color = OpalColors.TextTertiary)
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = OpalColors.TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
