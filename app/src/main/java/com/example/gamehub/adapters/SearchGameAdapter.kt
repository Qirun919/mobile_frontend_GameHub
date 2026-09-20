package com.example.gamehub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gamehub.R
import com.example.gamehub.models.Game

class SearchGameAdapter(
    private var games: List<Game>,
    private val onClick: (Game) -> Unit
) : RecyclerView.Adapter<SearchGameAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imageCover)
        val title: TextView = view.findViewById(R.id.textTitle)
        val genres: TextView = view.findViewById(R.id.textGenres)
        val price: TextView = view.findViewById(R.id.textPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_game, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = games[position]
        holder.title.text = game.title
        holder.genres.text = game.genres
            ?.joinToString(", ") { it.description ?: "" } ?: ""
        holder.price.text = if (game.price == 0.0) "Free" else "RM %.2f".format(game.price)
        holder.image.load(game.coverImage?.url) {
            crossfade(true)
        }
        holder.itemView.setOnClickListener { onClick(game) }
    }

    override fun getItemCount() = games.size

    fun updateData(newGames: List<Game>) {
        games = newGames
        notifyDataSetChanged()
    }
}