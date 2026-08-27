package com.example.gamehub.models

data class Friendship(
    val id: String,
    val userId: String,
    val friendId: String,
    val status: String
)

data class AddFriendRequest(
    val userId: String,
    val friendId: String
)

data class UpdateFriendRequest(
    val status: String
)