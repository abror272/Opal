package com.opal.app.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.canDrawOverlays
import com.opal.app.data.canWriteSecureSettings
import com.opal.app.data.hasUsageAccess
import com.opal.app.data.isIgnoringBatteryOptimizations
import com.opal.app.data.openAutostartSettings
import com.opal.app.data.openBlockingSettings
import com.opal.app.data.openOverlaySettings
import com.opal.app.data.openUsageAccessSettings
import com.opal.app.data.requestIgnoreBatteryOptimizations
import com.opal.app.glass.GlassCard
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons

/**
 * MIUI/HyperOS bloklash xizmatini tez-tez o'chirib qo'yadi. Bu ogohlantirish
 * foydalanuvchiga "neye blok ishlamayapti" ekanini ANIQ ko'rsatadi va bir
 * bosishda tuzatish imkonini beradi.
 */
@Composable
fun BlockingGuardBanner(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onEnable: () -> Unit = { openBlockingSettings() },
    onAdvanced: (() -> Unit)? = null
) {
    if (!visible) return
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OpalRadius.lg))
            .background(
                Brush.verticalGradient(
                    listOf(OpalColors.Danger.copy(alpha = 0.16f), OpalColors.Amber.copy(alpha = 0.07f))
                )
            )
            .border(1.dp, OpalColors.Danger.copy(alpha = 0.42f), RoundedCornerShape(OpalRadius.lg))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OpalColors.Danger.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    OpalIcons(OpalIcon.Shield, OpalColors.Danger, Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Bloklash o'chirilgan",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpalColors.TextPrimary
                    )
                    Text(
                        "Bloklangan ilovalar va taymer hozir ishlamaydi",
                        fontSize = 11.5.sp,
                        color = OpalColors.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(OpalColors.Accent)
                        .clickable { onEnable() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Bloklashni yoqish",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF08120B)
                    )
                }
                if (onAdvanced != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.09f))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                            .clickable { onAdvanced() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nega?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                    }
                }
            }
        }
    }
}

/** "Nega o'chib qoladi?" — tizim sozlamalariga olib boruvchi qadamlar. */
@Composable
fun BlockingSetupCard(modifier: Modifier = Modifier) {
    val batteryOk = isIgnoringBatteryOptimizations()
    val usageOk = hasUsageAccess()
    val overlayOk = canDrawOverlays()
    val secureOk = canWriteSecureSettings()

    GlassCard(modifier.fillMaxWidth(), radius = OpalRadius.lg, padding = 16.dp) {
        Column {
            Text(
                "Blok barqaror ishlashi uchun",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OpalColors.TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Opal ikki xil usulda bloklaydi. MIUI xizmatni o'chirib qo'ysa ham, ikkinchisi ishlaydi. Quyidagilarni yoqing:",
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = OpalColors.TextTertiary
            )
            Spacer(Modifier.height(12.dp))

            SetupRow(
                step = "1",
                title = "Bloklash xizmati",
                subtitle = "Sozlamalar → Qulaylik → Opal bloklash",
                done = false,
                action = "Ochish"
            ) { openBlockingSettings() }
            Spacer(Modifier.height(8.dp))
            SetupRow(
                step = "2",
                title = "Foydalanish tarixi",
                subtitle = if (usageOk) "Ruxsat berilgan — ekran vaqti hisoblanadi" else "Ekran vaqtini o'lchash uchun majburiy",
                done = usageOk,
                action = if (usageOk) "✓" else "Ochish"
            ) { openUsageAccessSettings() }
            Spacer(Modifier.height(8.dp))
            SetupRow(
                step = "3",
                title = "Boshqa ilovalar ustida",
                subtitle = if (overlayOk) "Ruxsat berilgan — ikkinchi dvigatel faol" else "Xizmat o'chsa ham blok ekrani chiqadi",
                done = overlayOk,
                action = if (overlayOk) "✓" else "Ochish"
            ) { openOverlaySettings() }
            Spacer(Modifier.height(8.dp))
            SetupRow(
                step = "4",
                title = "Avtomatik ishga tushish",
                subtitle = "\"Autostart\"ni yoqing (MIUI Security)",
                done = false,
                action = "Ochish"
            ) { openAutostartSettings() }
            Spacer(Modifier.height(8.dp))
            SetupRow(
                step = "5",
                title = "Batareya cheklovisiz",
                subtitle = if (batteryOk) "Allaqachon ruxsat berilgan" else "Fon rejimida o'chirilmasligi uchun",
                done = batteryOk,
                action = if (batteryOk) "✓" else "Ochish"
            ) { requestIgnoreBatteryOptimizations() }

            if (secureOk) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "✓ O'z-o'zini tiklash yoqilgan (xizmat o'chsa Opal o'zi qayta ulaydi)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OpalColors.Success
                )
            }
        }
    }
}

@Composable
private fun SetupRow(
    step: String,
    title: String,
    subtitle: String,
    done: Boolean,
    action: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OpalRadius.md))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(50))
                .background(if (done) OpalColors.Success.copy(alpha = 0.22f) else OpalColors.Accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                OpalIcons(OpalIcon.Check, OpalColors.Success, Modifier.size(13.dp))
            } else {
                Text(step, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OpalColors.MintLight)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
            Text(subtitle, fontSize = 10.5.sp, color = OpalColors.TextTertiary, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            action,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (done) OpalColors.Success else OpalColors.Accent
        )
    }
}
