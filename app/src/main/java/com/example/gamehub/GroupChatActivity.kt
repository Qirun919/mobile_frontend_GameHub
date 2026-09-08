package com.example.gamehub

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.gamehub.models.Message
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import com.example.gamehub.network.WebSocketManager
import kotlinx.coroutines.launch

class GroupChatActivity : ComponentActivity() {

    private lateinit var containerMessages: LinearLayout
    private lateinit var scrollView: ScrollView
    private var serverId: String = ""
    private var myUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        containerMessages = findViewById(R.id.containerMessages)
        scrollView = findViewById(R.id.scrollMessages)

        myUserId = TokenManager.getUserId() ?: ""
        serverId = intent.getStringExtra("server_id") ?: ""
        val serverName = intent.getStringExtra("server_name") ?: ""

        findViewById<TextView>(R.id.textChatFriendName).text = serverName
        findViewById<android.widget.ImageView>(R.id.imageChatAvatar)
            .setImageResource(android.R.drawable.ic_menu_myplaces)

        val editMessage = findViewById<EditText>(R.id.editMessage)
        val buttonSend = findViewById<Button>(R.id.buttonSend)

        buttonSend.setOnClickListener {
            val content = editMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                WebSocketManager.sendGroupMessage(myUserId, serverId, content)
                editMessage.setText("")
            }
        }

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        WebSocketManager.ensureConnected()
        subscribeToGroupMessages()
    }

    private fun subscribeToGroupMessages() {
        WebSocketManager.subscribeGroupMessages(serverId) { messageJson ->
            runOnUiThread {
                try {
                    val message = RetrofitInstance.moshi.adapter(Message::class.java).fromJson(messageJson)
                    if (message != null) {
                        addMessageToScreen(message.senderId, message.content)
                    }
                } catch (e: Exception) {
                    Log.e("GameHub", "Error parsing group message: ${e.message}")
                }
            }
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            try {
                val messages = RetrofitInstance.api.getServerMessages(serverId)
                containerMessages.removeAllViews()
                for (message in messages) {
                    addMessageToScreen(message.senderId, message.content)
                }
                Log.d("GameHub", "Loaded ${messages.size} group messages")
            } catch (e: Exception) {
                Log.e("GameHub", "Error loading group history: ${e.message}")
            }
        }
    }

    private fun addMessageToScreen(senderId: String, content: String) {
        val isMine = senderId == myUserId
        val layoutRes = if (isMine) R.layout.item_message_sent else R.layout.item_message_received

        val itemView = layoutInflater.inflate(layoutRes, containerMessages, false)
        itemView.findViewById<TextView>(R.id.textMessageContent).text = content
        containerMessages.addView(itemView)

        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
}