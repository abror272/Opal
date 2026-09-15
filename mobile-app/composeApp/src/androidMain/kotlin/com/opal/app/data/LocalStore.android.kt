package com.opal.app.data

import android.content.Context

private const val DATA_PREFS = "opal_data"

private fun dataPrefs() =
    opalContext().getSharedPreferences(DATA_PREFS, Context.MODE_PRIVATE)

actual fun loadLocalString(key: String): String? = try {
    dataPrefs().getString(key, null)
} catch (_: Throwable) {
    null
}

actual fun saveLocalString(key: String, value: String) {
    try {
        dataPrefs().edit().putString(key, value).apply()
    } catch (_: Throwable) {
    }
}
