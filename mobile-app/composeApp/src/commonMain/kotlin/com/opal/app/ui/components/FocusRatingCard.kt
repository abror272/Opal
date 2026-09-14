package com.opal.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.theme.OpalColors

/** Sessiya bahosi darajalari. */
data class RatingLevel(val score: Int, val emoji: String, val label: String)

val RATING_LEVELS = listOf(
    RatingLevel(45, "😵", "Qiyin"),
    RatingLevel(58, "😕", "O'ta"),
    RatingLevel(72, "🙂", "Yaxshi"),
    RatingLevel(86, "😊", "Zo'r"),
    RatingLevel(96, "🤩", "Mukammal")
)

/**
 * Fokus baholash kartasi — sessiya tugagach "qanday o'tdi?" deb so'raydi.
 * Foydalanuvchi bahosi Focus Score'ga aylanadi.
 */
@Composable
fun FocusRatingCard(
    early: Boolean,
    picked: Int?,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0x1F86EFAC), Color(0x08FFFFFF))))
            .border(1.dp, Color(0x4086EFAC), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Text(
            if (early) "Erta tugaldi — lekin har bir daqiqa hisoblandi."
            else "To'liq yakunlandi! Focus Score'ingizni tanlang 👇",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = OpalColors.TextTertiary
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RATING_LEVELS.forEachIndexed { i, lv ->
                val active = picked == i
                val scale by animateFloatAsState(
                    if (active) 1.10f else 1f,
                    spring(stiffness = 420f, dampingRatio = 0.5f),
                    label = "rateScale"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .scale(scale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) Color(0x2E86EFAC) else Color(0x0BFFFFFF))
                        .border(
                            1.dp,
                            if (active) Color(0x8086EFAC) else Color(0x1AFFFFFF),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onPick(i) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(lv.emoji, fontSize = 20.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        lv.label.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color(0xFFC9FBDC) else OpalColors.TextTertiary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        if (picked != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Focus Score ${RATING_LEVELS[picked].score} saqlandi ${RATING_LEVELS[picked].emoji}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF9FE8B5),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
