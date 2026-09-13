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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.formatMinutes
import com.opal.app.glass.GlassCard
import com.opal.app.glass.GlassPane
import com.opal.app.theme.OpalColors
import com.opal.app.ui.components.AnimatedCount
import com.opal.app.ui.components.Avatar
import com.opal.app.ui.components.GradientButton
import com.opal.app.ui.components.ProgressRing
import com.opal.app.ui.components.SectionTitle
import com.opal.app.ui.components.StatChip
import com.opal.app.ui.components.TrendBadge
import com.opal.app.ui.components.WeekBarChart
import com.opal.app.data.weekdayLabelUz
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
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        // ---- Header ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Salom, ${profile.name} 👋",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpalColors.TextPrimary
                )
                Text(
                    if (profile.protectionEnabled) "Himoya faol, bugun zo'r kun" else "Himoya o'chirilgan",
                    fontSize = 13.sp,
                    color = OpalColors.TextSecondary,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Avatar(profile.name)
        }

        Spacer(Modifier.height(18.dp))

        // ---- Score / himoya kartasi ----
        GlassCard(Modifier.fillMaxWidth(), radius = 28.dp, padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = goalFrac,
                    modifier = Modifier.size(118.dp),
                    strokeWidth = 11.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedCount(
                            value = today?.savedMinutes ?: 0,
                            style = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
                        )
                        Text("daqiqa", fontSize = 10.sp, color = OpalColors.TextTertiary)
                        Text("tejaldi", fontSize = 10.sp, color = OpalColors.TextTertiary)
                    }
                }

                Spacer(Modifier.width(18.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Himoya", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                                .background(
                                    if (profile.protectionEnabled) OpalColors.Success.copy(alpha = 0.15f)
                                    else OpalColors.TextTertiary.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                if (profile.protectionEnabled) "YONIQ" else "O'CHIQ",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (profile.protectionEnabled) OpalColors.Success else OpalColors.TextSecondary
                            )
                        }
                    }
                    Text(
                        "🔥 ${profile.streakDays} kunlik streak",
                        fontSize = 12.5.sp,
                        color = OpalColors.Amber,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        "Maqsad: ${formatMinutes(stats.goalMinutes)} / kun",
                        fontSize = 12.sp,
                        color = OpalColors.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Switch(
                        checked = profile.protectionEnabled,
                        onCheckedChange = { checked -> scope.launch { repo.setProtection(checked) } },
                        modifier = Modifier.padding(top = 8.dp),
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

        Spacer(Modifier.height(14.dp))

        // ---- Stat chips ----
        Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip("⏳", formatMinutes(today?.screenTimeMinutes ?: 0), "Ekran vaqti", Modifier.weight(1f))
            StatChip("🛡️", formatMinutes(today?.savedMinutes ?: 0), "Tejaldi", Modifier.weight(1f))
            StatChip("👆", "${today?.pickups ?: 0}", "Olishlar", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // ---- CTA ----
        GradientButton(
            text = "Tez fokusni boshlash",
            modifier = Modifier.fillMaxWidth(),
            leading = { Text("⚡", fontSize = 16.sp) }
        ) { onStartFocus() }

        Spacer(Modifier.height(22.dp))

        // ---- Bloklangan ilovalar strip ----
        SectionTitle(
            "Bloklangan ilovalar",
            trailing = {
                Text(
                    "${blocked.size} ta",
                    fontSize = 12.sp,
                    color = OpalColors.TextSecondary
                )
            }
        )
        Spacer(Modifier.height(10.dp))
        if (blocked.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                Text("Hammasi ochiq ✨", fontSize = 13.sp, color = OpalColors.TextSecondary)
            }
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                blocked.forEachIndexed { i, app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            GlassPane(Modifier.size(64.dp), radius = 22.dp, base = 0.05f) {
                                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                                    Text(app.emoji, fontSize = 24.sp)
                                }
                            }
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(OpalColors.AccentDeep)
                            , contentAlignment = Alignment.Center) {
                                Text("🔒", fontSize = 9.sp)
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

        Spacer(Modifier.height(22.dp))

        // ---- Haftalik mini chart (Stats tabga olib boradi) ----
        GlassCard(Modifier.fillMaxWidth(), radius = 26.dp, padding = 16.dp, onClick = onOpenStats) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bu hafta", fontSize = 14.sp, color = OpalColors.TextSecondary)
                        AnimatedCount(
                            value = stats.weekSavedMinutes,
                            suffix = " daqiqa",
                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    TrendBadge(stats.trendPercent)
                }
                Spacer(Modifier.height(12.dp))
                WeekBarChart(
                    values = stats.days.map { it.screenTimeMinutes },
                    labels = stats.days.map { weekdayLabelUz(it.date) },
                    todayIndex = stats.days.indexOfLast { it.id == (today?.id ?: "") }.let { if (it < 0) stats.days.lastIndex else it },
                    chartHeight = 72.dp
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}
