package com.opal.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 7 kunlik bar chart — barlar stagger bilan o'sib chiqadi.
 * values: har kun qiymati, labels: Dush/Sesh..., todayIndex: alohida gradient beriladi.
 */
@Composable
fun WeekBarChart(
    values: List<Int>,
    labels: List<String>,
    todayIndex: Int,
    modifier: Modifier = Modifier,
    chartHeight: androidx.compose.ui.unit.Dp = 140.dp
) {
    val anims = remember(values) { List(values.size) { Animatable(0f) } }
    LaunchedEffect(values) {
        anims.forEachIndexed { i, a ->
            launch {
                delay(i * 55L)
                a.animateTo(1f, tween(750, easing = EaseOutCubic))
            }
        }
    }

    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(chartHeight)) {
            val chartMaxHeight = maxHeight
            val maxV = (values.maxOrNull() ?: 1).coerceAtLeast(1)
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                values.forEachIndexed { i, v ->
                    val h = chartMaxHeight * (v.toFloat() / maxV) * anims[i].value
                    val isToday = i == todayIndex
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.58f)
                                .height(h.coerceAtLeast(4.dp))
                                .background(
                                    if (isToday) OpalGradient
                                    else Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.34f),
                                            Color.White.copy(alpha = 0.10f)
                                        )
                                    ),
                                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { i, label ->
                Text(
                    label,
                    Modifier.weight(1f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = if (i == todayIndex) OpalColors.TextPrimary else OpalColors.TextTertiary,
                    fontWeight = if (i == todayIndex) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Trend line chart — silliq bezier egri chiziq, gradient fill, chapdan o'ngga "reveal" animatsiya.
 */
@Composable
fun TrendLineChart(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: androidx.compose.ui.unit.Dp = 120.dp
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1150, easing = EaseOutCubic))
    }

    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
            if (values.size < 2) return@Canvas
            val maxV = (values.maxOrNull() ?: 1).coerceAtLeast(1)
            val n = values.size
            val stepX = size.width / (n - 1)
            val topPad = size.height * 0.12f
            val bottomPad = size.height * 0.10f
            val usable = size.height - topPad - bottomPad

            val points = values.mapIndexed { i, v ->
                Offset(stepX * i, topPad + usable * (1f - v.toFloat() / maxV))
            }

            val line = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until n) {
                    val prev = points[i - 1]
                    val cur = points[i]
                    val dx = (cur.x - prev.x) / 2f
                    cubicTo(prev.x + dx, prev.y, cur.x - dx, cur.y, cur.x, cur.y)
                }
            }

            val fill = Path().apply {
                addPath(line)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }

            clipRect(right = size.width * progress.value) {
                drawPath(
                    fill,
                    Brush.verticalGradient(
                        listOf(Color(0x40B36BFF), Color(0x00B36BFF))
                    )
                )
                drawPath(
                    line,
                    brush = OpalGradient,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )
                points.forEachIndexed { i, p ->
                    if (p.x <= size.width * progress.value + 1f) {
                        val last = i == n - 1
                        drawCircle(
                            color = if (last) Color.White else Color.White.copy(alpha = 0.55f),
                            radius = if (last) 4.5.dp.toPx() else 2.6.dp.toPx(),
                            center = p
                        )
                        if (last) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.25f),
                                radius = 9.dp.toPx(),
                                center = p
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { i, label ->
                Text(
                    label,
                    Modifier.weight(1f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = if (i == labels.lastIndex) OpalColors.TextPrimary else OpalColors.TextTertiary
                )
            }
        }
    }
}
