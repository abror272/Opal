package com.opal.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.formatMinutes
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import kotlinx.coroutines.delay
import kotlin.random.Random

private data class LeaderEntry(
    val id: String,
    val name: String,
    val avatar: String,
    val savedMinutes: Int,
    val streak: Int,
    val isMe: Boolean = false,
    val focusing: Boolean = false
)

private val DEMO_FRIENDS = listOf(
    Triple("Aziza", "👩‍🚀", 42),
    Triple("Bekzod", "🧑‍🎤", 28),
    Triple("Dilnoza", "👩‍🏫", 15),
    Triple("Javohir", "🧑‍⚕️", 9),
    Triple("Malika", "🧑‍🎨", 21)
)

private fun seedEntries(myName: String, mySaved: Int, myStreak: Int): List<LeaderEntry> {
    val others = DEMO_FRIENDS.mapIndexed { i, (n, a, streak) ->
        LeaderEntry(
            id = "f$i",
            name = n,
            avatar = a,
            savedMinutes = 40 + (i * 37) % 220,
            streak = streak,
            focusing = i % 3 == 0
        )
    }
    return others + LeaderEntry(
        id = "me",
        name = myName.ifBlank { "Siz" },
        avatar = "🧑",
        savedMinutes = mySaved,
        streak = myStreak,
        isMe = true,
        focusing = false
    )
}

/** Do'stlar reytingi — jonli yangilanadigan (demo simulyatsiya). */
@Composable
fun LiveLeaderboard(
    myName: String,
    mySavedMinutes: Int,
    myStreak: Int,
    modifier: Modifier = Modifier
) {
    var entries by remember { mutableStateOf(seedEntries(myName, mySavedMinutes, myStreak)) }
    var online by remember { mutableStateOf(4) }

    LaunchedEffect(Unit) {
        val rnd = Random(System.currentTimeMillis())
        while (true) {
            delay(4000)
            entries = entries.map { e ->
                if (e.isMe || e.focusing.not()) e
                else if (rnd.nextFloat() < 0.55f) e.copy(savedMinutes = e.savedMinutes + rnd.nextInt(1, 5))
                else e
            }
            online = 3 + rnd.nextInt(4)
        }
    }

    val ranked = entries.sortedByDescending { it.savedMinutes }
    val maxSaved = (ranked.maxOfOrNull { it.savedMinutes } ?: 1).coerceAtLeast(1)
    val myRank = ranked.indexOfFirst { it.isMe } + 1
    val myEntry = ranked.firstOrNull { it.isMe }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OpalRadius.lg))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(OpalRadius.lg))
            .padding(18.dp)
    ) {
        // header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OpalIcons(OpalIcon.Trophy, OpalColors.Amber, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Do'stlar reytingi", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(OpalColors.Amber.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("DEMO", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = OpalColors.Amber, letterSpacing = 0.4.sp)
                }
            }
            LiveBadge()
        }

        Spacer(Modifier.height(14.dp))

        // mening o'rnim
        if (myEntry != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0x143D5AFE), Color(0x14E861FF))))
                    .border(1.dp, Color(0x3D8EA2FF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sizning o'rningiz", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$myRank-o'rin / ${ranked.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF8EA2FF)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("$online online", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        ranked.forEachIndexed { idx, e ->
            LeaderRow(entry = e, rank = idx + 1, maxSaved = maxSaved)
            if (idx != ranked.lastIndex) Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Do'stlaringiz har soniyada jonli yangilanadi",
            fontSize = 10.sp,
            color = OpalColors.TextTertiary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LiveBadge() {
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x1A34D399))
            .border(1.dp, Color(0x3334D399), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).alpha(pulse).clip(CircleShape).background(Color(0xFF34D399)))
        Spacer(Modifier.width(5.dp))
        Text("JONLI", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6EE7B7), letterSpacing = 1.sp)
    }
}

@Composable
private fun LeaderRow(entry: LeaderEntry, rank: Int, maxSaved: Int) {
    val pct by animateFloatAsState(
        (entry.savedMinutes.toFloat() / maxSaved).coerceIn(0.04f, 1f),
        tween(700),
        label = "bar"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (entry.isMe) Brush.horizontalGradient(listOf(Color(0x143D5AFE), Color(0x147B61FF)))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // rank
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    when (rank) {
                        1 -> Brush.linearGradient(listOf(Color(0xFFFFD86B), Color(0xFFE0A800)))
                        2 -> Brush.linearGradient(listOf(Color(0xFFCFD8DC), Color(0xFF90A4AE)))
                        3 -> Brush.linearGradient(listOf(Color(0xFFFFB176), Color(0xFFD2691E)))
                        else -> Brush.linearGradient(listOf(Color(0x14FFFFFF), Color(0x0FFFFFFF)))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$rank",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (rank <= 3) Color.White else OpalColors.TextSecondary
            )
        }
        Spacer(Modifier.width(10.dp))

        // avatar
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.avatar, fontSize = 18.sp)
            if (entry.focusing) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34D399))
                        .border(2.dp, Color(0xFF0A0F12), CircleShape)
                )
            }
        }
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (entry.isMe) "${entry.name} (siz)" else entry.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isMe) Color(0xFF8EA2FF) else OpalColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.streak >= 10) {
                    Spacer(Modifier.width(4.dp))
                    Text("🔥", fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(5.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(pct)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (entry.isMe) Brush.horizontalGradient(listOf(Color(0xFF3D5AFE), Color(0xFF7B61FF)))
                            else Brush.horizontalGradient(listOf(Color(0xFFB7A6FF), Color(0xFFE8B7FF)))
                        )
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            formatMinutes(entry.savedMinutes),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OpalColors.TextPrimary
        )
    }
}
