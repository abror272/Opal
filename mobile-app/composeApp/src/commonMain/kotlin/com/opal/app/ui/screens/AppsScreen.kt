package com.opal.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.opal.app.data.InstalledApp
import com.opal.app.data.RuleSpec
import com.opal.app.data.currentMinutesOfDay
import com.opal.app.data.installedSocialPackages
import com.opal.app.data.minutesToTime
import com.opal.app.data.openBlockingSettings
import com.opal.app.data.parseTimeToMinutes
import com.opal.app.data.ruleStatusLabel
import com.opal.app.glass.Pressable
import com.opal.app.platform.rememberSafePadding
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.theme.accentBrushFor
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.BlockingSetupCard
import com.opal.app.ui.components.HexAvatar
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import opalapp.composeapp.generated.resources.Res
import opalapp.composeapp.generated.resources.routine_deepwork
import opalapp.composeapp.generated.resources.routine_family
import opalapp.composeapp.generated.resources.routine_sleep

private fun rulePhoto(key: String): DrawableResource? = when (key) {
    "sleep" -> Res.drawable.routine_sleep
    "deepwork" -> Res.drawable.routine_deepwork
    "family" -> Res.drawable.routine_family
    else -> null
}

private fun ruleGradient(key: String): List<Color> = when (key) {
    "sleep" -> listOf(Color(0xFF1A1F3D), Color(0xFF0A0D20))
    "deepwork" -> listOf(Color(0xFF26221C), Color(0xFF0F0D0A))
    "family" -> listOf(Color(0xFF1C2626), Color(0xFF0A1010))
    else -> listOf(Color(0xFF2A3B5C), Color(0xFF141C30))
}

@Composable
fun AppsScreen(onOpenProfile: () -> Unit = {}) {
    val repo = remember { AppGraph.repo }
    val apps by repo.installedApps.collectAsState()
    val blocked by repo.blockedPackages.collectAsState()
    val strict by repo.strictBlocking.collectAsState()
    val serviceOn by repo.blockingServiceOn.collectAsState()
    val rules by repo.rules.collectAsState()
    val loading by repo.appsLoading.collectAsState()
    val insets = rememberSafePadding()

    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<RuleSpec?>(null) }
    var nowMin by remember { mutableStateOf(currentMinutesOfDay()) }

    LaunchedEffect(Unit) {
        repo.loadDeviceData()
        // accessibility xizmati holatini kuzatib turish (sozlamalardan qaytganda)
        while (true) {
            delay(1500)
            repo.refreshBlockingService()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMin = currentMinutesOfDay()
        }
    }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val blockedApps = filtered.filter { blocked.contains(it.packageName) }
    val allowedApps = filtered.filterNot { blocked.contains(it.packageName) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OpalSpacing.xl)
                .padding(top = insets.calculateTopPadding())
        ) {
            Spacer(Modifier.height(OpalSpacing.sm))

            // ---- Header ----
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ilovalar",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpalColors.TextPrimary,
                    letterSpacing = (-0.3).sp
                )
                HexAvatar(onClick = onOpenProfile)
            }

            Spacer(Modifier.height(OpalSpacing.lg))

            // ---- Qat'iy bloklash holati ----
            StrictBlockCard(
                serviceOn = serviceOn,
                strict = strict,
                onToggle = { on ->
                    if (on && !serviceOn) {
                        openBlockingSettings()
                    } else {
                        repo.setStrictBlocking(on)
                    }
                },
                onOpenSettings = {
                    openBlockingSettings()
                }
            )

            Spacer(Modifier.height(OpalSpacing.lg))

            // ---- Qidiruv ----
            SearchBox(query) { query = it }

            Spacer(Modifier.height(OpalSpacing.lg))

            // ---- Blocked grid ----
            SectionTitle("Bloklangan", blockedApps.size)
            Spacer(Modifier.height(OpalSpacing.md))
            if (blockedApps.isEmpty()) {
                Text(
                    if (loading) "Yuklanmoqda…" else "Bloklangan ilova yo'q",
                    fontSize = 13.sp,
                    color = OpalColors.TextTertiary
                )
            } else {
                AppGrid(blockedApps, blocked, showLock = true) { app, isBlocked ->
                    repo.setPackageBlocked(app.packageName, !isBlocked)
                }
            }

            Spacer(Modifier.height(OpalSpacing.xl))

            // ---- Rules bento (tahrirlanadi) ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Qoidalar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpalColors.TextPrimary.copy(alpha = 0.85f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "tahrirlash uchun bosing",
                    fontSize = 10.5.sp,
                    color = OpalColors.TextTertiary
                )
            }
            Spacer(Modifier.height(OpalSpacing.md))
            rules.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.md)) {
                    row.forEach { rule ->
                        RuleCard(rule, ruleStatusLabel(rule, nowMin), Modifier.weight(1f)) { editing = rule }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(OpalSpacing.md))
            }

            Spacer(Modifier.height(OpalSpacing.lg))

            // ---- Apps (allowed) ----
            SectionTitle("Boshqa ilovalar", allowedApps.size)
            Spacer(Modifier.height(OpalSpacing.md))
            if (loading && apps.isEmpty()) {
                Text("Ilovalar yuklanmoqda…", fontSize = 13.sp, color = OpalColors.TextTertiary)
            } else if (allowedApps.isEmpty() && query.isNotBlank()) {
                Text("Hech narsa topilmadi", fontSize = 13.sp, color = OpalColors.TextTertiary)
            } else if (allowedApps.isEmpty()) {
                Text("Barcha ilovalar bloklangan 🎉", fontSize = 13.sp, color = OpalColors.TextTertiary)
            } else {
                AppGrid(allowedApps, blocked, showLock = false) { app, isBlocked ->
                    repo.setPackageBlocked(app.packageName, !isBlocked)
                }
            }

            Spacer(Modifier.height(OpalSpacing.xxxl))
        }

        editing?.let { rule ->
            RuleEditorSheet(
                initial = rule,
                apps = apps,
                onDismiss = { editing = null },
                onSave = { updated ->
                    repo.updateRule(updated)
                    editing = null
                }
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.07f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("$count", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextSecondary)
        }
    }
}

@Composable
private fun StrictBlockCard(
    serviceOn: Boolean,
    strict: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OpalRadius.lg))
            .background(
                if (strict && serviceOn) Brush.linearGradient(listOf(Color(0x2686EFAC), Color(0x0D5EEAD4)))
                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
            )
            .border(
                1.dp,
                if (strict && serviceOn) OpalColors.Accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(OpalRadius.lg)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (strict && serviceOn) OpalColors.Accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.07f)),
                    contentAlignment = Alignment.Center
                ) {
                    OpalIcons(OpalIcon.Shield, if (strict && serviceOn) OpalColors.Accent else OpalColors.TextSecondary, Modifier.size(17.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("Qat'iy bloklash", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
                    Text(
                        if (!serviceOn) "Xizmat o'chirilgan — yoqing"
                        else if (strict) "Bloklangan ilovalar darhol to'xtatiladi"
                        else "Bloklash faol emas",
                        fontSize = 11.sp,
                        color = OpalColors.TextTertiary
                    )
                }
                OpalSwitch(checked = strict && serviceOn, onCheckedChange = onToggle)
            }

            if (!serviceOn) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(OpalColors.Accent.copy(alpha = 0.14f))
                        .border(1.dp, OpalColors.Accent.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .clickable(onClick = onOpenSettings)
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sozlamalarda Opal bloklashni yoqish",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpalColors.MintLight
                    )
                }
                Spacer(Modifier.height(14.dp))
                BlockingSetupCard()
            }
        }
    }
}

@Composable
private fun OpalSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .width(50.dp)
            .height(29.dp)
            .clip(RoundedCornerShape(50))
            .background(if (checked) OpalColors.Accent else Color.White.copy(alpha = 0.14f))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .size(23.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun SearchBox(query: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔍", fontSize = 13.sp)
        Spacer(Modifier.width(9.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Ilova qidirish…", fontSize = 13.5.sp, color = OpalColors.TextTertiary)
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = OpalColors.TextPrimary, fontSize = 13.5.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Box(Modifier.size(18.dp).clip(CircleShape).clickable { onChange("") }, contentAlignment = Alignment.Center) {
                Text("✕", fontSize = 12.sp, color = OpalColors.TextTertiary)
            }
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<InstalledApp>,
    blocked: Set<String>,
    showLock: Boolean,
    onToggle: (InstalledApp, Boolean) -> Unit
) {
    apps.chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
            row.forEach { app ->
                AppTile(
                    app = app,
                    blocked = blocked.contains(app.packageName),
                    showLock = showLock,
                    modifier = Modifier.weight(1f)
                ) { onToggle(app, blocked.contains(app.packageName)) }
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(OpalSpacing.lg))
    }
}

@Composable
private fun AppTile(
    app: InstalledApp,
    blocked: Boolean,
    showLock: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onToggle)) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(15.dp))
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(15.dp))
                        .background(accentBrushFor(app.label.length)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        app.label.take(1).uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // bloklash holati belgisi
            if (showLock) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B100D))
                        .border(1.dp, OpalColors.Accent.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    OpalIcons(OpalIcon.Lock, OpalColors.Accent, Modifier.size(10.dp))
                }
            } else {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("＋", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }

        Text(
            app.label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (blocked) OpalColors.TextPrimary.copy(alpha = 0.85f) else OpalColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )

        Pressable(onClick = onToggle) {
            Text(
                if (blocked) "Ochish" else "Bloklash",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (blocked) OpalColors.Accent else OpalColors.TextTertiary
            )
        }
    }
}

@Composable
private fun RuleCard(
    rule: RuleSpec,
    status: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val photo = rulePhoto(rule.photo)
    Box(
        modifier
            .height(170.dp)
            .clip(RoundedCornerShape(OpalRadius.md))
            .background(Brush.linearGradient(ruleGradient(rule.photo)))
            .border(
                1.dp,
                if (rule.enabled) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(OpalRadius.md)
            )
            .clickable(onClick = onClick)
    ) {
        if (photo != null) {
            Image(
                painter = painterResource(photo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = if (rule.enabled) 0.10f else 0.45f),
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f)
                        )
                    )
            )
        }
        Column(
            Modifier.matchParentSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(rule.icon, fontSize = 20.sp)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (rule.enabled) OpalColors.Accent.copy(alpha = 0.18f)
                            else Color.White.copy(alpha = 0.10f)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (rule.enabled) "YONIQ" else "O'CHIQ",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (rule.enabled) OpalColors.MintLight else Color.White.copy(alpha = 0.55f),
                        letterSpacing = 0.6.sp
                    )
                }
            }
            Column {
                Text(
                    rule.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 17.sp
                )
                Text(
                    if (rule.type == "limit") "Har kuni" else "${rule.start} — ${rule.end}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    rule.subtitle,
                    fontSize = 9.5.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (status != null) {
                    val live = status == "Hozir faol"
                    Text(
                        if (live) "● $status" else status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (live) OpalColors.MintLight else Color.White.copy(alpha = 0.42f),
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}

/* ==================== Qoida tahrirlash ==================== */

@Composable
private fun RuleEditorSheet(
    initial: RuleSpec,
    apps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onSave: (RuleSpec) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var subtitle by remember { mutableStateOf(initial.subtitle) }
    var enabled by remember { mutableStateOf(initial.enabled) }
    var type by remember { mutableStateOf(initial.type) }
    var mode by remember { mutableStateOf(initial.mode) }
    var selectedApps by remember { mutableStateOf(initial.apps.toSet()) }
    var startMin by remember { mutableStateOf(parseTimeToMinutes(initial.start)) }
    var endMin by remember { mutableStateOf(parseTimeToMinutes(initial.end)) }
    var opens by remember { mutableStateOf(initial.opens) }
    val insets = rememberSafePadding()

    // Ro'yxat bo'sh bo'lsa — qurilmadagi standart chalg'ituvchilarni tanlab beramiz.
    LaunchedEffect(Unit) {
        if (selectedApps.isEmpty() && mode != "blockAll") {
            selectedApps = installedSocialPackages().ifEmpty { emptySet() }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss)
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Color(0xFF0B0E14))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.09f),
                    RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                )
                .padding(bottom = insets.calculateBottomPadding() + 18.dp)
        ) {
            // handle
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.20f))
                )
            }

            Column(
                Modifier
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(initial.icon, fontSize = 22.sp)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "Qoidani tahrirlash",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpalColors.TextPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    OpalSwitch(enabled) { enabled = it }
                }

                Spacer(Modifier.height(18.dp))

                FieldLabel("Nomi")
                EditorField(title) { title = it }

                Spacer(Modifier.height(14.dp))

                FieldLabel("Tavsif")
                EditorField(subtitle) { subtitle = it }

                Spacer(Modifier.height(18.dp))

                FieldLabel("Turi")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TypeChip("Jadval (vaqt)", type == "schedule") { type = "schedule" }
                    TypeChip("Limit (ochish soni)", type == "limit") { type = "limit" }
                }

                Spacer(Modifier.height(18.dp))

                if (type == "schedule") {
                    FieldLabel("Rejim")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeChip("🔒", "Hammasini bloklash", mode == "blockAll") { mode = "blockAll" }
                        ModeChip("🚫", "Tanlangan ilovalarni bloklash", mode == "block") { mode = "block" }
                        ModeChip("✅", "Tanlangan ilovalarni ochish", mode == "allow") { mode = "allow" }
                    }
                    if (mode != "blockAll") {
                        Spacer(Modifier.height(16.dp))
                        FieldLabel("Ilovalar — ${selectedApps.size} ta tanlandi")
                        AppPicker(apps, selectedApps) { pkg ->
                            selectedApps = if (pkg in selectedApps) selectedApps - pkg else selectedApps + pkg
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    FieldLabel("Vaqt oralig'i")
                    TimeRow("Boshlanish", startMin) { startMin = it }
                    Spacer(Modifier.height(10.dp))
                    TimeRow("Tugash", endMin) { endMin = it }
                } else {
                    FieldLabel("Kunlik ochish soni")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepperButton("−") { if (opens > 1) opens-- }
                        Text(
                            "$opens",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpalColors.TextPrimary,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.Center
                        )
                        StepperButton("＋") { if (opens < 200) opens++ }
                    }
                    Spacer(Modifier.height(16.dp))
                    FieldLabel("Ilovalar — ${selectedApps.size} ta tanlandi")
                    AppPicker(apps, selectedApps) { pkg ->
                        selectedApps = if (pkg in selectedApps) selectedApps - pkg else selectedApps + pkg
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.07f))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Bekor qilish", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextSecondary)
                    }
                    Box(
                        Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(listOf(OpalColors.Accent, Color(0xFF5EEAD4))))
                            .clickable {
                                onSave(
                                    initial.copy(
                                        title = title.ifBlank { initial.title },
                                        subtitle = subtitle,
                                        enabled = enabled,
                                        type = type,
                                        mode = if (type == "schedule") mode else "block",
                                        start = minutesToTime(startMin),
                                        end = minutesToTime(endMin),
                                        opens = opens,
                                        apps = if (type == "schedule" && mode == "blockAll") emptyList()
                                        else selectedApps.toList()
                                    )
                                )
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Saqlash", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF052E16))
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = OpalColors.TextTertiary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 7.dp)
    )
}

@Composable
private fun EditorField(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = OpalColors.TextPrimary, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) OpalColors.Accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (selected) OpalColors.Accent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) OpalColors.MintLight else OpalColors.TextSecondary
        )
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = OpalColors.TextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton("−") { onChange((minutes - 15 + 1440) % 1440) }
            Text(
                minutesToTime(minutes),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = OpalColors.TextPrimary,
                modifier = Modifier.width(76.dp),
                textAlign = TextAlign.Center
            )
            StepperButton("＋") { onChange((minutes + 15) % 1440) }
        }
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
    }
}

@Composable
private fun ModeChip(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) OpalColors.Accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
            .border(
                1.dp,
                if (selected) OpalColors.Accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.09f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 15.sp)
        Spacer(Modifier.width(9.dp))
        Text(
            label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) OpalColors.MintLight else OpalColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (selected) OpalColors.Accent else Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) OpalIcons(OpalIcon.Check, Color(0xFF052E16), Modifier.size(11.dp))
        }
    }
}

@Composable
private fun AppPicker(
    apps: List<InstalledApp>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    if (apps.isEmpty()) {
        Text("Ilovalar yuklanmoqda…", fontSize = 12.sp, color = OpalColors.TextTertiary)
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .verticalScroll(rememberScrollState())
    ) {
        apps.forEach { app ->
            val on = app.packageName in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(app.packageName) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    val ic = app.icon
                    if (ic != null) {
                        Image(bitmap = ic, contentDescription = null, modifier = Modifier.size(26.dp))
                    } else {
                        Text("📱", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    app.label,
                    fontSize = 12.5.sp,
                    color = OpalColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(19.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (on) OpalColors.Accent else Color.Transparent)
                        .border(
                            1.dp,
                            if (on) OpalColors.Accent else Color.White.copy(alpha = 0.25f),
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (on) OpalIcons(OpalIcon.Check, Color(0xFF052E16), Modifier.size(12.dp))
                }
            }
        }
    }
}
