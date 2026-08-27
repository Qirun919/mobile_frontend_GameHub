package com.example.gamehub

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.gamehub.models.Message
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import com.example.gamehub.network.WebSocketManager
import kotlinx.coroutines.launch

class ChatActivity : ComponentActivity() {

    private lateinit var containerMessages: LinearLayout
    private var friendId: String = ""
    private var myUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        containerMessages = findViewById(R.id.containerMessages)

        myUserId = TokenManager.getUserId() ?: ""
        friendId = intent.getStringExtra("friend_id") ?: ""

        val editMessage = findViewById<EditText>(R.id.editMessage)
        val buttonSend = findViewById<Button>(R.id.buttonSend)

        buttonSend.setOnClickListener {
            val content = editMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                WebSocketManager.sendPrivateMessage(myUserId, friendId, content)
                addMessageToScreen(myUserId, content)
                editMessage.setText("")
            }
        }

        WebSocketManager.subscribePrivateMessages { messageJson ->
            runOnUiThread {
                try {
                    val message = RetrofitInstance.moshi.adapter(Message::class.java).fromJson(messageJson)
                    if (message != null && message.senderId == friendId) {
                        addMessageToScreen(message.senderId, message.content)
                    }
                } catch (e: Exception) {
                    Log.e("GameHub", "Error parsing incoming message: ${e.message}")
                }
            }
        }

        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            try {
                val messages = RetrofitInstance.api.getPrivateMessages(myUserId, friendId)
                containerMessages.removeAllViews()
                for (message in messages) {
                    addMessageToScreen(message.senderId, message.content)
                }
                Log.d("GameHub", "Loaded ${messages.size} messages")
            } catch (e: Exception) {
                Log.e("GameHub", "Error loading history: ${e.message}")
            }
        }
    }

    private fun addMessageToScreen(senderId: String, content: String) {
        val textView = TextView(this)
        textView.text = if (senderId == myUserId) "Me: $content" else "Them: $content"
        textView.setPadding(8, 8, 8, 8)
        containerMessages.addView(textView)
    }

//    override fun onResume() {
//        super.onResume()
//        WebSocketManager.ensureConnected()

//        WebSocketManager.subscribePrivateMessages { messageJson ->
//            runOnUiThread {
//                try {
//                    val message = RetrofitInstance.moshi.adapter(Message::class.java).fromJson(messageJson)
//                    if (message != null && message.senderId == friendId) {
//                        addMessageToScreen(message.senderId, message.content)
//                    }
//                } catch (e: Exception) {
//                    Log.e("GameHub", "Error parsing incoming message: ${e.message}")
//                }
//            }
//        }
//    }
}