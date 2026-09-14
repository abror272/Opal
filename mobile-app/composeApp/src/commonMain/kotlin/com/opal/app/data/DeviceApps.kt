package com.opal.app.data

import androidx.compose.ui.graphics.ImageBitmap

/** Telefonda o'rnatilgan, ishga tushiriladigan ilova. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap? = null
)

/**
 * Qurilmadagi ishga tushiriladigan ilovalar ro'yxati.
 * OG'IR amal — fonda (Dispatchers.Default) chaqiring.
 */
expect fun loadInstalledApps(): List<InstalledApp>

/* ---------- Bloklangan paketlar (doimiy saqlanadi) ---------- */

expect fun loadBlockedPackages(): Set<String>
expect fun saveBlockedPackages(packages: Set<String>)

/* ---------- Qat'iy bloklash (accessibility xizmati) ---------- */

/** Qat'iy bloklash faolmi? */
expect fun isStrictBlocking(): Boolean
expect fun setStrictBlocking(active: Boolean)

/** Bloklash xizmati tizim sozlamalarida yoqilganmi? */
expect fun isBlockingServiceEnabled(): Boolean

/** Bloklash xizmatini yoqish uchun tizim sozlamalarini ochish. */
expect fun openBlockingSettings()

/** Ishlab chiqaruvchi (MIUI/EMUI/ColorOS...) "avtostart" sozlamalarini ochadi. */
expect fun openAutostartSettings(): Boolean

/** Batareya optimizatsiyasidan ozod qilinganmi (fon xizmati o'ldirilmasligi uchun). */
expect fun isIgnoringBatteryOptimizations(): Boolean

/** Batareya optimizatsiyasidan ozod qilish so'rovini ko'rsatish. */
expect fun requestIgnoreBatteryOptimizations()

/** Bloklangan ilovaga vaqtincha ruxsat (millisekund). */
expect fun grantGrace(packageName: String, millis: Long)
expect fun inGrace(packageName: String): Boolean

/* ---------- Kunlik ochish hisobi ("limit" qoidalari uchun) ---------- */

/** Ilova ochilganini qayd etish (bir xil paket 20s ichida takror sanalmaydi). */
expect fun recordAppOpen(packageName: String)

/** Shu paket bugun necha marta ochilgan. */
expect fun appOpensToday(packageName: String): Int

/** Qurilmada mavjud bo'lgan mashhur chalg'ituvchi ilovalar (qoida nishonlari). */
expect fun installedSocialPackages(): Set<String>
