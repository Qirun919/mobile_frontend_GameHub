package com.example.gamehub.models

data class User(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val online: Boolean
)