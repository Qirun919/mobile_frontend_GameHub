package com.example.gamehub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gamehub.adapters.ReviewAdapter
import com.example.gamehub.models.Game
import com.example.gamehub.models.Review
import com.example.gamehub.network.CartManager
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class GameDetailsActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer;

    private fun loadReviews(recycler: RecyclerView, gameId: String) {
        lifecycleScope.launch {
            try {
                val reviews = RetrofitInstance.api.getReviewsByGame(gameId) ?: emptyList()
                (recycler.adapter as? ReviewAdapter)?.updateData(reviews)
                Log.d("GameHub", "Reviews loaded: ${reviews.size}")
            } catch (e: Exception) {
                Log.e("GameHub", "Error loading reviews: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_details)

        val gameJson = intent.getStringExtra("game_json")
        val game = RetrofitInstance.moshi.adapter(Game::class.java).fromJson(gameJson ?: "")

        if (game == null) {
            finish()
            return
        }

        findViewById<ImageView>(R.id.imageCover).load(game.coverImage?.url)

        // trailer url player
        val playerView = findViewById<PlayerView>(R.id.playerView)

        if (game.trailerUrl.isNullOrEmpty()) {
            playerView.visibility = android.view.View.GONE
        } else {
            playerView.visibility = android.view.View.VISIBLE

            player = ExoPlayer.Builder(this).build()
            playerView.player = player

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(game.trailerUrl))
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }

        findViewById<TextView>(R.id.textTitle).text = game.title

        val webDescription = findViewById<android.webkit.WebView>(R.id.webDescription)
        webDescription.settings.javaScriptEnabled = true
        webDescription.settings.loadWithOverviewMode = true
        webDescription.settings.useWideViewPort = true

        webDescription.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                view?.evaluateJavascript(
                    "(function() { return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight); })();"
                ) { height ->
                    runOnUiThread {
                        val heightPx = height.toFloat().toInt()
                        val params = view.layoutParams
                        params.height = heightPx
                        view.layoutParams = params
                        view.requestLayout()
                    }
                }
            }
        }

        val htmlContent = """
            <html>
            <head>
            <style>
                body { 
                    background-color: #1B2838; 
                    color: #C6D4DF; 
                    font-size: 24px;
                    font-family: sans-serif;
                    margin: 0;
                    padding: 0;
                }
                img { max-width: 100%; height: auto; }
                iframe { max-width: 100%; }
            </style>
            </head>
            <body>
                ${game.detailedDescription ?: game.description ?: ""}
            </body>
            </html>
        """.trimIndent()

        webDescription.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

        checkOwnership(game)

        val recyclerScreenshots = findViewById<RecyclerView>(R.id.recyclerScreenshots)
        recyclerScreenshots.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerScreenshots.adapter = ScreenshotAdapter(game.screenshots ?: emptyList())

        findViewById<Button>(R.id.buttonAddToCart).setOnClickListener {
            CartManager.addToCart(game.id)
            Toast.makeText(this, "${game.title} added to cart", Toast.LENGTH_SHORT).show()
        }

        // go back button
        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener {
            finish()
        }

        // Developer
        findViewById<TextView>(R.id.textDeveloper).text =
            game.developers?.joinToString(", ") ?: "Unknown"

        // Publisher
        findViewById<TextView>(R.id.textPublisher).text =
            game.publishers?.joinToString(", ") ?: "Unknown"

        // Release Date
        findViewById<TextView>(R.id.textReleaseDate).text =
            game.releaseDate ?: "Unknown"

        // Price
        findViewById<TextView>(R.id.textPrice).text =
            if (game.price == 0.0) "Free to Play" else "RM %.2f".format(game.price)

        // Genres
        val genreContainer = findViewById<LinearLayout>(R.id.genreContainer)
        game.genres?.forEach { genre ->
            val chip = TextView(this).apply {
                text = genre.description
                textSize = 12f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(20, 8, 20, 8)
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 32f
                    setColor(0xFF2A3F5F.toInt())
                }
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 8, 0) }
                layoutParams = params
            }
            genreContainer.addView(chip)
        }

        if (game.genres.isNullOrEmpty()) {
            findViewById<LinearLayout>(R.id.genreContainer).visibility = View.GONE
        }

        // Platforms
        val platformContainer = findViewById<LinearLayout>(R.id.platformContainer)
        game.platforms?.let { platforms ->
            val platformList = mutableListOf<String>()
            if (platforms.windows) platformList.add("Windows")
            if (platforms.mac) platformList.add("Mac")
            if (platforms.linux) platformList.add("Linux")

            platformList.forEach { platform ->
                val chip = TextView(this).apply {
                    text = platform
                    textSize = 12f
                    setTextColor(0xFF8F98A0.toInt())
                    setPadding(20, 8, 20, 8)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 32f
                        setColor(0xFF1B2838.toInt())
                        setStroke(1, 0xFF2A3F5F.toInt())
                    }
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 8, 0) }
                    layoutParams = params
                }
                platformContainer.addView(chip)
            }
        }

        if (game.platforms == null) {
            findViewById<LinearLayout>(R.id.platformContainer).visibility = View.GONE
        }



        // review
        val recyclerReviews = findViewById<RecyclerView>(R.id.recyclerReviews)
        recyclerReviews.layoutManager = LinearLayoutManager(this)
        val reviewAdapter = ReviewAdapter(emptyList(), TokenManager.getUserId()) { review ->
            lifecycleScope.launch {
                try {
                    RetrofitInstance.api.deleteReview(review.id!!)
                    Log.d("GameHub", "Deleted review: ${review.id}")
                    loadReviews(recyclerReviews, game.id)
                    Log.d("GameHub", "Reloaded reviews")
                } catch (e: Exception) {
                    Log.e("GameHub", "Error deleting review: ${e.message}")
                }
            }
        }
        recyclerReviews.adapter = reviewAdapter

        // load reviews
        loadReviews(recyclerReviews, game.id)

        // submit review
        val editContent = findViewById<EditText>(R.id.editReviewContent)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val buttonSubmit = findViewById<Button>(R.id.buttonSubmitReview)

        buttonSubmit.setOnClickListener {
            val content = editContent.text.toString().trim()
            val rating = ratingBar.rating.toInt()
            val userId = TokenManager.getUserId()

            if (content.isEmpty()) {
                Toast.makeText(this, "Please write a review!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (rating == 0) {
                Toast.makeText(this, "Please select a rating!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (userId == null) {
                Toast.makeText(this, "Please login first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val review = Review(
                        id = null,
                        userId = userId,
                        username = TokenManager.getUsername(),
                        gameId = game.id,
                        content = content,
                        rating = rating
                    )
                    RetrofitInstance.api.addReview(review)
                    editContent.text.clear()
                    ratingBar.rating = 0f
                    loadReviews(recyclerReviews, game.id)
                    Toast.makeText(this@GameDetailsActivity, "Review submitted!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("GameHub", "Error submitting review: ${e.message}")
                }
            }
        }
    }

    private fun checkOwnership(game: Game) {
        val myUserId = TokenManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                val ownedGames = RetrofitInstance.api.getOwnedGames(myUserId)
                val alreadyOwned = ownedGames.any { it.id == game.id }

                val buttonAddToCart = findViewById<Button>(R.id.buttonAddToCart)

                if (alreadyOwned) {
                    buttonAddToCart.isEnabled = false
                    buttonAddToCart.text = "Already Owned"
                } else {
                    buttonAddToCart.isEnabled = true
                    buttonAddToCart.text = "Add to Cart"
                }

            } catch (e: Exception) {
                Log.e("GameHub", "Check ownership failed: ${e.message}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::player.isInitialized) {
            player.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::player.isInitialized) {
            player.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::player.isInitialized) {
            player.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::player.isInitialized) {
            player.release()
        }
    }
}