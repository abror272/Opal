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

/** Bloklangan ilovaga vaqtincha ruxsat (millisekund). */
expect fun grantGrace(packageName: String, millis: Long)
expect fun inGrace(packageName: String): Boolean
