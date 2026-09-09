package com.example.gamehub.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gamehub.CartActivity
import com.example.gamehub.GameAdapter
import com.example.gamehub.GameDetailsActivity
import com.example.gamehub.R
import com.example.gamehub.models.Game
import com.example.gamehub.network.CartManager
import com.example.gamehub.network.RetrofitInstance
import kotlinx.coroutines.launch

class GamesFragment : Fragment() {

    private var currentPage = 0
    private val pageSize = 10
    private lateinit var containerNewRelease: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerPopular = view.findViewById<RecyclerView>(R.id.recyclerPopular)
        recyclerPopular.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        containerNewRelease = view.findViewById(R.id.containerNewRelease)

        val buttonShowMore = view.findViewById<Button>(R.id.buttonShowMore)
        buttonShowMore.setOnClickListener {
            loadNextPage()
        }

        val buttonCart = view.findViewById<ImageButton>(R.id.buttonCart)
        buttonCart.setOnClickListener {
            startActivity(Intent(requireContext(), CartActivity::class.java))
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
            try {
                val newGames = RetrofitInstance.api.getGamesPaged(currentPage, pageSize)
                for (game in newGames) {
                    val itemView = layoutInflater.inflate(R.layout.item_game, containerNewRelease, false)
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

    private fun openGameDetails(game: Game) {
        val gameJson = RetrofitInstance.moshi.adapter(Game::class.java).toJson(game)
        val intent = Intent(requireContext(), GameDetailsActivity::class.java)
        intent.putExtra("game_json", gameJson)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateCartIcon()
    }

    private fun updateCartIcon() {
        val badge = view?.findViewById<TextView>(R.id.textCartBadge) ?: return
        val count = CartManager.getCartCount()

        if (count > 0) {
            badge.text = count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }
}