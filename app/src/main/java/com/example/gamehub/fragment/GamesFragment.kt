package com.example.gamehub.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.gamehub.CartActivity
import com.example.gamehub.GameAdapter
import com.example.gamehub.GameDetailsActivity
import com.example.gamehub.MainActivity
import com.example.gamehub.R
import com.example.gamehub.adapters.BannerAdapter
import com.example.gamehub.adapters.NewReleaseGameAdapter
import com.example.gamehub.models.Game
import com.example.gamehub.network.CartManager
import com.example.gamehub.network.RetrofitInstance
import kotlinx.coroutines.launch

class GamesFragment : Fragment() {

    private var currentPage = 0
    private val pageSize = 10
    private lateinit var recyclerNewRelease: RecyclerView
    private lateinit var newReleaseAdapter: NewReleaseGameAdapter
    private val newReleaseGames = mutableListOf<Game>()
    private var currentGenre: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // go SearchFragment
        val searchBar = view.findViewById<EditText>(R.id.searchBar)
        searchBar.setOnClickListener {
            (activity as MainActivity).openSearch()
        }

        // Cart
        val buttonCart = view.findViewById<ImageButton>(R.id.buttonCart)
        buttonCart.setOnClickListener {
            Log.d("GameHub", "Cart button clicked!")
            startActivity(Intent(requireContext(), CartActivity::class.java))
        }

        // Popular RecyclerView
        val recyclerPopular = view.findViewById<RecyclerView>(R.id.recyclerPopular)
        recyclerPopular.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // New Release RecyclerView
        recyclerNewRelease = view.findViewById(R.id.recyclerNewRelease)
        recyclerNewRelease.layoutManager = LinearLayoutManager(requireContext())
        newReleaseAdapter = NewReleaseGameAdapter(newReleaseGames) { game ->
            openGameDetails(game)
        }
        recyclerNewRelease.adapter = newReleaseAdapter

        // Show More
        val buttonShowMore = view.findViewById<Button>(R.id.buttonShowMore)
        buttonShowMore.setOnClickListener {
            loadNextPage()
        }

        // load
        loadBanner()
        loadGenreFilter(view)
        loadPopularGames(recyclerPopular)
        loadNextPage()
    }

    // Banner
    private fun loadBanner() {
        lifecycleScope.launch {
            try {
                val games = RetrofitInstance.api.getPopularGames()
                val bannerGames = games.take(5)
                val viewPager = view?.findViewById<ViewPager2>(R.id.bannerViewPager) ?: return@launch
                val dotsContainer = view?.findViewById<LinearLayout>(R.id.bannerDots) ?: return@launch

                viewPager.adapter = BannerAdapter(bannerGames) { game ->
                    openGameDetails(game)
                }

                // Dots
                setupBannerDots(dotsContainer, bannerGames.size, viewPager)

                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val runnable = object : Runnable {
                    override fun run() {
                        val current = viewPager.currentItem
                        val next = if (current + 1 >= bannerGames.size) 0 else current + 1
                        viewPager.setCurrentItem(next, true)
                        handler.postDelayed(this, 3000) // 3 sec
                    }
                }
                handler.postDelayed(runnable, 3000)

            } catch (e: Exception) {
                Log.e("GameHub", "Error loading banner: ${e.message}")
            }
        }
    }

    private fun setupBannerDots(container: LinearLayout, count: Int, viewPager: ViewPager2) {
        val dots = Array(count) { View(requireContext()) }
        dots.forEach { dot ->
            val params = LinearLayout.LayoutParams(12, 12).apply {
                setMargins(6, 0, 6, 0)
            }
            dot.layoutParams = params
            dot.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0x44FFFFFF)
            }
            container.addView(dot)
        }
        dots[0].background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(0xFFFFFFFF.toInt())
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.forEachIndexed { index, dot ->
                    (dot.background as android.graphics.drawable.GradientDrawable)
                        .setColor(if (index == position) 0xFFFFFFFF.toInt() else 0x44FFFFFF)
                }
            }
        })
    }

    // Genre Filter
    private fun loadGenreFilter(view: View) {
        lifecycleScope.launch {
            try {
                val genres = RetrofitInstance.api.getAllGenres()
                val container = view.findViewById<LinearLayout>(R.id.genreFilterContainer)

                addGenreChip(container, "All", true) {
                    currentGenre = null
                    resetNewRelease()
                    loadNextPage()
                }

                genres.forEach { genre ->
                    addGenreChip(container, genre, false) {
                        filterByGenre(genre)
                    }
                }
            } catch (e: Exception) {
                Log.e("GameHub", "Error loading genres: ${e.message}")
            }
        }
    }

    private var selectedChip: TextView? = null

    private fun addGenreChip(
        container: LinearLayout,
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        val chip = TextView(requireContext()).apply {
            this.text = text
            textSize = 12f
            setTextColor(if (isSelected) 0xFF1B2838.toInt() else 0xFFFFFFFF.toInt())
            setPadding(24, 12, 24, 12)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 32f
                setColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFF2A3F5F.toInt())
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 12, 0) }
            layoutParams = params
            setOnClickListener {
                selectedChip?.let { prev ->
                    prev.setTextColor(0xFFFFFFFF.toInt())
                    (prev.background as android.graphics.drawable.GradientDrawable)
                        .setColor(0xFF2A3F5F.toInt())
                }
                setTextColor(0xFF1B2838.toInt())
                (background as android.graphics.drawable.GradientDrawable)
                    .setColor(0xFFFFFFFF.toInt())
                selectedChip = this
                onClick()
            }
        }

        if (isSelected) selectedChip = chip

        container.addView(chip)
    }

    private fun filterByGenre(genre: String) {
        currentGenre = genre
        resetNewRelease()
        loadNextPage()
    }

    private fun resetNewRelease() {
        currentPage = 0
        newReleaseGames.clear()
        newReleaseAdapter.notifyDataSetChanged()
    }

    // Popular
    private fun loadPopularGames(recyclerPopular: RecyclerView) {
        lifecycleScope.launch {
            try {
                val popularGames = RetrofitInstance.api.getPopularGames()
                recyclerPopular.adapter = GameAdapter(popularGames) { game ->
                    openGameDetails(game)
                }
            } catch (e: Exception) {
                Log.e("GameHub", "Error loading popular games: ${e.message}")
            }
        }
    }

    // New Release
    private fun loadNextPage() {
        lifecycleScope.launch {
            try {
                val newGames = if (currentGenre == null) {
                    RetrofitInstance.api.getGamesPaged(currentPage, pageSize)
                } else {
                    RetrofitInstance.api.getGamesByGenre(currentGenre!!, currentPage, pageSize) ?: emptyList()
                }

                if (newGames.isEmpty()) {
                    view?.findViewById<Button>(R.id.buttonShowMore)?.visibility = View.GONE
                } else {
                    newReleaseGames.addAll(newGames)
                    newReleaseAdapter.notifyDataSetChanged()
                    currentPage++
                    view?.findViewById<Button>(R.id.buttonShowMore)?.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("GameHub", "Error: ${e.message}")
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