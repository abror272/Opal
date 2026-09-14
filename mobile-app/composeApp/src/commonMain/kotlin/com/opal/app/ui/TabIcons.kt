package com.opal.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 3 ta asosiy tab (haqiqiy Opal kabi) — o'zbekcha. */
enum class TabKey(val title: String) {
    HOME("Asosiy"),
    APPS("Ilovalarim"),
    TIMER("Taymer")
}

/**
 * Tab ikonkalari — haqiqiy Opal uslubi:
 * Home = halqa + markaziy nuqta (Opal belgisi), Ilovalarim = 9 nuqta, Taymer = play.
 */
@Composable
fun OpalTabIcon(tab: TabKey, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawTabIcon(tab, tint)
    }
}

private fun DrawScope.drawTabIcon(tab: TabKey, c: Color) {
    val s = size.minDimension / 24f
    fun pt(x: Float, y: Float) = Offset(x * s, y * s)
    val stroke = Stroke(width = 1.9f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

    when (tab) {
        TabKey.HOME -> {
            // tashqi halqa
            drawCircle(c, radius = 8.2f * s, center = pt(12f, 12f), style = stroke)
            // markaziy to'ldirilgan nuqta
            drawCircle(c, radius = 3.1f * s, center = pt(12f, 12f))
        }

        TabKey.APPS -> {
            // 9 nuqta (3x3)
            val r = 1.9f * s
            listOf(4.6f, 12f, 19.4f).forEach { x ->
                listOf(4.6f, 12f, 19.4f).forEach { y ->
                    drawCircle(c, radius = r, center = pt(x, y))
                }
            }
        }

        TabKey.TIMER -> {
            // play uchburchagi (yumaloq burchakli)
            val p = Path().apply {
                moveTo(8.6f * s, 5.4f * s)
                lineTo(18.8f * s, 12f * s)
                lineTo(8.6f * s, 18.6f * s)
                close()
            }
            drawPath(p, c)
        }
    }
}

fun defaultIconSize(): Dp = 24.dp
