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

class BannerAdapter(
    private val games: List<Game>,
    private val onClick: (Game) -> Unit
) : RecyclerView.Adapter<BannerAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.bannerImage)
        val title: TextView = view.findViewById(R.id.bannerTitle)
        val price: TextView = view.findViewById(R.id.bannerPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = games[position]
        holder.title.text = game.title
        holder.price.text = if (game.price == 0.0) "Free to Play" else "RM %.2f".format(game.price)
        holder.image.load(game.coverImage?.url) {
            crossfade(true)
        }
        holder.itemView.setOnClickListener { onClick(game) }
    }

    override fun getItemCount() = games.size
}