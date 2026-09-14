package com.opal.app.blocking

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.opal.app.data.grantGrace
import com.opal.app.theme.OpalTheme
import com.opal.app.ui.screens.BlockedAppScreen

/** Bloklangan ilova ochilganda ko'rsatiladigan to'liq ekranli oyna. */
class BlockActivity : ComponentActivity() {

    private var pkg: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        render()
    }

    private fun render() {
        val label = labelOf(pkg)
        val icon = iconOf(pkg)
        setContent {
            OpalTheme {
                BlockedAppScreen(
                    appName = label,
                    appIcon = icon,
                    onDismiss = { goHome() },
                    onAllow = {
                        if (pkg.isNotEmpty()) {
                            grantGrace(pkg, GRACE_MILLIS)
                            launchApp(pkg)
                        } else {
                            goHome()
                        }
                    }
                )
            }
        }
    }

    private fun goHome() {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Throwable) {
        }
        finish()
    }

    private fun launchApp(packageName: String) {
        val i = packageManager.getLaunchIntentForPackage(packageName)
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            try {
                startActivity(i)
            } catch (_: Throwable) {
            }
        }
        finish()
    }

    private fun labelOf(packageName: String): String {
        if (packageName.isEmpty()) return "Ilova"
        return try {
            val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(ai).toString()
        } catch (_: Throwable) {
            packageName.substringAfterLast('.')
        }
    }

    private fun iconOf(packageName: String): ImageBitmap? {
        if (packageName.isEmpty()) return null
        return try {
            val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationIcon(ai).toBitmap(160)
        } catch (_: Throwable) {
            null
        }
    }

    private fun Drawable.toBitmap(target: Int): ImageBitmap? = try {
        if (this is BitmapDrawable && bitmap != null) {
            Bitmap.createScaledBitmap(bitmap, target, target, true).asImageBitmap()
        } else {
            val w = intrinsicWidth.takeIf { it > 0 } ?: target
            val h = intrinsicHeight.takeIf { it > 0 } ?: target
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            setBounds(0, 0, w, h)
            draw(canvas)
            Bitmap.createScaledBitmap(bmp, target, target, true).asImageBitmap()
        }
    } catch (_: Throwable) {
        null
    }

    companion object {
        const val EXTRA_PACKAGE = "blocked_package"
        const val GRACE_MILLIS = 5 * 60 * 1000L
    }
}
