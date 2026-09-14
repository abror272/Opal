package com.opal.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Opal — yagona, izchil chiziqli ikonlar to'plami.
 * Material-icons'ga bog'liq emas; 24x24 viewport, masshtablanuvchi stroke.
 */
enum class OpalIcon {
    Lock, Shield, Flame, Zap, Phone, Clock, Chart, Check,
    TrendUp, TrendDown, Trophy, Star, Moon, Gem, Target,
    Sliders, Play, ChevronRight, ChevronLeft, Eye,
    Hourglass, Plant, Person, Globe, Plus, Minus, Gear, Ring
}

@Composable
fun OpalIcons(icon: OpalIcon, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawOpalIcon(icon, tint) }
}

private fun DrawScope.drawOpalIcon(icon: OpalIcon, c: Color) {
    val s = size.minDimension / 24f
    val sw = 1.7f * s
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun pt(x: Float, y: Float) = Offset(x * s, y * s)

    fun poly(vararg xy: Float, close: Boolean = false) {
        val p = Path()
        p.moveTo(xy[0] * s, xy[1] * s)
        var i = 2
        while (i < xy.size) {
            p.lineTo(xy[i] * s, xy[i + 1] * s)
            i += 2
        }
        if (close) p.close()
        drawPath(p, c, style = stroke)
    }

    when (icon) {
        OpalIcon.Lock -> {
            // shackle
            val shackle = Path().apply {
                moveTo(8f * s, 11f * s)
                cubicTo(8f * s, 8.6f * s, 8.6f * s, 6f * s, 12f * s, 6f * s)
                cubicTo(15.4f * s, 6f * s, 16f * s, 8.6f * s, 16f * s, 11f * s)
            }
            drawPath(shackle, c, style = stroke)
            // body
            drawRoundRect(
                color = c,
                topLeft = pt(5.5f, 10.6f),
                size = Size(13f * s, 9.4f * s),
                cornerRadius = CornerRadius(2.8f * s),
                style = stroke
            )
            // keyhole
            drawCircle(c, radius = 1.3f * s, center = pt(12f, 14.4f))
            drawLine(c, pt(12f, 15.6f), pt(12f, 17.4f), strokeWidth = sw, cap = StrokeCap.Round)
        }

        OpalIcon.Shield -> {
            val p = Path().apply {
                moveTo(12f * s, 3f * s)
                lineTo(19f * s, 6f * s)
                lineTo(19f * s, 11.6f * s)
                cubicTo(19f * s, 16.4f * s, 16.1f * s, 19.6f * s, 12f * s, 21f * s)
                cubicTo(7.9f * s, 19.6f * s, 5f * s, 16.4f * s, 5f * s, 11.6f * s)
                lineTo(5f * s, 6f * s)
                close()
            }
            drawPath(p, c, style = stroke)
            poly(9f, 11.8f, 11.2f, 14f, 15.4f, 9.6f)
        }

        OpalIcon.Flame -> {
            val p = Path().apply {
                moveTo(12f * s, 2.6f * s)
                cubicTo(13.8f * s, 6.2f * s, 15.8f * s, 7.7f * s, 15.8f * s, 11.4f * s)
                cubicTo(15.8f * s, 15.6f * s, 14f * s, 18.6f * s, 12f * s, 21.4f * s)
                cubicTo(10f * s, 18.6f * s, 8.2f * s, 15.6f * s, 8.2f * s, 11.4f * s)
                cubicTo(8.2f * s, 7.7f * s, 10.2f * s, 6.2f * s, 12f * s, 2.6f * s)
                close()
            }
            drawPath(p, c, style = stroke)
            val inner = Path().apply {
                moveTo(12f * s, 20f * s)
                cubicTo(10.8f * s, 17.8f * s, 10.7f * s, 16f * s, 11.4f * s, 14.4f * s)
                cubicTo(11.9f * s, 13.2f * s, 12.6f * s, 12.4f * s, 12.6f * s, 11.3f * s)
                cubicTo(12.6f * s, 10.2f * s, 12f * s, 9.3f * s, 11.3f * s, 8.4f * s)
            }
            drawPath(inner, c, style = stroke)
        }

        OpalIcon.Zap -> {
            val p = Path().apply {
                moveTo(13.2f * s, 2f * s)
                lineTo(5.5f * s, 13f * s)
                lineTo(11f * s, 13f * s)
                lineTo(9.8f * s, 22f * s)
                lineTo(18.8f * s, 10.2f * s)
                lineTo(12.8f * s, 10.2f * s)
                close()
            }
            drawPath(p, c)
        }

        OpalIcon.Phone -> {
            drawRoundRect(
                color = c,
                topLeft = pt(7f, 3f),
                size = Size(10f * s, 18f * s),
                cornerRadius = CornerRadius(2.6f * s),
                style = stroke
            )
            drawLine(c, pt(10.4f, 5.4f), pt(13.6f, 5.4f), strokeWidth = sw, cap = StrokeCap.Round)
        }

        OpalIcon.Clock -> {
            drawCircle(c, radius = 8.6f * s, center = pt(12f, 12f), style = stroke)
            drawLine(c, pt(12f, 12f), pt(12f, 7.4f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(c, pt(12f, 12f), pt(15.6f, 13.6f), strokeWidth = sw, cap = StrokeCap.Round)
        }

        OpalIcon.Chart -> {
            drawRoundRect(c, pt(5.4f, 14f), Size(3.6f * s, 7f * s), CornerRadius(1.5f * s))
            drawRoundRect(c, pt(10.2f, 8.4f), Size(3.6f * s, 12.6f * s), CornerRadius(1.5f * s))
            drawRoundRect(c, pt(15f, 4.4f), Size(3.6f * s, 16.6f * s), CornerRadius(1.5f * s))
        }

        OpalIcon.Check -> poly(5f, 12.4f, 9.8f, 17.2f, 19f, 7.4f)

        OpalIcon.TrendUp -> {
            poly(3.4f, 16.8f, 9.2f, 11f, 13f, 14.2f, 20.4f, 6.6f)
            poly(15.8f, 6.6f, 20.6f, 6.6f, 20.6f, 11.4f)
        }

        OpalIcon.TrendDown -> {
            poly(3.4f, 7.2f, 9.2f, 13f, 13f, 9.8f, 20.4f, 17.4f)
            poly(15.8f, 17.4f, 20.6f, 17.4f, 20.6f, 12.6f)
        }

        OpalIcon.Trophy -> {
            val cup = Path().apply {
                moveTo(8f * s, 4f * s)
                lineTo(16f * s, 4f * s)
                lineTo(16f * s, 9.6f * s)
                cubicTo(16f * s, 13.4f * s, 14.3f * s, 15.7f * s, 12f * s, 16.6f * s)
                cubicTo(9.7f * s, 15.7f * s, 8f * s, 13.4f * s, 8f * s, 9.6f * s)
                close()
            }
            drawPath(cup, c, style = stroke)
            poly(8f, 6f, 4.8f, 6.4f, 4.8f, 11f, 8f, 12.4f)
            poly(16f, 6f, 19.2f, 6.4f, 19.2f, 11f, 16f, 12.4f)
            drawLine(c, pt(12f, 16.6f), pt(12f, 19f), strokeWidth = sw, cap = StrokeCap.Round)
            poly(9.4f, 19.6f, 14.6f, 19.6f)
        }

        OpalIcon.Star -> {
            val p = Path().apply {
                moveTo(12f * s, 3f * s)
                lineTo(14.12f * s, 9.09f * s)
                lineTo(20.56f * s, 9.22f * s)
                lineTo(15.42f * s, 13.11f * s)
                lineTo(17.29f * s, 19.28f * s)
                lineTo(12f * s, 15.6f * s)
                lineTo(6.71f * s, 19.28f * s)
                lineTo(8.58f * s, 13.11f * s)
                lineTo(3.44f * s, 9.22f * s)
                lineTo(9.88f * s, 9.09f * s)
                close()
            }
            drawPath(p, c)
        }

        OpalIcon.Moon -> {
            val p = Path().apply {
                moveTo(19.5f * s, 14.6f * s)
                cubicTo(15f * s, 16.1f * s, 11f * s, 14f * s, 9.5f * s, 10.4f * s)
                cubicTo(8.5f * s, 8.2f * s, 9f * s, 6f * s, 10.6f * s, 4.5f * s)
                cubicTo(6f * s, 5.5f * s, 4f * s, 9f * s, 4f * s, 12.6f * s)
                cubicTo(4f * s, 16.9f * s, 7.2f * s, 20f * s, 11.5f * s, 20f * s)
                cubicTo(15f * s, 20f * s, 17.6f * s, 17.9f * s, 19.5f * s, 14.6f * s)
                close()
            }
            drawPath(p, c)
        }

        OpalIcon.Gem -> {
            val p = Path().apply {
                moveTo(12f * s, 3f * s)
                lineTo(19f * s, 8f * s)
                lineTo(12f * s, 21f * s)
                lineTo(5f * s, 8f * s)
                close()
            }
            drawPath(p, c, style = stroke)
            poly(5f, 8f, 19f, 8f)
            poly(12f, 3f, 12f, 10.6f)
            poly(12f, 10.6f, 5f, 8f)
            poly(12f, 10.6f, 19f, 8f)
        }

        OpalIcon.Target -> {
            drawCircle(c, radius = 8.6f * s, center = pt(12f, 12f), style = stroke)
            drawCircle(c, radius = 4.4f * s, center = pt(12f, 12f), style = stroke)
            drawCircle(c, radius = 1.3f * s, center = pt(12f, 12f))
        }

        OpalIcon.Sliders -> {
            drawLine(c, pt(4f, 7f), pt(20f, 7f), strokeWidth = sw, cap = StrokeCap.Round)
            drawCircle(c, radius = 2.1f * s, center = pt(14.4f, 7f), style = Stroke(width = 1.7f * s))
            drawLine(c, pt(4f, 12f), pt(20f, 12f), strokeWidth = sw, cap = StrokeCap.Round)
            drawCircle(c, radius = 2.1f * s, center = pt(8.8f, 12f), style = Stroke(width = 1.7f * s))
            drawLine(c, pt(4f, 17f), pt(20f, 17f), strokeWidth = sw, cap = StrokeCap.Round)
            drawCircle(c, radius = 2.1f * s, center = pt(16.2f, 17f), style = Stroke(width = 1.7f * s))
        }

        OpalIcon.Play -> {
            val p = Path().apply {
                moveTo(9.2f * s, 6.2f * s)
                lineTo(19f * s, 12f * s)
                lineTo(9.2f * s, 17.8f * s)
                close()
            }
            drawPath(p, c)
        }

        OpalIcon.ChevronRight -> poly(9f, 6f, 15f, 12f, 9f, 18f)

        OpalIcon.Eye -> {
            val p = Path().apply {
                moveTo(2.5f * s, 12f * s)
                cubicTo(6f * s, 5.6f * s, 18f * s, 5.6f * s, 21.5f * s, 12f * s)
                cubicTo(18f * s, 18.4f * s, 6f * s, 18.4f * s, 2.5f * s, 12f * s)
                close()
            }
            drawPath(p, c, style = stroke)
            drawCircle(c, radius = 3f * s, center = pt(12f, 12f), style = stroke)
        }

        OpalIcon.ChevronLeft -> poly(15f, 6f, 9f, 12f, 15f, 18f)

        OpalIcon.Hourglass -> {
            val p = Path().apply {
                moveTo(7f * s, 3.5f * s)
                lineTo(17f * s, 3.5f * s)
                lineTo(17f * s, 7f * s)
                cubicTo(17f * s, 9.6f * s, 14.4f * s, 11f * s, 12f * s, 12f * s)
                cubicTo(14.4f * s, 13f * s, 17f * s, 14.4f * s, 17f * s, 17f * s)
                lineTo(17f * s, 20.5f * s)
                lineTo(7f * s, 20.5f * s)
                lineTo(7f * s, 17f * s)
                cubicTo(7f * s, 14.4f * s, 9.6f * s, 13f * s, 12f * s, 12f * s)
                cubicTo(9.6f * s, 11f * s, 7f * s, 9.6f * s, 7f * s, 7f * s)
                close()
            }
            drawPath(p, c, style = stroke)
            poly(7f, 3.5f, 17f, 3.5f)
            poly(7f, 20.5f, 17f, 20.5f)
        }

        OpalIcon.Plant -> {
            // tree / rest — toj + tana
            val crown = Path().apply {
                moveTo(12f * s, 3f * s)
                cubicTo(8.4f * s, 3f * s, 6f * s, 5.6f * s, 6f * s, 8.6f * s)
                cubicTo(4.4f * s, 9.4f * s, 3.5f * s, 11.2f * s, 4.2f * s, 13f * s)
                cubicTo(4.8f * s, 14.6f * s, 6.4f * s, 15.4f * s, 8f * s, 15.2f * s)
                cubicTo(8.4f * s, 16.6f * s, 10f * s, 17.4f * s, 11.4f * s, 17f * s)
                lineTo(11.4f * s, 21f * s)
                lineTo(12.6f * s, 21f * s)
                lineTo(12.6f * s, 17f * s)
                cubicTo(14f * s, 17.4f * s, 15.6f * s, 16.6f * s, 16f * s, 15.2f * s)
                cubicTo(17.6f * s, 15.4f * s, 19.2f * s, 14.6f * s, 19.8f * s, 13f * s)
                cubicTo(20.5f * s, 11.2f * s, 19.6f * s, 9.4f * s, 18f * s, 8.6f * s)
                cubicTo(18f * s, 5.6f * s, 15.6f * s, 3f * s, 12f * s, 3f * s)
                close()
            }
            drawPath(crown, c, style = stroke)
        }

        OpalIcon.Person -> {
            drawCircle(c, radius = 4.2f * s, center = pt(12f, 8f), style = stroke)
            val body = Path().apply {
                moveTo(4.5f * s, 20.5f * s)
                cubicTo(4.5f * s, 15.6f * s, 8f * s, 13.6f * s, 12f * s, 13.6f * s)
                cubicTo(16f * s, 13.6f * s, 19.5f * s, 15.6f * s, 19.5f * s, 20.5f * s)
            }
            drawPath(body, c, style = stroke)
        }

        OpalIcon.Globe -> {
            drawCircle(c, radius = 8.6f * s, center = pt(12f, 12f), style = stroke)
            // meridian
            val mer = Path().apply {
                moveTo(12f * s, 3.4f * s)
                cubicTo(8.5f * s, 6f * s, 8.5f * s, 18f * s, 12f * s, 20.6f * s)
                cubicTo(15.5f * s, 18f * s, 15.5f * s, 6f * s, 12f * s, 3.4f * s)
                close()
            }
            drawPath(mer, c, style = stroke)
            drawLine(c, pt(3.6f, 12f), pt(20.4f, 12f), strokeWidth = sw, cap = StrokeCap.Round)
        }

        OpalIcon.Plus -> {
            drawLine(c, pt(12f, 5f), pt(12f, 19f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(c, pt(5f, 12f), pt(19f, 12f), strokeWidth = sw, cap = StrokeCap.Round)
        }

        OpalIcon.Minus -> {
            drawLine(c, pt(5f, 12f), pt(19f, 12f), strokeWidth = sw, cap = StrokeCap.Round)
        }

        OpalIcon.Gear -> {
            drawCircle(c, radius = 3.2f * s, center = pt(12f, 12f), style = stroke)
            // 8 teeth as short radial lines
            var i = 0
            while (i < 8) {
                val ang = (i * 45f) * 0.017453292f
                val cx = 12f + kotlin.math.cos(ang) * 7.4f
                val cy = 12f + kotlin.math.sin(ang) * 7.4f
                val cx2 = 12f + kotlin.math.cos(ang) * 9.6f
                val cy2 = 12f + kotlin.math.sin(ang) * 9.6f
                drawLine(c, pt(cx, cy), pt(cx2, cy2), strokeWidth = sw, cap = StrokeCap.Round)
                i++
            }
            drawCircle(c, radius = 6.4f * s, center = pt(12f, 12f), style = Stroke(width = sw))
        }

        OpalIcon.Ring -> {
            drawCircle(c, radius = 8.4f * s, center = pt(12f, 12f), style = Stroke(width = 2.4f * s))
        }
    }
}
