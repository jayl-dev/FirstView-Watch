/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.jlsoft.firstviewwatch.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.ambient.AmbientLifecycleObserver
import com.jlsoft.firstviewwatch.MyApplication
import com.jlsoft.firstviewwatch.login.LoginActivity
import com.jlsoft.firstviewwatch.map.WearMapScreen

class MainActivity : ComponentActivity() ,  AmbientLifecycleObserver.AmbientLifecycleCallback {

    private lateinit var ambientObserver: AmbientLifecycleObserver


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        // Create and register your ambient observer.
        ambientObserver = AmbientLifecycleObserver(this, this)
//        lifecycle.addObserver(ambientObserver)
        setupMainUi()

    }

    override fun onStart() {
        super.onStart()
        checkLoginToken()
        keepScreenOn()
    }

    fun keepScreenOn(){

        if(MyApplication.appPrefs().getBoolean("keep_screen_on", true)){
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }else{
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun checkLoginToken() {
        val token = MyApplication.myPrefs().getString("login_token", null)

        when {
            token.isNullOrEmpty() -> redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupMainUi() {
        setContent {
            WearMapScreen()
        }
    }


    override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
        super.onEnterAmbient(ambientDetails)
        Log.d("Ambient", "Enter")
    }
    override fun onExitAmbient() {
        Log.d("Ambient", "Exit")
    }
    override fun onUpdateAmbient() {
        // Optionally update UI elements periodically while in ambient mode
    }


}

