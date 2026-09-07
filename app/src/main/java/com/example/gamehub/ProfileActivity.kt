package com.example.gamehub

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : ComponentActivity() {

    private lateinit var imageAvatar: ImageView
    private lateinit var textError: TextView
    private var myUserId: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        imageAvatar = findViewById(R.id.imageAvatar)
        val textUsername = findViewById<TextView>(R.id.textUsername)
        val textEmail = findViewById<TextView>(R.id.textEmail)
        textError = findViewById(R.id.textError)


        imageAvatar.clipToOutline = true
        imageAvatar.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }

        myUserId = TokenManager.getUserId()
        if (myUserId == null) {
            textError.text = "You are not logged in"
            return
        }

        imageAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        loadProfile()
    }

    private fun loadProfile() {
        val userId = myUserId ?: return
        lifecycleScope.launch {
            try {
                val user = RetrofitInstance.api.getUserById(userId)
                Log.d("GameHub", "avatarUrl from server: ${user.avatarUrl}")
                findViewById<TextView>(R.id.textUsername).text = user.username
                findViewById<TextView>(R.id.textEmail).text = user.email

                if (user.avatarUrl.isNullOrEmpty()) {
                    imageAvatar.setImageResource(android.R.drawable.ic_menu_gallery)
                } else {
                    imageAvatar.load(user.avatarUrl)
                }

                loadStats(userId)

            } catch (e: Exception) {
                Log.e("GameHub", "Load profile failed: ${e.message}")
            }
        }
    }

    private fun loadStats(userId: String) {
        lifecycleScope.launch {
            try {
                val ownedGames = RetrofitInstance.api.getOwnedGames(userId)
                findViewById<TextView>(R.id.textGameCount).text = ownedGames.size.toString()
            } catch (e: Exception) {
                findViewById<TextView>(R.id.textGameCount).text = "0"
            }

            try {
                val friendships = RetrofitInstance.api.getFriendships(userId)
                val acceptedCount = friendships.count { it.status == "accepted" }
                findViewById<TextView>(R.id.textFriendCount).text = acceptedCount.toString()
            } catch (e: Exception) {
                findViewById<TextView>(R.id.textFriendCount).text = "0"
            }
        }
    }

    private fun uploadImage(uri: Uri) {
        val userId = myUserId ?: return

        lifecycleScope.launch {
            try {
                textError.text = "Uploading..."

                val imageUrl = withContext(Dispatchers.IO) {
                    uploadToImgbb(uri)
                }

                if (imageUrl != null) {
                    RetrofitInstance.api.updateAvatar(userId, mapOf("avatarUrl" to imageUrl))
                    textError.text = ""
                    Log.d("GameHub", "Avatar updated: $imageUrl")
                    loadProfile()
                } else {
                    textError.text = "Upload failed"
                }

            } catch (e: Exception) {
                Log.e("GameHub", "Upload failed: ${e.message}")
                textError.text = "Upload failed: ${e.message}"
            }
        }
    }

    private fun uploadToImgbb(uri: Uri): String? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val tempFile = File(cacheDir, "upload_temp.jpg")
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }

        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", tempFile.name, tempFile.asRequestBody())
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload?key=${BuildConfig.IMGBB_API_KEY}")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            return if (json.getBoolean("success")) {
                json.getJSONObject("data").getString("url")
            } else {
                null
            }
        }
    }
}