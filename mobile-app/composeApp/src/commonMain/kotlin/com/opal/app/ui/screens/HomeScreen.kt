package com.opal.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.computeScores
import com.opal.app.data.formatMinutes
import com.opal.app.glass.GlassCard
import com.opal.app.glass.GlassPane
import com.opal.app.glass.Pressable
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.AllowedPill
import com.opal.app.ui.components.CrystalHero
import com.opal.app.ui.components.GemImage
import com.opal.app.ui.components.GlassButton
import com.opal.app.ui.components.HexAvatar
import com.opal.app.ui.components.OpalWordmark
import com.opal.app.ui.components.ScoreBracket
import com.opal.app.ui.components.StatRingPill
import com.opal.app.ui.components.gemsFor
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private data class Suggestion(
    val category: String,
    val tag: String,
    val title: String,
    val body: String,
    val cta: String,
    val emoji: String,
    val icon: OpalIcon
)

private fun suggestionFor(hour: Int, overGoal: Boolean): Suggestion = when {
    overGoal -> Suggestion("Fokus", "Maqsad oshdi", "Ekrandan tanaffus kerak", "Bugungi ekran vaqti maqsaddan oshdi. Qisqa fokus sessiyasi reytingni tiklaydi.", "Fokusni boshlash", "🎯", OpalIcon.Hourglass)
    hour >= 22 || hour < 5 -> Suggestion("Uyqu", "Uyqu rejimi", "Yotish vaqti bo'ldi", "Telefon yotoqda ertangi kunning energiyasini o'g'irlaydi. Uyqu rejimini yoqing.", "Uyqu rejimini boshlash", "🌙", OpalIcon.Moon)
    hour in 18..21 -> Suggestion("Uyqu", "Oxirgi olish", "Tinchlash qiyinmi?", "Uyquga yengil kirish uchun yo'naltirilgan meditatsiyani sinab ko'ring.", "Meditatsiya va uyqu", "🌙", OpalIcon.Moon)
    hour in 14..17 -> Suggestion("Dam", "Tushlikdan keyin", "Kun o'rtasidagi pasayish?", "1 daqiqalik nafas mashg'uloti fokusni qayta tiklaydi.", "1 daqiqa nafas olish", "🌿", OpalIcon.Plant)
    hour in 11..13 -> Suggestion("Dam", "Tushlik yaqin", "Kichik dam rejimini rejalashtiring", "Ish rejimidan oldin qisqa dam bloki energiyani saqlab qoladi.", "Dam taymerini qo'yish", "🌿", OpalIcon.Plant)
    else -> Suggestion("Fokus", "Yangi kun", "Yangi kun — yangi rekord", "Chalg'ituvchilar ortga to'planishidan oldin chuqur fokus bilan boshlang.", "Deep Focus 45d", "🧠", OpalIcon.Hourglass)
}

@Composable
fun HomeScreen(
    onStartFocus: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenStats: () -> Unit,
    onBreathe: () -> Unit,
    onOpenApps: () -> Unit = {}
) {
    val repo = remember { AppGraph.repo }
    val scope = rememberCoroutineScope()
    val profile by repo.profile.collectAsState()
    val stats by repo.stats.collectAsState()
    val apps by repo.apps.collectAsState()
    val sessions by repo.sessions.collectAsState()
    val installed by repo.installedApps.collectAsState()
    val blockedPkgs by repo.blockedPackages.collectAsState()

    LaunchedEffect(Unit) { repo.loadDeviceData() }

    val scores = remember(profile, stats, sessions) { computeScores(profile, stats, sessions) }
    val today = stats.today
    val overGoal = today != null && today.screenTimeMinutes > today.goalMinutes
    val allowedReal = remember(installed, blockedPkgs) {
        installed.filterNot { blockedPkgs.contains(it.packageName) }
    }
    val hour = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour }
    val sug = remember(hour, overGoal) { suggestionFor(hour, overGoal) }
    val hadSleep = remember(sessions) { sessions.any { it.type == "SLEEP" } }
    val gems = remember(profile, hadSleep) { gemsFor(profile, hadSleep) }

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
            OpalWordmark()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OpalIcons(OpalIcon.Flame, OpalColors.Amber, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${profile.streakDays}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpalColors.Amber
                    )
                }
                Spacer(Modifier.width(OpalSpacing.md))
                HexAvatar(onOpenProfile)
            }
        }

        Spacer(Modifier.height(OpalSpacing.xs))

        // ---- Kristall qahramon ----
        CrystalHero(onClick = onStartFocus, modifier = Modifier.align(Alignment.CenterHorizontally))

        // ---- Score ----
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ball", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = OpalColors.TextSecondary, letterSpacing = 0.5.sp)
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${scores.score}",
                    style = TextStyle(
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OpalColors.Accent,
                        letterSpacing = (-1).sp,
                        shadow = androidx.compose.ui.graphics.Shadow(color = OpalColors.Accent.copy(alpha = 0.5f), blurRadius = 26f)
                    )
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (scores.delta >= 0) "▲" else "▼",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (scores.delta >= 0) OpalColors.Success else OpalColors.Danger,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(OpalSpacing.xs))

        // ---- bracket + pilllar ----
        ScoreBracket(Modifier.align(Alignment.CenterHorizontally))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OpalSpacing.md)
        ) {
            StatRingPill(OpalIcon.Moon, scores.sleep, "Uyqu", scores.sleep / 100f, Modifier.weight(1f), onOpenStats)
            StatRingPill(OpalIcon.Hourglass, scores.focus, "Fokus", scores.focus / 100f, Modifier.weight(1f), onOpenStats)
            StatRingPill(OpalIcon.Plant, scores.rest, "Dam", scores.rest / 100f, Modifier.weight(1f), onOpenStats)
        }

        Spacer(Modifier.height(OpalSpacing.xl))

        // ---- Tavsiya kartasi ----
        RecommendationCard(sug = sug, allowedCount = allowedReal.size, allowedIcons = allowedReal.take(3).map { it.icon }, onAction = onStartFocus, onDetails = onOpenStats, onAllowed = onOpenApps)

        Spacer(Modifier.height(OpalSpacing.xxl))

        // ---- Bugungi holat: Screen time + Himoya ----
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
            GlassCard(Modifier.weight(1.5f), radius = OpalRadius.lg, padding = 14.dp, onClick = onOpenStats) {
                Column(Modifier.fillMaxWidth()) {
                    Text("BUGUN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextTertiary, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            formatMinutes(today?.screenTimeMinutes ?: 0),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OpalColors.TextPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Ekran vaqti", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextTertiary)
                    }
                    Spacer(Modifier.height(OpalSpacing.sm))
                    val avg = stats.avgDailyScreenMinutes
                    val diff = kotlin.math.abs((today?.screenTimeMinutes ?: 0) - avg)
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background((if (overGoal) OpalColors.Danger else OpalColors.Success).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (overGoal) "▲ maqsaddan oshdi" else "▼ o'rtachadan ${formatMinutes(diff)}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (overGoal) OpalColors.Danger else OpalColors.Success
                        )
                    }
                }
            }

            GlassCard(Modifier.weight(1f), radius = OpalRadius.lg, padding = 14.dp) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (profile.protectionEnabled) OpalColors.SuccessSoft else Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            OpalIcons(
                                if (profile.protectionEnabled) OpalIcon.Shield else OpalIcon.Shield,
                                if (profile.protectionEnabled) OpalColors.Success else OpalColors.TextTertiary,
                                Modifier.size(13.dp)
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Text("Himoya", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
                    }
                    Spacer(Modifier.height(OpalSpacing.sm))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (profile.protectionEnabled) "Bloklangan" else "O'chirilgan",
                            fontSize = 9.5.sp,
                            color = OpalColors.TextTertiary
                        )
                        Switch(
                            checked = profile.protectionEnabled,
                            onCheckedChange = { v -> scope.launch { repo.setProtection(v) } },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = OpalColors.Success,
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.md))

        // ---- Start Timer CTA + nafas mashqi ----
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
            Box(Modifier.weight(1.5f)) {
                GlassButton(
                    text = "Taymerni boshlash",
                    modifier = Modifier.fillMaxWidth(),
                    icon = OpalIcon.Play,
                    onClick = onStartFocus
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .clickable(onClick = onBreathe),
                contentAlignment = Alignment.Center
            ) {
                Text("🌿 1 daq", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
            }
        }

        Spacer(Modifier.height(OpalSpacing.xl))

        // ---- My Apps strip ----
        Pressable(onClick = onOpenApps, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ilovalarim", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
                OpalIcons(OpalIcon.ChevronRight, OpalColors.TextTertiary, Modifier.size(15.dp))
            }
        }
        Spacer(Modifier.height(OpalSpacing.md))
        val myBlocked = remember(installed, blockedPkgs) {
            installed.filter { blockedPkgs.contains(it.packageName) }
        }
        if (myBlocked.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(OpalSpacing.md)
            ) {
                myBlocked.take(10).forEach { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            Box(
                                Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(17.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = app.label,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(46.dp)
                                    )
                                } else {
                                    Text(
                                        app.label.take(1).uppercase(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0C1410)),
                                contentAlignment = Alignment.Center
                            ) {
                                OpalIcons(OpalIcon.Lock, OpalColors.Accent, Modifier.size(10.dp))
                            }
                        }
                        Pressable(onClick = { repo.setPackageBlocked(app.packageName, false) }) {
                            Text(
                                "Ochish",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OpalColors.Accent,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                "Hali bloklangan ilova yo'q — Ilovalarim bo'limida tanlang",
                fontSize = 12.sp,
                color = OpalColors.TextTertiary
            )
        }

        Spacer(Modifier.height(OpalSpacing.xl))

        // ---- Gemstones teaser ----
        Pressable(onClick = onOpenProfile, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Toshlar", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
                Text(
                    "${gems.count { it.unlocked }}/${gems.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OpalColors.TextTertiary
                )
            }
        }
        Spacer(Modifier.height(OpalSpacing.md))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(OpalSpacing.md)
        ) {
            gems.take(5).forEach { g ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        if (g.unlocked) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.radialGradient(listOf(g.mid.copy(alpha = 0.28f), Color.Transparent))
                                    )
                            )
                        }
                        GemImage(g.key, 46.dp, g.unlocked)
                        if (!g.unlocked) {
                            OpalIcons(OpalIcon.Lock, OpalColors.TextTertiary, Modifier.size(12.dp))
                        }
                    }
                    Text(
                        g.name,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OpalColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.xxxl))
    }
}

@Composable
private fun RecommendationCard(
    sug: Suggestion,
    allowedCount: Int,
    allowedIcons: List<androidx.compose.ui.graphics.ImageBitmap?>,
    onAction: () -> Unit,
    onDetails: () -> Unit,
    onAllowed: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.lg, padding = 18.dp) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OpalIcons(sug.icon, OpalColors.Accent, Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(sug.category, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextSecondary)
                        Text("  /  ", fontSize = 12.5.sp, color = OpalColors.TextTertiary)
                        Text(sug.tag, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextTertiary)
                    }
                    Pressable(onClick = onDetails) {
                        Text("•••", fontSize = 15.sp, color = OpalColors.TextTertiary)
                    }
                }

                Spacer(Modifier.height(OpalSpacing.sm))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sug.title, fontSize = 16.5.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary, lineHeight = 21.sp)
                        Text(
                            sug.body,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = OpalColors.TextSecondary,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                    Spacer(Modifier.width(OpalSpacing.md))
                    Box(
                        Modifier
                            .size(56.dp)
                            .background(
                                Brush.radialGradient(listOf(OpalColors.Accent.copy(alpha = 0.22f), Color.Transparent))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sug.emoji, fontSize = 30.sp)
                    }
                }

                Spacer(Modifier.height(OpalSpacing.md))

                GlassButton(
                    text = sug.cta,
                    modifier = Modifier.fillMaxWidth(),
                    icon = OpalIcon.Play,
                    onClick = onAction
                )
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).offset(y = 16.dp)) {
            AllowedPill(count = allowedCount, icons = allowedIcons, onClick = onAllowed)
        }
    }
}
