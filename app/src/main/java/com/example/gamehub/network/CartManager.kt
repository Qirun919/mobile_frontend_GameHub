package com.example.gamehub.network

object CartManager {
    private val cartGameIds = mutableListOf<String>()
    
    fun addToCart(gameId: String) {
        if (!cartGameIds.contains(gameId)) {
            cartGameIds.add(gameId)
        }
    }

    fun removeFromCart(gameId: String) {
        cartGameIds.remove(gameId)
    }

    fun getCart(): List<String> {
        return cartGameIds.toList()
    }

    fun clearCart() {
        cartGameIds.clear()
    }

    fun getCartCount(): Int {
        return cartGameIds.size
    }
}