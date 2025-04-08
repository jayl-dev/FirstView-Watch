package com.jlsoft.firstviewwatch

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build

class MyApplication : Application() {


    init {
        instance = this
    }

    companion object {
        private var instance: MyApplication? = null

        private const val PREF_NAME = "MyPrefs"

        fun myPrefs(): SharedPreferences {
            return applicationContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        }

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }

        fun isRunningOnEmulator(): Boolean {
            return (Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86"))
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize things that need to be set up globally
    }
}