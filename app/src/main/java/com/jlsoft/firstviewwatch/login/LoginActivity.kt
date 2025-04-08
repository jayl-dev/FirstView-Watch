package com.jlsoft.firstviewwatch.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.jlsoft.firstviewwatch.R
import com.jlsoft.firstviewwatch.api.AuthApiSerivce
import com.jlsoft.firstviewwatch.api.AuthClient
import com.jlsoft.firstviewwatch.api.FirstViewClient
import com.jlsoft.firstviewwatch.api.LoginRequest
import com.jlsoft.firstviewwatch.api.TokenRequest
import com.jlsoft.firstviewwatch.presentation.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.jlsoft.firstviewwatch.MyApplication
import androidx.core.content.edit


class LoginActivity : ComponentActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        setupInputListeners()
        setupLoginButton()
    }

    private fun setupInputListeners() {

        // Enable login button only when both fields have text
        listOf(etUsername, etPassword).forEach { editText ->
            editText.doAfterTextChanged {
                btnLogin.isEnabled = etUsername.text.isNotBlank() &&
                        etPassword.text.isNotBlank()
            }
        }
    }

    private fun setupLoginButton() {
        btnLogin.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        when {
            !validateCredentials(username, password) ->
                showError("Invalid credentials")
            else -> {
                login(etUsername.text.toString(), etPassword.text.toString())
            }
        }
    }

    private fun validateCredentials(username: String, password: String) =
        username.isNotBlank() && password.isNotBlank()

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()

    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun generateDeviceUid(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun login(username: String, password: String) {

        // Run the API call in a coroutine
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestPayload = LoginRequest(
                    email_or_phone = username,
                    password = password,
                    remember_me = true,
                    device_name = "android",
                    device_uid = generateDeviceUid(applicationContext)
                )
                val response = AuthClient.instance.login(requestPayload)
                if(response.login_token!= null){
                    MyApplication.myPrefs().edit {
                        putString("email", username)
                        putString("login_token", response.login_token)
                        apply()
                    }

                }
                runOnUiThread {
                    navigateToMain()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    // Handle errors on the UI thread
                    Toast.makeText(
                        applicationContext,
                        "login failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}