package com.opal.app.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.glass.GlassPane
import com.opal.app.glass.Pressable
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import kotlinx.coroutines.launch

@Composable
fun AppsScreen() {
    val repo = remember { AppGraph.repo }
    val scope = rememberCoroutineScope()
    val apps by repo.apps.collectAsState()
    val profile by repo.profile.collectAsState()

    val blocked = apps.filter { it.blocked }
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
            Text(
                "Apps",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = OpalColors.TextPrimary,
                letterSpacing = (-0.3).sp
            )
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (profile.protectionEnabled) OpalColors.AccentSoft else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                OpalIcons(
                    OpalIcon.Shield,
                    if (profile.protectionEnabled) OpalColors.Accent else OpalColors.TextTertiary,
                    Modifier.size(19.dp)
                )
            }
        }

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- Blocked grid ----
        Text("Blocked", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
        Spacer(Modifier.height(OpalSpacing.md))
        if (blocked.isEmpty()) {
            Text("Hozircha bloklangan ilova yo'q", fontSize = 13.sp, color = OpalColors.TextTertiary)
        } else {
            blocked.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
                    row.forEach { app ->
                        AppTile(
                            emoji = app.emoji,
                            name = app.name,
                            action = "Ochish",
                            modifier = Modifier.weight(1f)
                        ) { scope.launch { repo.setBlocked(app, false) } }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(OpalSpacing.lg))
            }
        }

        Spacer(Modifier.height(OpalSpacing.sm))

        // ---- Rules ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rules", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
            OpalIcons(OpalIcon.ChevronRight, OpalColors.TextTertiary, Modifier.size(16.dp))
        }
        Spacer(Modifier.height(OpalSpacing.md))

        val rules = listOf(
            Rule("10 Unblock Daily", "Har kuni", "7 left", listOf(Color(0xFF3B2E6E), Color(0xFF6D5BD0)), OpalIcon.Lock),
            Rule("Sleep Time", "22:00 — 08:00", "Block All", listOf(Color(0xFF1E3A4C), Color(0xFF2E7D8F)), OpalIcon.Moon),
            Rule("Deep Work", "09:00 — 17:00", "7s 5d gold", listOf(Color(0xFF3A3320), Color(0xFFB08A3C)), OpalIcon.Hourglass),
            Rule("Lunch Break", "12:00 — 13:00", "Snapchat", listOf(Color(0xFF3A2620), Color(0xFFB06A3C)), OpalIcon.Clock)
        )
        rules.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.md)) {
                row.forEach { rule ->
                    RuleCard(rule, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(OpalSpacing.md))
        }

        Spacer(Modifier.height(OpalSpacing.xxl))

        // ---- Allowed ----
        Text("Allowed (${allowed.size})", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
        Spacer(Modifier.height(OpalSpacing.md))
        allowed.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
                row.forEach { app ->
                    AppTile(
                        emoji = app.emoji,
                        name = app.name,
                        action = "Bloklash",
                        modifier = Modifier.weight(1f)
                    ) { scope.launch { repo.setBlocked(app, true) } }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(OpalSpacing.lg))
        }

        Spacer(Modifier.height(OpalSpacing.xxxl))
    }
}

@Composable
private fun AppTile(
    emoji: String,
    name: String,
    action: String,
    modifier: Modifier = Modifier,
    onAction: () -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        GlassPane(
            Modifier.size(62.dp),
            radius = 20.dp,
            base = 0.06f
        ) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 27.sp)
            }
        }
        Text(
            name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = OpalColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 7.dp)
        )
        Pressable(onClick = onAction) {
            Text(
                action,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = OpalColors.Accent,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private data class Rule(
    val title: String,
    val subtitle: String,
    val badge: String,
    val colors: List<Color>,
    val icon: OpalIcon
)

@Composable
private fun RuleCard(rule: Rule, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(158.dp)
            .clip(RoundedCornerShape(OpalRadius.md))
            .background(Brush.linearGradient(rule.colors))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(OpalRadius.md))
    ) {
        // subtle icon watermark
        Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            OpalIcons(rule.icon, Color.White.copy(alpha = 0.35f), Modifier.size(30.dp))
        }
        Column(
            Modifier.matchParentSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(rule.badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column {
                Text(rule.title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    rule.subtitle,
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
