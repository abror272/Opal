package com.opal.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.formatMinutes
import com.opal.app.glass.GlassCard
import com.opal.app.theme.OpalColors
import com.opal.app.ui.components.GradientCircle
import kotlinx.coroutines.launch

@Composable
fun AppsScreen() {
    val repo = remember { AppGraph.repo }
    val scope = rememberCoroutineScope()
    val apps by repo.apps.collectAsState()
    val blockedCount = apps.count { it.blocked }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(10.dp))

        Text("Ilovalar", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
        Text(
            "$blockedCount ta bloklangan • jami ${apps.size}",
            fontSize = 13.sp,
            color = OpalColors.TextSecondary,
            modifier = Modifier.padding(top = 3.dp)
        )

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(apps, key = { _, app -> app.id }) { index, app ->
                GlassCard(Modifier.fillMaxWidth(), radius = 22.dp, padding = 13.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradientCircle(index = index, modifier = Modifier.size(46.dp)) {
                            Text(app.emoji, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    app.name,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OpalColors.TextPrimary
                                )
                                if (app.blocked) {
                                    Spacer(Modifier.width(7.dp))
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(OpalColors.Danger.copy(alpha = 0.16f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text("BLOKDA", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = OpalColors.Danger)
                                    }
                                }
                            }
                            Text(
                                "${app.category} • bugun ${formatMinutes(app.todayMinutes)} / ${formatMinutes(app.dailyLimitMinutes)}",
                                fontSize = 11.5.sp,
                                color = OpalColors.TextTertiary,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                            // limit progress
                            val frac = (app.todayMinutes.toFloat() / app.dailyLimitMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
                            Spacer(Modifier.height(7.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(frac)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (frac >= 1f) OpalColors.Danger
                                            else if (frac > 0.7f) OpalColors.Amber
                                            else OpalColors.Success
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Switch(
                            checked = app.blocked,
                            onCheckedChange = { checked ->
                                scope.launch { repo.setBlocked(app, checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = OpalColors.AccentDeep,
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bloklash darhol kuchga kiradi 🔒",
                    fontSize = 11.5.sp,
                    color = OpalColors.TextTertiary,
                    modifier = Modifier.padding(bottom = 28.dp)
                )
            }
        }
    }
}
