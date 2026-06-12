package com.irfanjayadi.rentify.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Item

class ItemOwnerAdapter(
    private val items: MutableList<Item>,
    private val onEdit: (Item) -> Unit,
    private val onDelete: (Item) -> Unit
) : RecyclerView.Adapter<ItemOwnerAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivItemImage: ImageView = view.findViewById(R.id.ivItemImage)
        val tvItemTitle: TextView = view.findViewById(R.id.tvItemTitle)
        val tvItemStatus: TextView = view.findViewById(R.id.tvItemStatus)
        val tvItemPrice: TextView = view.findViewById(R.id.tvItemPrice)
        val btnMenu: ImageButton = view.findViewById(R.id.btnItemMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_owner_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvItemTitle.text = item.title
        holder.tvItemStatus.text = item.status
        holder.tvItemPrice.text = "Rp ${String.format("%,.0f", item.pricePerDay)}/hari"

        // Warna status
        val statusColor = when (item.status.lowercase()) {
            "tersedia" -> holder.itemView.context.getColor(R.color.green_rentify)
            "disewa" -> holder.itemView.context.getColor(android.R.color.holo_orange_dark)
            else -> holder.itemView.context.getColor(android.R.color.darker_gray)
        }
        holder.tvItemStatus.setTextColor(statusColor)

        // Load gambar pertama dari list media
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

        // Popup menu titik 3
        holder.btnMenu.setOnClickListener { anchor ->
            val popup = PopupMenu(anchor.context, anchor)
            popup.menuInflater.inflate(R.menu.menu_item_options, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> { onEdit(item); true }
                    R.id.action_delete -> { onDelete(item); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}