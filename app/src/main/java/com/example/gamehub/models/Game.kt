package com.example.gamehub.models


data class Game(
    val id: String,
    val steamGameId: Int,
    val title: String,
    val description: String,
    val detailedDescription: String?,
    val price: Double,
    val coverImage: GameImage?,
    val screenshots: List<GameImage>?,
    val trailerUrl: String?,
    val genres: List<Genre>?,
    val developers: List<String>?,
    val publishers: List<String>?,
    val releaseDate: String?,
    val categories: List<String>?,
    val platforms: Platforms?
)

data class GameImage(
    val id: Int,
    val name: String?,
    val url: String?,
    val type: String?
)

data class Genre(
    val id: String?,
    val description: String?
)

data class Platforms(
    val windows: Boolean,
    val mac: Boolean,
    val linux: Boolean
)