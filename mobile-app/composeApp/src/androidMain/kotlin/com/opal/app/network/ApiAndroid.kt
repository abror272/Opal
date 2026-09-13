package com.opal.app.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Android emulyatorda 10.0.2.2 — host kompyuterning localhost'i.
 * Real qurilmada shu qiymatni LAN IP'ga o'zgartiring: masalan "http://192.168.1.5:3000"
 */
actual val defaultApiBase: String = "http://10.0.2.2:3000"

actual fun platformEngine(): HttpClientEngine = OkHttp.create()
