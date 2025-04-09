package com.jlsoft.firstviewwatch

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import androidx.preference.PreferenceManager

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

        fun appPrefs(): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(applicationContext())

        }

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }

        fun isRunningOnEmulator(): Boolean {
            return (Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("sdk_gwear")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86"))
        }

        fun forceRestart( context : Context) {
            // Get the launch intent for your package (your app’s entry point)
            val packageManager = context.packageManager
            val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                // Clear the current back stack so that you start fresh
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (launchIntent != null) {
                // Wrap the intent in a PendingIntent
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )

                // Schedule the PendingIntent to fire shortly (e.g., after 100ms)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.set(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + 500,
                    pendingIntent
                )
            }

            // If the context is an Activity, finish it and clear its task.
            if (context is Activity) {
                context.finishAffinity()
            }

            // Kill the current process. This forces a full restart when the alarm triggers.
            Runtime.getRuntime().exit(0)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize things that need to be set up globally
    }




}