package com.example.gamehub

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.gamehub.models.CommunityServer
import com.example.gamehub.models.CreateServerRequest
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import kotlinx.coroutines.launch

class CommunityActivity : ComponentActivity() {

    private lateinit var containerServers: LinearLayout
    private lateinit var textError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community)

        containerServers = findViewById(R.id.containerServers)
        textError = findViewById(R.id.textError)

        val editServerName = findViewById<EditText>(R.id.editServerName)
        val buttonCreateServer = findViewById<Button>(R.id.buttonCreateServer)

        buttonCreateServer.setOnClickListener {
            val name = editServerName.text.toString().trim()
            if (name.isEmpty()) {
                textError.text = "Please enter a server name"
                return@setOnClickListener
            }
            createServer(name)
        }

        loadServers()
    }

    private fun createServer(name: String) {
        val myUserId = TokenManager.getUserId()
        if (myUserId == null) {
            textError.text = "You are not logged in"
            return
        }

        lifecycleScope.launch {
            try {
                val request = CreateServerRequest(name, "A community server", listOf(myUserId))
                RetrofitInstance.api.createServer(request)
                textError.text = ""
                Log.d("GameHub", "Server created: $name")
                loadServers()
            } catch (e: Exception) {
                Log.e("GameHub", "Create server failed: ${e.message}")
                textError.text = "Failed to create server"
            }
        }
    }

    private fun joinServer(server: CommunityServer) {
        val myUserId = TokenManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                RetrofitInstance.api.joinServer(server.id, myUserId)
                Log.d("GameHub", "Joined server: ${server.name}")
                loadServers()
            } catch (e: Exception) {
                Log.e("GameHub", "Join server failed: ${e.message}")
            }
        }
    }

    private fun loadServers() {
        val myUserId = TokenManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                val servers = RetrofitInstance.api.getServers()
                containerServers.removeAllViews()

                for (server in servers) {
                    val itemView = layoutInflater.inflate(android.R.layout.simple_list_item_2, containerServers, false)
                    val text1 = itemView.findViewById<TextView>(android.R.id.text1)
                    val text2 = itemView.findViewById<TextView>(android.R.id.text2)

                    text1.text = server.name
                    val isMember = server.userIds.contains(myUserId)
                    text2.text = if (isMember) "Joined (tap to open)" else "Tap to join"

                    itemView.setOnClickListener {
                        if (isMember) {
                            // 之后加跳转到服务器聊天画面
                            Log.d("GameHub", "Open server: ${server.name}")
                        } else {
                            joinServer(server)
                        }
                    }

                    containerServers.addView(itemView)
                }

                Log.d("GameHub", "Loaded ${servers.size} servers")

            } catch (e: Exception) {
                Log.e("GameHub", "Load servers failed: ${e.message}")
            }
        }
    }
}