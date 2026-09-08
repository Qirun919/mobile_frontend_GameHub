package com.example.gamehub

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import coil.load
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

        lifecycleScope.launch {
            try {
                val friendUser = RetrofitInstance.api.getUserById(friendId)
                findViewById<TextView>(R.id.textChatFriendName).text = friendUser.username
                val avatarView = findViewById<ImageView>(R.id.imageChatAvatar)
                if (friendUser.avatarUrl.isNullOrEmpty()) {
                    avatarView.setImageResource(android.R.drawable.ic_menu_gallery)
                } else {
                    avatarView.load(friendUser.avatarUrl)
                }
            } catch (e: Exception) {
                Log.e("GameHub", "Load friend info failed: ${e.message}")
            }
        }

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
        val isMine = senderId == myUserId
        val layoutRes = if (isMine) R.layout.item_message_sent else R.layout.item_message_received

        val itemView = layoutInflater.inflate(layoutRes, containerMessages, false)
        itemView.findViewById<TextView>(R.id.textMessageContent).text = content

        containerMessages.addView(itemView)

        val scrollView = findViewById<ScrollView>(R.id.scrollMessages)
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
}