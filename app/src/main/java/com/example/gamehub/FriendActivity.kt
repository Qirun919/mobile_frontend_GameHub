package com.example.gamehub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        containerFriends = findViewById(R.id.containerFriends)
        textError = findViewById(R.id.textError)

        val editFriendId = findViewById<EditText>(R.id.editFriendId)
        val buttonAddFriend = findViewById<Button>(R.id.buttonAddFriend)

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
        Log.d("GameHub", "myUserId is: $myUserId")

        lifecycleScope.launch {
            try {
                val friendships = RetrofitInstance.api.getFriendships(myUserId)
                val allUsers = RetrofitInstance.api.getUsers()

                containerFriends.removeAllViews()

                for (friendship in friendships) {
                    val otherUserId = if (friendship.userId == myUserId) friendship.friendId else friendship.userId
                    val otherUser = allUsers.find { it.id == otherUserId }
                    val displayName = otherUser?.username ?: otherUserId

                    val itemView = layoutInflater.inflate(android.R.layout.simple_list_item_2, containerFriends, false)
                    val text1 = itemView.findViewById<TextView>(android.R.id.text1)
                    val text2 = itemView.findViewById<TextView>(android.R.id.text2)
                    val buttonAccept = itemView.findViewById<ImageButton>(R.id.buttonAccept)
                    val buttonReject = itemView.findViewById<ImageButton>(R.id.buttonReject)

                    text1.text = displayName
                    text2.text = "Status: ${friendship.status}"

                    if (friendship.status == "pending" && friendship.friendId == myUserId) {
                        text2.text = "Status: pending (tap to accept)"
                        buttonAccept.visibility = android.view.View.VISIBLE
                        buttonReject.visibility = android.view.View.VISIBLE
                        buttonAccept.setOnClickListener {
                            acceptFriend(friendship)
                        }
                        buttonReject.setOnClickListener {
                            rejectFriend(friendship)
                        }
                    } else if (friendship.status == "accepted") {
                        itemView.setOnClickListener {
                            val chatIntent = Intent(this@FriendsActivity, ChatActivity::class.java)
                            chatIntent.putExtra("friend_id", otherUserId)
                            startActivity(chatIntent)
                        }
                    }

                    containerFriends.addView(itemView)
                }

                Log.d("GameHub", "Loaded ${friendships.size} friendships")

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