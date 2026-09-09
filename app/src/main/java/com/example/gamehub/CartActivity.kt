package com.example.gamehub

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.gamehub.models.CreateOrderRequest
import com.example.gamehub.models.Game
import com.example.gamehub.network.CartManager
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch

class CartActivity : ComponentActivity() {

    private lateinit var containerCart: LinearLayout
    private lateinit var textTotalPrice: TextView
    private lateinit var textError: TextView
    private var cartGames: List<Game> = emptyList()
    private lateinit var paymentSheet: PaymentSheet
    private var currentOrderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        currentOrderId = savedInstanceState?.getString("current_order_id")

        containerCart = findViewById(R.id.containerCart)
        textTotalPrice = findViewById(R.id.textTotalPrice)
        textError = findViewById(R.id.textError)

        paymentSheet = PaymentSheet(this, :: onPaymentSheetResult)

        findViewById<Button>(R.id.buttonCheckout).setOnClickListener {
            checkout()
        }

        loadCart()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_order_id", currentOrderId)
    }

    private fun loadCart() {
        val cartIds = CartManager.getCart()

        if (cartIds.isEmpty()) {
            containerCart.removeAllViews()
            textTotalPrice.text = "Total: RM 0.0"
            return
        }

        lifecycleScope.launch {
            try {
                cartGames = RetrofitInstance.api.getGamesByIds(cartIds)

                containerCart.removeAllViews()
                var total = 0.0

                for (game in cartGames) {
                    val itemView = layoutInflater.inflate(R.layout.item_cart, containerCart, false)
                    itemView.findViewById<TextView>(R.id.textCartGameTitle).text = game.title
                    itemView.findViewById<TextView>(R.id.textCartGamePrice).text = "RM ${game.price}"
                    itemView.findViewById<ImageView>(R.id.imageCartGame).load(game.coverImage?.url)

                    itemView.findViewById<Button>(R.id.buttonRemoveFromCart).setOnClickListener {
                        CartManager.removeFromCart(game.id)
                        loadCart()
                    }

                    containerCart.addView(itemView)
                    total += game.price
                }

                textTotalPrice.text = "Total: RM $total"

            } catch (e: Exception) {
                Log.e("GameHub", "Load cart failed: ${e.message}")
            }
        }
    }

    private fun checkout() {
        val myUserId = TokenManager.getUserId()
        if (myUserId == null) {
            textError.text = "You are not logged in"
            return
        }

        if (cartGames.isEmpty()) {
            textError.text = "Your cart is empty"
            return
        }

        lifecycleScope.launch {
            try {
                val gameIds = cartGames.map { it.id }
                val order = RetrofitInstance.api.createOrder(CreateOrderRequest(myUserId, gameIds))
                val orderId = order.id ?: return@launch
                currentOrderId = orderId

                val paymentOrder = RetrofitInstance.api.createPayment(orderId)
                val clientSecret = paymentOrder.clientSecret

                if (clientSecret != null) {
                    val configuration = PaymentSheet.Configuration("GameHub")
                    paymentSheet.presentWithPaymentIntent(clientSecret, configuration)
                } else {
                    textError.text = "Failed to create payment"
                }

            } catch (e: Exception) {
                Log.e("GameHub", "Checkout failed: ${e.message}")
                textError.text = "Checkout failed: ${e.message}"
            }
        }
    }

    private fun onPaymentSheetResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                Log.d("GameHub", "Payment completed")
                confirmOrder()
            }
            is PaymentSheetResult.Canceled -> {
                Log.d("GameHub", "Payment canceled")
                textError.text = "Payment canceled"
            }
            is PaymentSheetResult.Failed -> {
                Log.e("GameHub", "Payment failed: ${result.error.message}")
                textError.text = "Payment failed: ${result.error.message}"
            }
        }
    }

    private fun confirmOrder() {
        Log.d("GameHub", "confirmOrder called, currentOrderId = $currentOrderId")
        val orderId = currentOrderId ?: return
        Log.d("GameHub", "Confirming order: $orderId")

        lifecycleScope.launch {
            try {
                RetrofitInstance.api.confirmOrder(orderId)
                CartManager.clearCart()
                loadCart()
                textError.text = "Payment successful! Enjoy your games."
                Log.d("GameHub", "Order confirmed")
            } catch (e: Exception) {
                Log.e("GameHub", "Confirm order failed: ${e.message}")
            }
        }
    }
}