package com.example.gamehub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gamehub.models.Game
import com.example.gamehub.network.RetrofitInstance
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

        val buttonTrailer = findViewById<Button>(R.id.buttonTrailer)
        if (game.trailerUrl.isNullOrEmpty()) {
            buttonTrailer.visibility = android.view.View.GONE
        } else {
            buttonTrailer.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(game.trailerUrl))
                startActivity(intent)
            }
        }

        val recyclerScreenshots = findViewById<RecyclerView>(R.id.recyclerScreenshots)
        recyclerScreenshots.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerScreenshots.adapter = ScreenshotAdapter(game.screenshots ?: emptyList())
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