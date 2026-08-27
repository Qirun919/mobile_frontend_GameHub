package com.example.gamehub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gamehub.models.Game

class GameAdapter(
    private val games: List<Game>,
    private val onItemClick: (Game) -> Unit
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView = view.findViewById(R.id.imageCover)
        val title: TextView = view.findViewById(R.id.textTitle)
        val price: TextView = view.findViewById(R.id.textPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]
        holder.title.text = game.title
        holder.price.text = "RM ${game.price}"
        holder.cover.load(game.coverImage?.url)

        holder.itemView.setOnClickListener {
            onItemClick(game)
        }
    }

    override fun getItemCount() = games.size
}