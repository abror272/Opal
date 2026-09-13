package com.opal.app.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/** iOS simulyator: Mac'dagi dev server localhost orqali ko'rinadi. */
actual val defaultApiBase: String = "http://localhost:3000"

actual fun platformEngine(): HttpClientEngine = Darwin.create()
