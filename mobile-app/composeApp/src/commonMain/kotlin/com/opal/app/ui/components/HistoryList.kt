package com.opal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.FocusSessionDto
import com.opal.app.data.formatMinutes
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius

/** O'tgan fokus sessiyalari ro'yxati. */
@Composable
fun HistoryList(sessions: List<FocusSessionDto>, modifier: Modifier = Modifier, limit: Int = 8) {
    val items = sessions.take(limit)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OpalRadius.lg))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(OpalRadius.lg))
            .padding(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Tarix", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("${sessions.size}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            Text("Hali sessiya yo'q", fontSize = 12.5.sp, color = OpalColors.TextTertiary)
            return@Column
        }

        items.forEachIndexed { i, s ->
            HistoryRow(s)
            if (i != items.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HistoryRow(s: FocusSessionDto) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Text(s.emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                s.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OpalColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatMinutes(s.durationMinutes),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OpalColors.TextTertiary
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (s.completed) Color(0x1F86EFAC) else Color(0x1FFFB454)
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        if (s.completed) "To'liq" else "Erta",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (s.completed) Color(0xFFB7F5CD) else Color(0xFFFFD27A)
                    )
                }
                if (s.savedMinutes > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "+${formatMinutes(s.savedMinutes)}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpalColors.MintLight
                    )
                }
            }
        }
        if (s.focusScore > 0) {
            ScoreRing(s.focusScore)
        }
    }
}

@Composable
private fun ScoreRing(score: Int) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(Color(0xFF86EFAC), Color(0xFF5EEAD4), Color(0xFF3D9970), Color(0xFF86EFAC))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(31.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A0F12)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$score",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OpalColors.MintLight
            )
        }
    }
}
