package com.opal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.formatMinutes
import com.opal.app.data.weekdayLabelUz
import com.opal.app.glass.GlassCard
import com.opal.app.glass.GlassPane
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.AnimatedCount
import com.opal.app.ui.components.Avatar
import com.opal.app.ui.components.GradientButton
import com.opal.app.ui.components.ProgressRing
import com.opal.app.ui.components.SectionTitle
import com.opal.app.ui.components.StatChip
import com.opal.app.ui.components.TrendBadge
import com.opal.app.ui.components.WeekBarChart
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onStartFocus: () -> Unit,
    onOpenStats: () -> Unit
) {
    val repo = remember { AppGraph.repo }
    val scope = rememberCoroutineScope()
    val profile by repo.profile.collectAsState()
    val stats by repo.stats.collectAsState()
    val apps by repo.apps.collectAsState()

    val today = stats.today
    val blocked = apps.filter { it.blocked }
    val goalFrac = if (today != null && stats.goalMinutes > 0) {
        (today.savedMinutes.toFloat() / stats.goalMinutes).coerceIn(0f, 1f)
    } else 0f

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
            Column {
                Text(
                    "Salom, ${profile.name}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpalColors.TextPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    if (profile.protectionEnabled) "Himoya faol — kun rejasi tayyor" else "Himoya o'chirilgan",
                    fontSize = 13.sp,
                    color = OpalColors.TextSecondary,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Avatar(profile.name)
        }

        Spacer(Modifier.height(OpalSpacing.xl))

        // ---- Himoya / maqsad kartasi ----
        GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.xl, padding = 20.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = goalFrac,
                    modifier = Modifier.size(122.dp),
                    strokeWidth = 11.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedCount(
                            value = today?.savedMinutes ?: 0,
                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
                        )
                        Text("daqiqa", fontSize = 10.sp, color = OpalColors.TextTertiary)
                        Text("tejaldi", fontSize = 10.sp, color = OpalColors.TextTertiary)
                    }
                }

                Spacer(Modifier.width(OpalSpacing.xl))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Himoya", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        StatusPill(active = profile.protectionEnabled)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OpalIcons(OpalIcon.Flame, OpalColors.Amber, Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${profile.streakDays} kunlik streak",
                            fontSize = 12.5.sp,
                            color = OpalColors.Amber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        "Maqsad: ${formatMinutes(stats.goalMinutes)} / kun",
                        fontSize = 12.sp,
                        color = OpalColors.TextSecondary,
                        modifier = Modifier.padding(top = 5.dp)
                    )

                    Switch(
                        checked = profile.protectionEnabled,
                        onCheckedChange = { checked -> scope.launch { repo.setProtection(checked) } },
                        modifier = Modifier.padding(top = 10.dp),
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = OpalColors.Accent,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.md))

        // ---- Stat chips ----
        Row(
            Modifier.fillMaxWidth().height(92.dp),
            horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)
        ) {
            StatChip(OpalIcon.Clock, formatMinutes(today?.screenTimeMinutes ?: 0), "Ekran vaqti", Modifier.weight(1f), OpalColors.Accent)
            StatChip(OpalIcon.Shield, formatMinutes(today?.savedMinutes ?: 0), "Tejaldi", Modifier.weight(1f), OpalColors.Success)
            StatChip(OpalIcon.Phone, "${today?.pickups ?: 0}", "Olishlar", Modifier.weight(1f), OpalColors.MintLight)
        }

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- CTA ----
        GradientButton(
            text = "Fokusni boshlash",
            modifier = Modifier.fillMaxWidth(),
            icon = OpalIcon.Play
        ) { onStartFocus() }

        Spacer(Modifier.height(OpalSpacing.xxl))

        // ---- Bloklangan ilovalar ----
        SectionTitle(
            "Bloklangan ilovalar",
            trailing = {
                Text("${blocked.size} ta", fontSize = 12.sp, color = OpalColors.TextSecondary)
            }
        )
        Spacer(Modifier.height(OpalSpacing.md))
        if (blocked.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OpalIcons(OpalIcon.Check, OpalColors.Success, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Hammasi ochiq", fontSize = 13.sp, color = OpalColors.TextSecondary)
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(OpalSpacing.md)
            ) {
                blocked.forEach { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            GlassPane(Modifier.size(62.dp), radius = 20.dp, base = 0.05f) {
                                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                                    Text(app.emoji, fontSize = 24.sp)
                                }
                            }
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(OpalColors.AccentDeep),
                                contentAlignment = Alignment.Center
                            ) {
                                OpalIcons(OpalIcon.Lock, Color(0xFF04241A), Modifier.size(11.dp))
                            }
                        }
                        Text(
                            app.name,
                            fontSize = 10.sp,
                            color = OpalColors.TextSecondary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.xxl))

        // ---- Haftalik mini chart ----
        GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.lg, padding = 16.dp, onClick = onOpenStats) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bu hafta", fontSize = 13.sp, color = OpalColors.TextSecondary)
                        AnimatedCount(
                            value = stats.weekSavedMinutes,
                            suffix = " daqiqa",
                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    TrendBadge(stats.trendPercent)
                }
                Spacer(Modifier.height(OpalSpacing.md))
                WeekBarChart(
                    values = stats.days.map { it.screenTimeMinutes },
                    labels = stats.days.map { weekdayLabelUz(it.date) },
                    todayIndex = stats.days.indexOfLast { it.id == (today?.id ?: "") }.let { if (it < 0) stats.days.lastIndex else it },
                    chartHeight = 72.dp
                )
            }
        }

        Spacer(Modifier.height(OpalSpacing.xxxl))
    }
}

@Composable
private fun StatusPill(active: Boolean) {
    val color = if (active) OpalColors.Success else OpalColors.TextTertiary
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                if (active) "Yoniq" else "O'chiq",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
