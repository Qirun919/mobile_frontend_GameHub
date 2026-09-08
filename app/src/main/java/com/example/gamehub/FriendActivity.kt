package com.example.gamehub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.gamehub.models.AddFriendRequest
import com.example.gamehub.models.Friendship
import com.example.gamehub.models.UpdateFriendRequest
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import com.example.gamehub.network.WebSocketManager
import kotlinx.coroutines.launch

class FriendsActivity : ComponentActivity() {

    private lateinit var containerFriends: LinearLayout
    private lateinit var textError: TextView

    private var currentTab = "friends"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        containerFriends = findViewById(R.id.containerFriends)
        textError = findViewById(R.id.textError)

        val editFriendId = findViewById<EditText>(R.id.editFriendId)
        val buttonAddFriend = findViewById<Button>(R.id.buttonAddFriend)

        val buttonTabFriends = findViewById<Button>(R.id.buttonTabFriends)
        val buttonTabRequests = findViewById<Button>(R.id.buttonTabRequests)

        buttonTabFriends.setOnClickListener {
            currentTab = "friends"
            loadFriends()
        }

        buttonTabRequests.setOnClickListener {
            currentTab = "requests"
            loadFriends()
        }

        buttonAddFriend.setOnClickListener {
            val friendUsername = editFriendId.text.toString().trim()
            if (friendUsername.isEmpty()) {
                textError.text = "Please enter a username"
                return@setOnClickListener
            }
            addFriend(friendUsername)
        }

        WebSocketManager.subscribeFriendRequests {
            runOnUiThread {
                Log.d("GameHub", "New friend request received!")
                loadFriends()
            }
        }

        loadFriends()
    }

    override fun onResume() {
        super.onResume()
        WebSocketManager.ensureConnected()
        subscribeToFriendRequests()
    }

    private fun subscribeToFriendRequests() {
        WebSocketManager.subscribeFriendRequests {
            runOnUiThread {
                Log.d("GameHub", "New friend request received!")
                loadFriends()
            }
        }
    }

    private fun addFriend(friendUsername: String) {
        val myUserId = TokenManager.getUserId()
        if (myUserId == null) {
            textError.text = "You are not logged in"
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("GameHub", "Step 1: Looking up username $friendUsername")
                val friendUser = RetrofitInstance.api.getUserByUsername(friendUsername)
                Log.d("GameHub", "Step 1 success: found user ${friendUser.id}")

                if (friendUser.id == myUserId) {
                    textError.text = "You cannot add yourself"
                    return@launch
                }

                Log.d("GameHub", "Step 2: Sending friend request")
                RetrofitInstance.api.addFriend(AddFriendRequest(myUserId, friendUser.id))
                Log.d("GameHub", "Step 2 success")

                textError.text = ""
                loadFriends()

            } catch (e: Exception) {
                Log.e("GameHub", "Add friend failed: ${e.message}")
                textError.text = "User not found or already friends."
            }
        }
    }

    private fun loadFriends() {
        val myUserId = TokenManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                val friendships = RetrofitInstance.api.getFriendships(myUserId)
                val allUsers = RetrofitInstance.api.getUsers()

                containerFriends.removeAllViews()

                if (currentTab == "requests") {
                    val pending = friendships.filter { it.status == "pending" && it.friendId == myUserId }

                    if (pending.isEmpty()) {
                        val emptyText = TextView(this@FriendsActivity)
                        emptyText.text = "No pending requests"
                        emptyText.setPadding(12, 24, 12, 12)
                        containerFriends.addView(emptyText)
                    }

                    for (friendship in pending) {
                        val otherUserId = if (friendship.userId == myUserId) friendship.friendId else friendship.userId
                        val otherUser = allUsers.find { it.id == otherUserId }

                        val itemView = layoutInflater.inflate(R.layout.item_friend_request, containerFriends, false)
                        itemView.findViewById<TextView>(R.id.textFriendName).text = otherUser?.username ?: otherUserId

                        val avatarView = itemView.findViewById<ImageView>(R.id.imageFriendAvatar)
                        if (otherUser?.avatarUrl.isNullOrEmpty()) {
                            avatarView.setImageResource(android.R.drawable.ic_menu_gallery)
                        } else {
                            avatarView.load(otherUser?.avatarUrl)
                        }

                        itemView.findViewById<Button>(R.id.buttonAccept).setOnClickListener {
                            acceptFriend(friendship)
                        }
                        itemView.findViewById<Button>(R.id.buttonReject).setOnClickListener {
                            rejectFriend(friendship)
                        }

                        containerFriends.addView(itemView)
                    }

                } else {
                    val accepted = friendships.filter { it.status == "accepted" }

                    val acceptedWithUser = accepted.mapNotNull { f ->
                        val otherUserId = if (f.userId == myUserId) f.friendId else f.userId
                        val otherUser = allUsers.find { it.id == otherUserId }
                        if (otherUser != null) Pair(f, otherUser) else null
                    }.sortedByDescending { it.second.online }

                    if (acceptedWithUser.isEmpty()) {
                        val emptyText = TextView(this@FriendsActivity)
                        emptyText.text = "No friends yet"
                        emptyText.setPadding(12, 24, 12, 12)
                        containerFriends.addView(emptyText)
                    }

                    for ((friendship, otherUser) in acceptedWithUser) {
                        val itemView = layoutInflater.inflate(R.layout.item_friend, containerFriends, false)
                        itemView.findViewById<TextView>(R.id.textFriendName).text = otherUser.username
                        itemView.findViewById<TextView>(R.id.textFriendStatus).text = if (otherUser.online) "Online" else "Offline"

                        val dot = itemView.findViewById<View>(R.id.viewOnlineDot)
                        dot.setBackgroundColor(if (otherUser.online) android.graphics.Color.GREEN else android.graphics.Color.GRAY)

                        val avatarView = itemView.findViewById<ImageView>(R.id.imageFriendAvatar)
                        if (otherUser.avatarUrl.isNullOrEmpty()) {
                            avatarView.setImageResource(android.R.drawable.ic_menu_gallery)
                        } else {
                            avatarView.load(otherUser.avatarUrl)
                        }

                        itemView.setOnClickListener {
                            val chatIntent = Intent(this@FriendsActivity, ChatActivity::class.java)
                            chatIntent.putExtra("friend_id", otherUser.id)
                            startActivity(chatIntent)
                        }

                        containerFriends.addView(itemView)
                    }
                }

            } catch (e: Exception) {
                Log.e("GameHub", "Load friends failed: ${e.message}")
            }
        }
    }

    private fun acceptFriend(friendship: Friendship) {
        lifecycleScope.launch {
            try {
                RetrofitInstance.api.updateFriendship(friendship.id, UpdateFriendRequest("accepted"))
                Log.d("GameHub", "Friend accepted")
                loadFriends()
            } catch (e: Exception) {
                Log.e("GameHub", "Accept friend failed: ${e.message}")
            }
        }
    }
    private fun rejectFriend(friendship: Friendship) {
        lifecycleScope.launch {
            try {
                RetrofitInstance.api.deleteFriendship(friendship.id)
                Log.d("GameHub", "Friend request rejected")
                loadFriends()
            } catch (e: Exception) {
                Log.e("GameHub", "Reject friend failed: ${e.message}")
            }
        }
    }
}