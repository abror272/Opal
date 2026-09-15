package com.opal.app.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.opal.app.data.forceRebindAccessibility

/**
 * Telefon qayta yuklanganda (yoki Opal yangilanganda) bloklashni tiklaydi.
 * MIUI ba'zan accessibility bog'lanishini tiklamaydi — watchdog uni qayta ulaydi.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val ok = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == "miui.intent.action.BOOT_COMPLETED"

        if (!ok) return

        WatchdogService.start(context)
        try {
            forceRebindAccessibility()
        } catch (_: Throwable) {
        }
    }
}
