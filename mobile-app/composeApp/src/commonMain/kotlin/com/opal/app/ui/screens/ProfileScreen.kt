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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.formatMinutes
import com.opal.app.glass.GlassCard
import com.opal.app.glass.Pressable
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalGradient
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.Avatar
import com.opal.app.ui.components.GemstonesSection
import com.opal.app.ui.components.StatChip
import com.opal.app.ui.components.WeeklyReportCard
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    val repo = remember { AppGraph.repo }
    val scope = rememberCoroutineScope()
    val profile by repo.profile.collectAsState()
    val apps by repo.apps.collectAsState()
    val stats by repo.stats.collectAsState()
    val sessions by repo.sessions.collectAsState()

    val isPlus = profile.plan == "PLUS"
    val hadSleep = remember(sessions) { sessions.any { it.type == "SLEEP" } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = OpalSpacing.xl)
    ) {
        Spacer(Modifier.height(OpalSpacing.sm))

        Text("Profil", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary, letterSpacing = (-0.3).sp)

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- Identifikatsiya ----
        GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.xl, padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(profile.name, size = 60.dp, fontSize = 23)
                Spacer(Modifier.width(OpalSpacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(profile.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
                    Text(profile.handle, fontSize = 13.sp, color = OpalColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
                    if (isPlus) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            OpalIcons(OpalIcon.Star, OpalColors.MintLight, Modifier.size(12.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "Opal Plus a'zosi",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OpalColors.MintLight
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.md))

        // ---- Statistikalar ----
        Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
            StatChip(OpalIcon.Flame, "${profile.streakDays}", "Streak", Modifier.weight(1f), OpalColors.Amber)
            StatChip(OpalIcon.Clock, "${profile.totalSessions}", "Sessiya", Modifier.weight(1f), OpalColors.Accent)
            StatChip(OpalIcon.Shield, formatMinutes(profile.totalSavedMinutes), "Tejaldi", Modifier.weight(1f), OpalColors.Success)
        }

        Spacer(Modifier.height(OpalSpacing.xl))

        // ---- Haftalik hisobot ----
        WeeklyReportCard(stats = stats, profile = profile)

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- Gemstones ----
        GemstonesSection(profile = profile, hadSleepSession = hadSleep)

        Spacer(Modifier.height(OpalSpacing.xl))

        // ---- Yutuqlar ----
        Text("Yutuqlar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
        Spacer(Modifier.height(OpalSpacing.md))
        val achievements = listOf(
            Triple(OpalIcon.Flame, "${profile.streakDays} kunlik streak", profile.streakDays >= 7),
            Triple(OpalIcon.Clock, "10 sessiya", profile.totalSessions >= 10),
            Triple(OpalIcon.Shield, "5 soat tejaldi", profile.totalSavedMinutes >= 300),
            Triple(OpalIcon.Lock, "3 ilova blokda", apps.count { it.blocked } >= 3),
            Triple(OpalIcon.Target, "Himoyachi", profile.protectionEnabled),
            Triple(OpalIcon.Star, "Opal Plus", isPlus)
        )
        achievements.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
                row.forEach { (icon, label, unlocked) ->
                    GlassCard(Modifier.weight(1f), radius = OpalRadius.md, padding = 12.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (unlocked) OpalColors.Accent.copy(alpha = 0.16f)
                                        else Color.White.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                OpalIcons(
                                    if (unlocked) icon else OpalIcon.Lock,
                                    if (unlocked) OpalColors.Accent else OpalColors.TextTertiary,
                                    Modifier.size(17.dp)
                                )
                            }
                            Text(
                                label,
                                fontSize = 9.5.sp,
                                color = if (unlocked) OpalColors.TextPrimary else OpalColors.TextTertiary,
                                lineHeight = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(OpalSpacing.sm))
        }

        Spacer(Modifier.height(OpalSpacing.sm))

        // ---- Sozlamalar ----
        Text("Sozlamalar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
        Spacer(Modifier.height(OpalSpacing.md))

        GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.lg, padding = 4.dp) {
            Column {
                SettingRow(OpalIcon.Shield, "Himoya", profile.protectionEnabled) { checked ->
                    scope.launch { repo.setProtection(checked) }
                }
                SettingDivider()
                SettingRow(OpalIcon.Zap, "Qat'iy rejim (strict)", profile.strictMode) { checked ->
                    scope.launch { repo.setStrict(checked) }
                }
                SettingDivider()

                // Kunlik maqsad
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OpalIcons(OpalIcon.Target, OpalColors.TextSecondary, Modifier.size(17.dp))
                    Spacer(Modifier.width(OpalSpacing.md))
                    Text(
                        "Kunlik maqsad",
                        fontSize = 14.sp,
                        color = OpalColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(180, 240, 300, 360).forEach { g ->
                            val selected = profile.goalMinutes == g
                            Pressable(onClick = { scope.launch { repo.setGoalMinutes(g) } }) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selected) OpalColors.Accent.copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.06f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        formatMinutes(g),
                                        fontSize = 11.sp,
                                        color = if (selected) OpalColors.TextPrimary else OpalColors.TextSecondary,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- Opal Plus ----
        if (!isPlus) {
            Pressable(onClick = { scope.launch { repo.setPlan("PLUS") } }, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(OpalRadius.lg))
                        .background(OpalGradient)
                        .padding(OpalSpacing.xl)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OpalIcons(OpalIcon.Star, Color.White, Modifier.size(18.dp))
                            Spacer(Modifier.width(OpalSpacing.sm))
                            Text("Opal Plus", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(
                            "Cheksiz rejimlar, ilg'or statistika, do'stlar bilan battle — bir oy bepul.",
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            Text("Sinab ko'rish", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Spacer(Modifier.width(4.dp))
                            OpalIcons(OpalIcon.ChevronRight, Color.White, Modifier.size(14.dp))
                        }
                    }
                }
            }
        } else {
            GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.lg, padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OpalIcons(OpalIcon.Star, OpalColors.MintLight, Modifier.size(20.dp))
                    Spacer(Modifier.width(OpalSpacing.sm))
                    Column {
                        Text("Opal Plus faol", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                        Text("Barcha premium imkoniyatlar ochiq", fontSize = 12.sp, color = OpalColors.TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.xxl))

        Text(
            "Opal Mobile • Kotlin Multiplatform • v1.0",
            fontSize = 11.sp,
            color = OpalColors.TextTertiary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(OpalSpacing.xxxl))
    }
}

@Composable
private fun SettingRow(icon: OpalIcon, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OpalIcons(icon, OpalColors.TextSecondary, Modifier.size(17.dp))
        Spacer(Modifier.width(OpalSpacing.md))
        Text(
            title,
            fontSize = 14.sp,
            color = OpalColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
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

@Composable
private fun SettingDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(0.5.dp)
            .background(OpalColors.Divider)
    )
}
