package com.example.gamehub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.gamehub.models.LoginRequest
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import com.example.gamehub.network.WebSocketManager
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        TokenManager.init(this)

        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val textError = findViewById<TextView>(R.id.textError)

        buttonLogin.setOnClickListener {
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                textError.text = "Please enter email and password"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.api.login(LoginRequest(email, password))
                    TokenManager.saveToken(response.token)
                    TokenManager.saveUserId(response.userId)
                    Log.d("GameHub", "Login success, token saved")

//                    WebSocketManager.connect()

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    Log.e("GameHub", "Login failed: ${e.message}")
                    textError.text = "Login failed: Invalid email or password"
                }
            }
        }
        val textGoSignup = findViewById<TextView>(R.id.textGoSignup)
        textGoSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}