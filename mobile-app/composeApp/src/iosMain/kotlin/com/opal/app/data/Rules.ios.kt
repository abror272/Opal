package com.opal.app.data

import platform.Foundation.NSUserDefaults

actual fun loadRulesRaw(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey("opal_rules")

actual fun saveRulesRaw(value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, "opal_rules")
}
