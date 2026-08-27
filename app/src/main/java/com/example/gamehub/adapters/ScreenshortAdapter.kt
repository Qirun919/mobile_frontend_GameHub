package com.example.gamehub

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gamehub.models.GameImage

class ScreenshotAdapter(private val screenshots: List<GameImage>) :
    RecyclerView.Adapter<ScreenshotAdapter.ScreenshotViewHolder>() {

    class ScreenshotViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenshotViewHolder {
        val imageView = ImageView(parent.context)
        imageView.layoutParams = ViewGroup.LayoutParams(400, ViewGroup.LayoutParams.MATCH_PARENT)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        return ScreenshotViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ScreenshotViewHolder, position: Int) {
        holder.imageView.load(screenshots[position].url)
    }

    override fun getItemCount() = screenshots.size
}