package com.opal.app.data

actual fun loadRulesRaw(): String? = prefs().getString("rules", null)

actual fun saveRulesRaw(value: String) {
    prefs().edit().putString("rules", value).apply()
}
