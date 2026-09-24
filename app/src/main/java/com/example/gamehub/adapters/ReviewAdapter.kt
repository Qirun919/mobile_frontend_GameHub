package com.example.gamehub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gamehub.R
import com.example.gamehub.models.Review

class ReviewAdapter(
    private var reviews: List<Review>,
    private val currentUserId: String?,
    private val onDelete: (Review) -> Unit
) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rating: TextView = view.findViewById(R.id.textRating)
        val userId: TextView = view.findViewById(R.id.textUserId)
        val content: TextView = view.findViewById(R.id.textContent)
        val delete: TextView = view.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]

        // star review
        val stars = "⭐".repeat(review.rating)
        holder.rating.text = stars
        holder.userId.text = review.username ?: "User: ${review.userId.takeLast(6)}" // only for last 6 number
        holder.content.text = review.content

        // can only delete your own review
        if (review.userId == currentUserId) {
            holder.delete.visibility = View.VISIBLE
            holder.delete.setOnClickListener { onDelete(review) }
        } else {
            holder.delete.visibility = View.GONE
        }
    }

    override fun getItemCount() = reviews.size

    fun updateData(newReviews: List<Review>) {
        reviews = newReviews.toMutableList()
        notifyDataSetChanged()
    }
}