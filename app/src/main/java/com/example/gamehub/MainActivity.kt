package com.example.gamehub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gamehub.models.Game
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import com.example.gamehub.network.WebSocketManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var currentPage = 0
    private val pageSize = 10
    private lateinit var containerNewRelease: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        TokenManager.init(this)

        val recyclerPopular = findViewById<RecyclerView>(R.id.recyclerPopular)
        recyclerPopular.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        containerNewRelease = findViewById(R.id.containerNewRelease)

        val buttonLogout = findViewById<Button>(R.id.buttonLogout)
        buttonLogout.setOnClickListener {
            WebSocketManager.disconnect()
            TokenManager.clearToken()
            Log.d("GameHub", "Logged out")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        val buttonShowMore = findViewById<Button>(R.id.buttonShowMore)
        buttonShowMore.setOnClickListener {
            loadNextPage()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_games

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_games -> {
                    true
                }
                R.id.nav_friends -> {
                    startActivity(Intent(this, FriendsActivity::class.java))
                    true
                }
                R.id.nav_community -> {
                    true
                }
                R.id.nav_profile -> {
                    true
                }
                else -> false
            }
        }

        loadPopularGames(recyclerPopular)
        loadNextPage()
    }

    private fun loadPopularGames(recyclerPopular: RecyclerView) {
        lifecycleScope.launch {
            try {
                val popularGames = RetrofitInstance.api.getPopularGames()
                recyclerPopular.adapter = GameAdapter(popularGames) { game ->
                    openGameDetails(game)
                }
                Log.d("GameHub", "Got ${popularGames.size} popular games")
            } catch (e: Exception) {
                Log.e("GameHub", "Error loading popular games: ${e.message}")
            }
        }
    }

    private fun loadNextPage() {
        lifecycleScope.launch {
            lifecycleScope.launch {
                try {
                    val newGames = RetrofitInstance.api.getGamesPaged(currentPage, pageSize)
                    for (game in newGames) {
                        val itemView =
                            layoutInflater.inflate(R.layout.item_game, containerNewRelease, false)
                        itemView.findViewById<TextView>(R.id.textTitle).text = game.title
                        itemView.findViewById<TextView>(R.id.textPrice).text = "RM ${game.price}"
                        itemView.findViewById<ImageView>(R.id.imageCover).load(game.coverImage?.url)
                        itemView.setOnClickListener {
                            openGameDetails(game)
                        }
                        containerNewRelease.addView(itemView)
                    }
                    currentPage++
                    Log.d("GameHub", "Loaded page $currentPage")
                } catch (e: Exception) {
                    Log.e("GameHub", "Error loading new release games: ${e.message}")
                }
            }
        }
    }
    private fun openGameDetails(game: Game) {
            val gameJson = RetrofitInstance.moshi.adapter(Game::class.java).toJson(game)
            val intent = Intent(this, GameDetailsActivity::class.java)
            intent.putExtra("game_json", gameJson)
            startActivity(intent)
    }
}

