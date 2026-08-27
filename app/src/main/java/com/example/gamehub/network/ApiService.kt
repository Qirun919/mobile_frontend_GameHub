package com.example.gamehub.network

import com.example.gamehub.models.AddFriendRequest
import com.example.gamehub.models.Friendship
import com.example.gamehub.models.Game
import com.example.gamehub.models.LoginRequest
import com.example.gamehub.models.LoginResponse
import com.example.gamehub.models.Message
import com.example.gamehub.models.SignupRequest
import com.example.gamehub.models.UpdateFriendRequest
import com.example.gamehub.models.User
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("games")
    suspend fun getGames(): List<Game>

    @GET("games/popular")
    suspend fun getPopularGames(): List<Game>

    @GET("games/paged")
    suspend fun getGamesPaged(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): List<Game>

    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("users")
    suspend fun signup(@Body request: SignupRequest): User

    @GET("users")
    suspend fun getUsers(): List<User>

    // friendship

    @GET("users/username/{username}")
    suspend fun getUserByUsername(@Path("username") username: String): User

    @GET("friendships/user/{userId}")
    suspend fun getFriendships(@Path("userId") userId: String): List<Friendship>

    @POST("friendships")
    suspend fun addFriend(@Body request: AddFriendRequest): Friendship

    @PUT("friendships/{id}")
    suspend fun updateFriendship(@Path("id") id: String, @Body request: UpdateFriendRequest): Friendship

    @DELETE("friendships/{id}")
    suspend fun deleteFriendship(@Path("id") id: String)

    // Friend

    @GET("messages/private/{userId1}/{userId2}")
    suspend fun getPrivateMessages(
        @Path("userId1") userId1: String,
        @Path("userId2") userId2: String
    ): List<Message>
}