package com.example.gamehub.models

data class CommunityServer(
    val id: String,
    val name: String,
    val description: String,
    val adminIds: List<String>,
    val userIds: List<String>,
    val admins: List<User>? = null,
    val users: List<User>? = null
)

data class CreateServerRequest(
    val name: String,
    val description: String,
    val adminIds: List<String>
)