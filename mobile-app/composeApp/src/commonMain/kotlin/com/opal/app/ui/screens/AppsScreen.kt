package com.opal.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.opal.app.theme.accentBrushFor
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.HexAvatar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import opalapp.composeapp.generated.resources.Res
import opalapp.composeapp.generated.resources.routine_deepwork
import opalapp.composeapp.generated.resources.routine_family
import opalapp.composeapp.generated.resources.routine_sleep

private data class Rule(
    val title: String,
    val time: String,
    val sub: String,
    val left: String?,
    val gradient: List<Color>,
    val icon: String,
    val photo: DrawableResource?
)

private val RULES = listOf(
    Rule("10 Unblock Daily", "Har kuni", "TikTok, Instagram +3", "7 left", listOf(Color(0xFF2A3B5C), Color(0xFF141C30)), "🔓", null),
    Rule("Sleep Time", "10PM — 8AM", "Block All", null, listOf(Color(0xFF1A1F3D), Color(0xFF0A0D20)), "🌙", Res.drawable.routine_sleep),
    Rule("Deep Work", "9AM — 5PM", "Block All, Except Productivity", null, listOf(Color(0xFF26221C), Color(0xFF0F0D0A)), "💻", Res.drawable.routine_deepwork),
    Rule("Lunch Break", "12—1PM", "Unblock Snapchat if blocked", null, listOf(Color(0xFF1C2626), Color(0xFF0A1010)), "🍽️", Res.drawable.routine_family)
)

@Composable
fun AppsScreen() {
    val repo = remember { AppGraph.repo }
    val scope = rememberCoroutineScope()
    val apps by repo.apps.collectAsState()

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
            HexAvatar(onClick = {})
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
                        BlockedTile(
                            emoji = app.emoji,
                            name = app.name,
                            modifier = Modifier.weight(1f)
                        ) { scope.launch { repo.setBlocked(app, false) } }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(OpalSpacing.lg))
            }
        }

        Spacer(Modifier.height(OpalSpacing.sm))

        // ---- Rules bento ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rules", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary.copy(alpha = 0.85f))
            Spacer(Modifier.width(4.dp))
            OpalIcons(OpalIcon.ChevronRight, OpalColors.TextTertiary, Modifier.size(15.dp))
        }
        Spacer(Modifier.height(OpalSpacing.md))
        RULES.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.md)) {
                row.forEach { rule -> RuleCard(rule, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(OpalSpacing.md))
        }

        Spacer(Modifier.height(OpalSpacing.sm))

        // ---- Apps (allowed) ----
        Text("Apps", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary.copy(alpha = 0.85f))
        Spacer(Modifier.height(OpalSpacing.md))
        allowed.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
                row.forEachIndexed { i, app ->
                    AllowedTile(
                        emoji = app.emoji,
                        name = app.name,
                        index = i,
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
private fun BlockedTile(emoji: String, name: String, modifier: Modifier = Modifier, onUnblock: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            GlassPane(Modifier.fillMaxWidth().aspectRatio(1f), radius = 16.dp, base = 0.06f) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 26.sp)
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B100D))
                    .border(1.dp, OpalColors.Accent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                OpalIcons(OpalIcon.Lock, OpalColors.Accent, Modifier.size(10.dp))
            }
        }
        Text(
            name,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = OpalColors.TextPrimary.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 7.dp)
        )
        Pressable(onClick = onUnblock) {
            Text("Unblock", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OpalColors.Accent)
        }
    }
}

@Composable
private fun AllowedTile(emoji: String, name: String, index: Int, modifier: Modifier = Modifier, onBlock: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(accentBrushFor(index))
                .clickable(onClick = onBlock),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 26.sp)
        }
        Text(
            name,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = OpalColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun RuleCard(rule: Rule, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(158.dp)
            .clip(RoundedCornerShape(OpalRadius.md))
            .background(Brush.linearGradient(rule.gradient))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(OpalRadius.md))
    ) {
        if (rule.photo != null) {
            Image(
                painter = painterResource(rule.photo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.10f),
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.75f)
                        )
                    )
            )
        }
        Column(
            Modifier.matchParentSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(rule.icon, fontSize = 20.sp)
            Column {
                if (rule.left != null) {
                    Box(
                        Modifier
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(OpalColors.Accent.copy(alpha = 0.12f))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(rule.left, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = OpalColors.MintLight)
                    }
                }
                Text(
                    rule.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 17.sp
                )
                Text(
                    rule.time,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    rule.sub,
                    fontSize = 9.5.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
