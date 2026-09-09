package com.example.gamehub.models

data class Order(
    val id: String? = null,
    val userId: String,
    val gameId: List<String>,
    val totalPrice: Double = 0.0,
    val paymentStatus: String? = null,
    val stripePaymentIntentId: String? = null,
    val clientSecret: String? = null
)

data class CreateOrderRequest(
    val userId: String,
    val gameId: List<String>
)