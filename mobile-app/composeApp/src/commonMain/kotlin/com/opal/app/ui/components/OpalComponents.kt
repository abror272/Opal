package com.opal.app.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalGradient
import com.opal.app.theme.OpalSweep
import com.opal.app.theme.accentBrushFor

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

/** Ijobiy/negativ trend badge (▼ 8% yashil, ▲ 12% qizil — ekran vaqti kamayishi yaxshi). */
@Composable
fun TrendBadge(percent: Int, modifier: Modifier = Modifier, invertGood: Boolean = true) {
    val good = if (invertGood) percent <= 0 else percent >= 0
    val color = if (good) OpalColors.Success else OpalColors.Danger
    val arrow = if (percent <= 0) "▼" else "▲"
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            "$arrow ${kotlin.math.abs(percent)}%",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Qisqa statistika chipi (emoji + qiymat + yorliq). */
@Composable
fun StatChip(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    com.opal.app.glass.GlassPane(modifier, radius = 20.dp, base = 0.06f) {
        Column(
            Modifier.matchParentSize().padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 17.sp)
            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OpalColors.TextPrimary,
                modifier = Modifier.padding(top = 5.dp)
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
        modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
        trailing()
    }
}

/** Gradient primary tugma (press scale + glow shadow bilan). */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
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
            .clip(RoundedCornerShape(50))
            .background(
                if (enabled) OpalGradient
                else Brush.linearGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.06f)))
            )
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            leading?.invoke()
            Text(
                text,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

/**
 * Progress ring — gradient arc + silliq animatsiya.
 * progress 0..1.
 */
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
    androidx.compose.runtime.LaunchedEffect(progress) {
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
