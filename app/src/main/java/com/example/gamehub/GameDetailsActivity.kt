package com.example.gamehub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
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
import com.example.gamehub.models.Game
import com.example.gamehub.network.CartManager
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class GameDetailsActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer;

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
        findViewById<TextView>(R.id.textPrice).text = "RM ${game.price}"
        findViewById<TextView>(R.id.textDescription).text = game.description

        checkOwnership(game)

        val recyclerScreenshots = findViewById<RecyclerView>(R.id.recyclerScreenshots)
        recyclerScreenshots.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerScreenshots.adapter = ScreenshotAdapter(game.screenshots ?: emptyList())

        findViewById<Button>(R.id.buttonAddToCart).setOnClickListener {
            CartManager.addToCart(game.id)
            Toast.makeText(this, "${game.title} added to cart", Toast.LENGTH_SHORT).show()
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