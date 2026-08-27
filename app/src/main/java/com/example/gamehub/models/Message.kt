package com.example.gamehub.models

data class Message(
    val id: String? = null,
    val senderId: String,
    val receiverId: String?,
    val serverId: String?,
    val content: String,
    val timestamp: String? = null
)