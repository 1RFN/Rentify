package com.irfanjayadi.rentify.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.irfanjayadi.rentify.R

class ImageSliderAdapter(
    private val imageUrls: List<String>
) : RecyclerView.Adapter<ImageSliderAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivSliderImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_slider, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(imageUrls[position])
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount() = imageUrls.size
}