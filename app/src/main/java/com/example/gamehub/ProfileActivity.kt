package com.example.gamehub

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import kotlinx.coroutines.launch

class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val imageAvatar = findViewById<ImageView>(R.id.imageAvatar)
        val textUsername = findViewById<TextView>(R.id.textUsername)
        val textEmail = findViewById<TextView>(R.id.textEmail)
        val editAvatarUrl = findViewById<EditText>(R.id.editAvatarUrl)
        val buttonUpdateAvatar = findViewById<Button>(R.id.buttonUpdateAvatar)
        val textError = findViewById<TextView>(R.id.textError)

        val myUserId = TokenManager.getUserId()
        if (myUserId == null) {
            textError.text = "You are not logged in"
            return
        }

        fun loadProfile() {
            lifecycleScope.launch {
                try {
                    val user = RetrofitInstance.api.getUserByUsername(myUserId)
                    textUsername.text = user.username
                    textEmail.text = user.email

                    if (user.avatarUrl.isNullOrEmpty()) {
                        imageAvatar.setImageResource(android.R.drawable.ic_menu_gallery)
                    } else {
                        imageAvatar.load(user.avatarUrl)
                    }

                } catch (e: Exception) {
                    Log.e("GameHub", "Load profile failed: ${e.message}")
                }
            }
        }

        buttonUpdateAvatar.setOnClickListener {
            val newUrl = editAvatarUrl.text.toString().trim()
            if (newUrl.isEmpty()) {
                textError.text = "Please enter an image URL"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    RetrofitInstance.api.updateAvatar(myUserId, mapOf("avatarUrl" to newUrl))
                    textError.text = ""
                    Log.d("GameHub", "Avatar updated")
                    loadProfile()
                } catch (e: Exception) {
                    Log.e("GameHub", "Update avatar failed: ${e.message}")
                    textError.text = "Failed to update avatar"
                }
            }
        }

        loadProfile()
    }
}