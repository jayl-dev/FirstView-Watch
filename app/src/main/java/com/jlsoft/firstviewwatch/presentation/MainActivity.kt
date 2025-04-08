/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.jlsoft.firstviewwatch.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jlsoft.firstviewwatch.MyApplication
import com.jlsoft.firstviewwatch.login.LoginActivity
import com.jlsoft.firstviewwatch.map.WearMapScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)


    }

    override fun onStart() {
        super.onStart()
        checkLoginToken()
    }

    private fun checkLoginToken() {
        val token = MyApplication.myPrefs().getString("login_token", null)

        when {
            token.isNullOrEmpty() -> redirectToLogin()
            else -> setupMainUi()
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
}

