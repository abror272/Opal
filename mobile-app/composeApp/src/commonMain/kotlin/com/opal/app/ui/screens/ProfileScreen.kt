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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.formatMinutes
import com.opal.app.glass.GlassCard
import com.opal.app.glass.Pressable
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalGradient
import com.opal.app.ui.components.AnimatedCount
import com.opal.app.ui.components.Avatar
import com.opal.app.ui.components.StatChip
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    val repo = remember { AppGraph.repo }
    val scope = rememberCoroutineScope()
    val profile by repo.profile.collectAsState()
    val apps by repo.apps.collectAsState()

    val isPlus = profile.plan == "PLUS"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        Text("Profil", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)

        Spacer(Modifier.height(16.dp))

        // ---- Identifikatsiya ----
        GlassCard(Modifier.fillMaxWidth(), radius = 28.dp, padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(profile.name, size = 60.dp, fontSize = 23)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
                    Text(profile.handle, fontSize = 13.sp, color = OpalColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
                    if (isPlus) {
                        Text(
                            "⭐ Opal Plus a'zosi",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OpalColors.Pink,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- Statistikalar ----
        Row(Modifier.fillMaxWidth().height(88.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip("🔥", "${profile.streakDays}", "Streak", Modifier.weight(1f))
            StatChip("🧠", "${profile.totalSessions}", "Sessiya", Modifier.weight(1f))
            StatChip("⏱️", formatMinutes(profile.totalSavedMinutes), "Tejaldi", Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // ---- Yutuqlar ----
        Text("Yutuqlar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
        Spacer(Modifier.height(10.dp))
        val achievements = listOf(
            Triple("🔥", "${profile.streakDays} kunlik streak", profile.streakDays >= 7),
            Triple("🧠", "10 sessiya", profile.totalSessions >= 10),
            Triple("⏱️", "5 soat tejaldi", profile.totalSavedMinutes >= 300),
            Triple("🚫", "3 ilova blokda", apps.count { it.blocked } >= 3),
            Triple("🛡️", "Himoyachi", profile.protectionEnabled),
            Triple("⭐", "Opal Plus", isPlus)
        )
        achievements.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (emoji, label, unlocked) ->
                    GlassCard(Modifier.weight(1f), radius = 20.dp, padding = 12.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                if (unlocked) emoji else "🔒",
                                fontSize = 22.sp,
                                modifier = Modifier.let { it }
                            )
                            Text(
                                label,
                                fontSize = 9.5.sp,
                                color = if (unlocked) OpalColors.TextPrimary else OpalColors.TextTertiary,
                                lineHeight = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(10.dp))

        // ---- Sozlamalar ----
        Text("Sozlamalar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth(), radius = 24.dp, padding = 4.dp) {
            Column {
                SettingRow("🛡️", "Himoya", profile.protectionEnabled) { checked ->
                    scope.launch { repo.setProtection(checked) }
                }
                SettingDivider()
                SettingRow("⚡", "Qat'iy rejim (strict)", profile.strictMode) { checked ->
                    scope.launch { repo.setStrict(checked) }
                }
                SettingDivider()

                // Kunlik maqsad
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎯", fontSize = 16.sp)
                    Spacer(Modifier.width(12.dp))
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

        Spacer(Modifier.height(16.dp))

        // ---- Opal Plus ----
        if (!isPlus) {
            Pressable(onClick = { scope.launch { repo.setPlan("PLUS") } }, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(OpalGradient)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("Opal Plus", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(
                            "Cheksiz rejimlar, ilg'or statistika, do'stlar bilan battle — bir oy bepul.",
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            "Sinab ko'rish →",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }
        } else {
            GlassCard(Modifier.fillMaxWidth(), radius = 26.dp, padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Opal Plus faol", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                        Text("Barcha premium imkoniyatlar ochiq", fontSize = 12.sp, color = OpalColors.TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Opal Mobile • Kotlin Multiplatform • v1.0",
            fontSize = 11.sp,
            color = OpalColors.TextTertiary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SettingRow(emoji: String, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(12.dp))
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
            .background(Color.White.copy(alpha = 0.08f))
    )
}
