package com.opal.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.opal.app.blocking.WatchdogService
import com.opal.app.data.AppGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Bildirishnoma ruxsati (Android 13+) — validatsiya bildirishnomalari uchun
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                    )
                } catch (_: Throwable) {
                }
            }
        }

        // Ikkinchi bloklash dvigatelini ishga tushirish (MIUI accessibility'ni o'ldirsa ham bloklaydi)
        WatchdogService.start(this)
        try {
            AppGraph.repo.ensureBlockingEngine()
        } catch (_: Throwable) {
        }

        setContent {
            OpalApp()
        }
    }

    override fun onResume() {
        super.onResume()
        // Sozlamalardan qaytganda ruxsat holatini yangilaymiz
        try {
            AppGraph.repo.refreshBlockingService()
            AppGraph.repo.ensureBlockingEngine()
        } catch (_: Throwable) {
        }
    }
}
