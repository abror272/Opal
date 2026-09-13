package com.opal.app.data

import com.opal.app.session.SessionController

/** Oddiy service locator — KMP uchun yetarli (DI kutubxonasisiz). */
object AppGraph {
    val repo: OpalRepository by lazy { OpalRepository() }
    val sessions: SessionController by lazy { SessionController(repo) }
}
