package com.opal.app

import android.app.Application

/** Ilova darajasidagi context — accessibility service ham ishlatadi. */
class OpalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: OpalApplication
            private set
    }
}
