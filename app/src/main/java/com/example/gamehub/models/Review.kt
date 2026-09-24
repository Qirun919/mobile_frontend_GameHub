package com.example.gamehub.models

data class Review(
    val id: String?,
    val userId: String,
    val username : String?,
    val gameId: String,
    val content: String,
    val rating: Int
)