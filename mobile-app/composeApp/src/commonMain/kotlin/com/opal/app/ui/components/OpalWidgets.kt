package com.opal.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.theme.GemRainbowGradient
import com.opal.app.theme.OpalColors
import com.opal.app.theme.RingSweep
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons

/**
 * Opal hero kristali — iridescent faceted gem + glow + pedestal.
 */
@Composable
fun OpalGem(modifier: Modifier = Modifier, size: Dp = 190.dp) {
    val inf = rememberInfiniteTransition(label = "gem")
    val shimmer by inf.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmer"
    )
    val tilt by inf.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(6200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tilt"
    )

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h * 0.44f
            val r = w * 0.30f

            // ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(OpalColors.Accent.copy(alpha = 0.34f * shimmer), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r * 2.4f
                ),
                radius = r * 2.4f,
                center = Offset(cx, cy)
            )

            rotate(tilt, Offset(cx, cy)) {
                // iridescent play-of-color gradient
                val iridescent = Brush.linearGradient(
                    0.00f to Color(0xFFBDF6D4),
                    0.20f to Color(0xFF7FE7D0),
                    0.42f to Color(0xFF8FB4FF),
                    0.60f to Color(0xFFC79BFF),
                    0.78f to Color(0xFFFF9EC7),
                    1.00f to Color(0xFFFFD98A),
                    start = Offset(cx - r, cy - r),
                    end = Offset(cx + r, cy + r)
                )
                // gem body (faceted)
                val body = Path().apply {
                    moveTo(cx, cy - r)
                    cubicTo(cx + r * 0.5f, cy - r * 0.82f, cx + r * 0.9f, cy - r * 0.5f, cx + r * 0.86f, cy - r * 0.22f)
                    lineTo(cx + r * 0.72f, cy + r * 0.66f)
                    cubicTo(cx + r * 0.5f, cy + r * 1.08f, cx - r * 0.5f, cy + r * 1.08f, cx - r * 0.72f, cy + r * 0.66f)
                    lineTo(cx - r * 0.86f, cy - r * 0.22f)
                    cubicTo(cx - r * 0.9f, cy - r * 0.5f, cx - r * 0.5f, cy - r * 0.82f, cx, cy - r)
                    close()
                }
                drawPath(body, iridescent, alpha = 0.96f)

                // ichki nur (play of color)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x887FE7D0), Color(0x33C79BFF), Color.Transparent),
                        center = Offset(cx + r * 0.15f, cy + r * 0.1f),
                        radius = r * 0.9f
                    ),
                    radius = r * 0.9f,
                    center = Offset(cx + r * 0.15f, cy + r * 0.1f)
                )

                // internal facets
                val facet = Color.White.copy(alpha = 0.18f * shimmer)
                drawLine(facet, Offset(cx, cy - r), Offset(cx, cy + r * 1.02f), strokeWidth = 1.4f)
                drawLine(facet, Offset(cx - r * 0.86f, cy - r * 0.22f), Offset(cx + r * 0.72f, cy + r * 0.66f), strokeWidth = 1.1f)
                drawLine(facet, Offset(cx + r * 0.86f, cy - r * 0.22f), Offset(cx - r * 0.72f, cy + r * 0.66f), strokeWidth = 1.1f)
                drawLine(facet, Offset(cx - r * 0.86f, cy - r * 0.22f), Offset(cx + r * 0.86f, cy - r * 0.22f), strokeWidth = 1.0f)
                drawLine(facet, Offset(cx, cy - r), Offset(cx - r * 0.86f, cy - r * 0.22f), strokeWidth = 1.0f)
                drawLine(facet, Offset(cx, cy - r), Offset(cx + r * 0.86f, cy - r * 0.22f), strokeWidth = 1.0f)

                // specular highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.6f * shimmer), Color.Transparent),
                        center = Offset(cx - r * 0.30f, cy - r * 0.44f),
                        radius = r * 0.66f
                    ),
                    radius = r * 0.66f,
                    center = Offset(cx - r * 0.30f, cy - r * 0.44f)
                )
            }

            // pedestal
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF1C221E), Color(0xFF090C0A))),
                topLeft = Offset(cx - r * 0.42f, cy + r * 1.02f),
                size = Size(r * 0.84f, r * 0.20f),
                cornerRadius = CornerRadius(r * 0.10f)
            )
        }
    }
}

/** Katta Opal Score — raqam + mint uchburchak. */
@Composable
fun ScoreGauge(
    score: Int,
    improving: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Score",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = OpalColors.TextSecondary,
            letterSpacing = 0.6.sp
        )
        Row(verticalAlignment = Alignment.Top) {
            AnimatedCount(
                value = score,
                style = TextStyle(
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpalColors.Accent,
                    letterSpacing = (-1).sp
                )
            )
            Spacer(Modifier.width(6.dp))
            Canvas(Modifier.padding(top = 12.dp).size(16.dp)) {
                val p = Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(p, if (improving) OpalColors.Accent else OpalColors.Danger)
            }
        }
    }
}

/**
 * Stat ring pill — stadium shakl, chetida progress yoyi.
 */
@Composable
fun StatRingPill(
    icon: OpalIcon,
    value: Int,
    label: String,
    progress: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(950, easing = FastOutSlowInEasing),
        label = "ringPill"
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.045f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.matchParentSize()) {
                val rad = size.height / 2f
                val inset = 1.2f
                val rr = RoundRect(
                    left = inset, top = inset,
                    right = size.width - inset, bottom = size.height - inset,
                    cornerRadius = CornerRadius(rad, rad)
                )
                val path = Path().apply { addRoundRect(rr) }
                drawPath(path, OpalColors.Track, style = Stroke(1.4f))
                if (animated > 0.005f) {
                    val pm = PathMeasure()
                    pm.setPath(path, false)
                    val seg = Path()
                    pm.getSegment(0f, pm.length * animated, seg, true)
                    drawPath(seg, RingSweep, style = Stroke(2.4f, cap = StrokeCap.Round))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OpalIcons(icon, OpalColors.Accent, Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "$value",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpalColors.TextPrimary
                )
            }
        }
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = OpalColors.TextSecondary,
            modifier = Modifier.padding(top = 9.dp)
        )
    }
}

/** "N allowed" pill — ruxsat etilgan ilovalar (haqiqiy ikonlar). */
@Composable
fun AllowedPill(
    count: Int,
    icons: List<androidx.compose.ui.graphics.ImageBitmap?>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icons.take(3).forEachIndexed { i, bmp ->
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF1B201C)),
                contentAlignment = Alignment.Center
            ) {
                if (bmp != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("📱", fontSize = 11.sp)
                }
            }
            if (i != icons.take(3).lastIndex) Spacer(Modifier.width(2.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$count ta ruxsat",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = OpalColors.TextPrimary
        )
        Spacer(Modifier.width(4.dp))
        OpalIcons(OpalIcon.ChevronRight, OpalColors.TextSecondary, Modifier.size(14.dp))
    }
}

/**
 * To'q shisha tugma — haqiqiy Opal uslubi (mint gradient emas).
 * Yumshoq oq gradient + nozik chegara + oq matn.
 */
@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: OpalIcon = OpalIcon.Play,
    onClick: () -> Unit
) {
    val content = if (enabled) Color.White else Color.White.copy(alpha = 0.45f)
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (enabled) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.05f)))
                else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.03f)))
            )
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.20f else 0.10f), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OpalIcons(icon, content, Modifier.size(16.dp))
            Spacer(Modifier.width(9.dp))
            Text(
                text,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
    }
}
