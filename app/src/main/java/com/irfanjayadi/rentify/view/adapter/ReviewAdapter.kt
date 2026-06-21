package com.irfanjayadi.rentify.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Review
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewAdapter(
    private val reviews: List<Review>
) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivRenterPhoto: CircleImageView = view.findViewById(R.id.ivRenterPhoto)
        val tvRenterName: TextView         = view.findViewById(R.id.tvRenterName)
        val ratingBar: RatingBar           = view.findViewById(R.id.ratingBarItem)
        val tvComment: TextView            = view.findViewById(R.id.tvComment)
        val tvDate: TextView               = view.findViewById(R.id.tvReviewDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]

        holder.tvRenterName.text  = review.renterName
        holder.ratingBar.rating   = review.rating
        holder.tvComment.text     = review.comment.ifEmpty { "Tidak ada komentar." }

        // Format tanggal
        val sdf  = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        holder.tvDate.text = sdf.format(Date(review.createdAt))

        // Foto reviewer
        if (review.renterPhotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(review.renterPhotoUrl)
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(holder.ivRenterPhoto)
        } else {
            holder.ivRenterPhoto.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun getItemCount() = reviews.size
}