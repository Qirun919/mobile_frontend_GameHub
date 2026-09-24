package com.example.gamehub.network

import com.example.gamehub.models.AddFriendRequest
import com.example.gamehub.models.CommunityServer
import com.example.gamehub.models.CreateOrderRequest
import com.example.gamehub.models.CreateServerRequest
import com.example.gamehub.models.Friendship
import com.example.gamehub.models.Game
import com.example.gamehub.models.LoginRequest
import com.example.gamehub.models.LoginResponse
import com.example.gamehub.models.Message
import com.example.gamehub.models.Order
import com.example.gamehub.models.Review
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

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): User

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


    // Friend chat

    @GET("messages/private/{userId1}/{userId2}")
    suspend fun getPrivateMessages(
        @Path("userId1") userId1: String,
        @Path("userId2") userId2: String
    ): List<Message>


    // community server

    @GET("servers")
    suspend fun getServers(): List<CommunityServer>

    @GET("servers/{id}")
    suspend fun getServerById(@Path("id") id: String): CommunityServer

    @POST("servers")
    suspend fun createServer(@Body request: CreateServerRequest): CommunityServer

    @POST("servers/{serverId}/join/{userId}")
    suspend fun joinServer(@Path("serverId") serverId: String, @Path("userId") userId: String): CommunityServer


    // Community Chat

    @GET("messages/server/{serverId}")
    suspend fun getServerMessages(@Path("serverId") serverId: String): List<Message>


    // profile
    @PUT("users/{id}/avatar")
    suspend fun updateAvatar(@Path("id") id: String, @Body body: Map<String, String>): User

    @GET("orders/user/{userId}/games")
    suspend fun getOwnedGames(@Path("userId") userId: String): List<Game>


    // order
    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Order

    @POST("orders/{id}/payment")
    suspend fun createPayment(@Path("id") id: String): Order

    @POST("games/batch")
    suspend fun getGamesByIds(@Body gameIds: List<String>): List<Game>

    @GET("orders/{id}/confirm")
    suspend fun confirmOrder(@Path("id") id: String): Order

    @GET("games/filter")
    suspend fun getGamesByGenre(
        @Query("genre") genre: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): List<Game>?

    @GET("games/search")
    suspend fun searchGames(@Query("keyword") keyword: String): List<Game>

    @GET("games/genres")
    suspend fun getAllGenres(): List<String>

    // review
    @GET("reviews/game/{gameId}")
    suspend fun getReviewsByGame(@Path("gameId") gameId: String): List<Review>?

    @POST("reviews")
    suspend fun addReview(@Body review: Review): Review

    @DELETE("reviews/{id}")
    suspend fun deleteReview(@Path("id") id: String): retrofit2.Response<Unit>

}