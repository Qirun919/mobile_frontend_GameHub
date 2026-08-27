package com.example.gamehub.models


data class Game(
    val id: String,
    val steamGameId: Int,
    val title: String,
    val description: String,
    val price: Double,
    val coverImage: GameImage?,
    val screenshots: List<GameImage>?,
    val trailerUrl: String?
)

data class GameImage(
    val id: Int,
    val name: String?,
    val url: String?,
    val type: String?
)