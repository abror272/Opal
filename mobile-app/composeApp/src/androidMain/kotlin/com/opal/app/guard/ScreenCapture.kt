package com.opal.app.guard

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import java.util.concurrent.Executor

/**
 * AccessibilityService orqali ekran rasmini oladi (API 30+).
 *
 * `android:canTakeScreenshot="true"` ruxsati kerak. Rasm faqat xotirada
 * tahlil qilinadi va darhol bo'shatiladi — saqlanmaydi, yuborilmaydi.
 */
internal object ScreenCapture {

    fun capture(
        service: AccessibilityService,
        executor: Executor,
        onResult: (Bitmap?) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            onResult(null)
            return
        }
        try {
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                executor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        var bitmap: Bitmap? = null
                        try {
                            val buffer = result.hardwareBuffer
                            val hardware = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                            if (hardware != null) {
                                bitmap = hardware.copy(Bitmap.Config.ARGB_8888, false)
                                hardware.recycle()
                            }
                            buffer.close()
                        } catch (_: Throwable) {
                            bitmap = null
                        }
                        onResult(bitmap)
                    }

                    override fun onFailure(errorCode: Int) {
                        onResult(null)
                    }
                }
            )
        } catch (_: Throwable) {
            onResult(null)
        }
    }
}
