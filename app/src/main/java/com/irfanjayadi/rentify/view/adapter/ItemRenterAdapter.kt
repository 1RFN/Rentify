package com.irfanjayadi.rentify.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Item
import java.text.NumberFormat
import java.util.Locale

class ItemRenterAdapter(
    private val items: MutableList<Item>,
    private val onClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemRenterAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivItemImage: ImageView   = view.findViewById(R.id.ivItemImage)
        val tvItemTitle: TextView    = view.findViewById(R.id.tvItemTitle)
        val tvItemCategory: TextView = view.findViewById(R.id.tvItemCategory)
        val tvItemPrice: TextView    = view.findViewById(R.id.tvItemPrice)
        val tvItemStatus: TextView   = view.findViewById(R.id.tvItemStatus)
        val tvItemRating: TextView   = view.findViewById(R.id.tvItemRating) // tambahan rating
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_renter_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvItemTitle.text    = item.title
        holder.tvItemCategory.text = item.categoryName

        val formatted = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
            .format(item.pricePerDay)
        holder.tvItemPrice.text = "Rp $formatted/hari"

        holder.tvItemStatus.text = item.status
        val statusColor = when (item.status.lowercase()) {
            "tersedia" -> holder.itemView.context.getColor(R.color.green_rentify)
            "disewa"   -> holder.itemView.context.getColor(android.R.color.holo_orange_dark)
            else       -> holder.itemView.context.getColor(android.R.color.darker_gray)
        }
        holder.tvItemStatus.setTextColor(statusColor)

        // tampilkan rating
        holder.tvItemRating.text = item.rating.toString()

        val firstImage = item.media.firstOrNull()
        if (!firstImage.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(firstImage)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .centerCrop()
                .into(holder.ivItemImage)
        } else {
            holder.ivItemImage.setImageResource(R.drawable.ic_profile)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
