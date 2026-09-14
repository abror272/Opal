package com.opal.app.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.glass.GlassPane
import com.opal.app.glass.Pressable
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalGradient
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.theme.OpalSweep
import com.opal.app.theme.accentBrushFor
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import kotlin.math.abs

/** Raqam silliq "count-up" animatsiya bilan o'zgaradi. */
@Composable
fun AnimatedCount(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary),
    suffix: String = ""
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(850, easing = EaseOutCubic),
        label = "count"
    )
    Text("$animated$suffix", modifier = modifier, style = style)
}

/** Gradient doira (avatar/app ikonka fonlari). */
@Composable
fun GradientCircle(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier
            .clip(CircleShape)
            .background(accentBrushFor(index)),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Ijobiy/negativ trend badge — ekran vaqti kamayishi yaxshi (yashil pastga strelka). */
@Composable
fun TrendBadge(percent: Int, modifier: Modifier = Modifier, invertGood: Boolean = true) {
    val good = if (invertGood) percent <= 0 else percent >= 0
    val color = if (good) OpalColors.Success else OpalColors.Danger
    val icon = if (percent <= 0) OpalIcon.TrendDown else OpalIcon.TrendUp
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OpalIcons(icon, color, Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "${abs(percent)}%",
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Qisqa statistika chipi — vektor ikon + qiymat + yorliq. */
@Composable
fun StatChip(
    icon: OpalIcon,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = OpalColors.Accent
) {
    GlassPane(modifier, radius = OpalRadius.md, base = 0.06f) {
        Column(
            Modifier
                .matchParentSize()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                OpalIcons(icon, tint, Modifier.size(15.dp))
            }
            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OpalColors.TextPrimary,
                modifier = Modifier.padding(top = 7.dp)
            )
            Text(
                label,
                fontSize = 10.sp,
                color = OpalColors.TextTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Bo'lim sarlavhasi + o'ng tomonda slot. */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
        trailing()
    }
}

/** Gradient primary tugma (press scale + ikon slot). */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: OpalIcon? = null,
    onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "btnScale"
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(OpalRadius.pill))
            .background(
                if (enabled) OpalGradient
                else Brush.linearGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.06f)))
            )
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) {
                OpalIcons(icon, if (enabled) Color.White else Color.White.copy(alpha = 0.5f), Modifier.size(17.dp))
            }
            Text(
                text,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

/** Gradient progress ring — aylana bo'ylab animatsiyalanuvchi yoy. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 13.dp,
    trackColor: Color = Color.White.copy(alpha = 0.07f),
    brush: Brush = OpalSweep,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val shown = remember(progress) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(progress) {
        shown.animateTo(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing))
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawRing(trackColor, brush, strokeWidth.toPx(), shown.value)
        }
        content()
    }
}

private fun DrawScope.drawRing(track: Color, brush: Brush, strokePx: Float, fraction: Float) {
    val inset = strokePx / 2 + 2f
    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
    val topLeft = Offset(inset, inset)

    drawCircle(
        color = track,
        radius = arcSize.minDimension / 2f,
        center = Offset(size.width / 2f, size.height / 2f),
        style = Stroke(width = strokePx)
    )

    if (fraction > 0.001f) {
        drawArc(
            brush = brush,
            startAngle = -90f,
            sweepAngle = 360f * fraction,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

/** Avatar — bosh harf bilan gradient doira. */
@Composable
fun Avatar(name: String, size: Dp = 44.dp, fontSize: Int = 17) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "A"
    GradientCircle(index = name.length, modifier = Modifier.size(size)) {
        Text(initial, color = Color.White, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    }
}
