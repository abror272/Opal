package com.opal.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.formatClock
import com.opal.app.session.SessionController

/** Boshqa tablarda bo'lganda faol sessiyani ko'rsatuvchi suzuvchi pill. */
@Composable
fun SessionPill(
    controller: SessionController,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active by controller.active.collectAsState()
    val elapsed by controller.elapsedSeconds.collectAsState()
    val a = active ?: return

    val total = a.totalSeconds
    val remaining = (total - elapsed).coerceAtLeast(0)
    val progress = if (total <= 0) 0f else (elapsed.toFloat() / total).coerceIn(0f, 1f)

    val transition = rememberInfiniteTransition(label = "pill")
    val pulse by transition.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xD910131F))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        // progress fon
        Box(
            Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(Color(0x1F86EFAC), Color(0x1F5EEAD4))))
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .alpha(pulse)
                    .clip(CircleShape)
                    .background(Color(0xFF34D399))
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${a.preset.emoji} ${a.preset.label}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                formatClock(remaining),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFBFE9FF)
            )
        }
    }
}
