package com.example.gamehub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.gamehub.models.SignupRequest
import com.example.gamehub.network.RetrofitInstance
import kotlinx.coroutines.launch

class SignupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val editUsername = findViewById<EditText>(R.id.editUsername)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val buttonSignup = findViewById<Button>(R.id.buttonSignup)
        val textError = findViewById<TextView>(R.id.textError)
        val textGoLogin = findViewById<TextView>(R.id.textGoLogin)

        textGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        buttonSignup.setOnClickListener {
            val username = editUsername.text.toString()
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                textError.text = "Please fill in all fields"
                return@setOnClickListener
            }

            if (password.length < 6) {
                textError.text = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    RetrofitInstance.api.signup(SignupRequest(username, email, password))
                    Log.d("GameHub", "Signup success")


                    startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    Log.e("GameHub", "Signup failed: ${e.message}")
                    textError.text = "Signup failed. Email may already be in use."
                }
            }
        }
    }
}