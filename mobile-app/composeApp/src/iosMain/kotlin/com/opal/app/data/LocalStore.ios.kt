package com.opal.app.data

import platform.Foundation.NSUserDefaults

actual fun loadLocalString(key: String): String? =
    NSUserDefaults.standardUserDefaults.stringForKey("opal_$key")

actual fun saveLocalString(key: String, value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, "opal_$key")
}
