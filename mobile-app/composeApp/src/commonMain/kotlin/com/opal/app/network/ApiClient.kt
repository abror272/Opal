package com.opal.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest.DefaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * API manzili platformaga qarab:
 *  - Android emulyator:  http://10.0.2.2:3000  (host kompyuterdagi dev server)
 *  - iOS simulyator:     http://localhost:3000
 *  - Real qurilma:       kompyuteringizning LAN IP manzilini yozing (masalan http://192.168.1.5:3000)
 */
expect val defaultApiBase: String

expect fun platformEngine(): HttpClientEngine

object ApiConfig {
    /** Real qurilmada shu qiymatni LAN IP'ga o'zgartiring yoki buildConstants orqali bering. */
    var baseUrl: String = defaultApiBase
}

private val opalJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = false
}

fun createHttpClient(): HttpClient = HttpClient(platformEngine()) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(opalJson)
    }
    install(DefaultRequest) {
        url(ApiConfig.baseUrl)
    }
    install(UserAgent) {
        agent = "OpalMobile/1.0 (Kotlin Multiplatform)"
    }
}
