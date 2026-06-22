package com.irfanjayadi.rentify.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OrderWithDetails(
    val transaction: Transaction,
    val itemTitle: String = "",
    val itemImage: String = "",
    val renterName: String = ""
)

class OrderOwnerAdapter(
    private val orders: MutableList<OrderWithDetails>,
    private val onAccept: (Transaction) -> Unit,
    private val onReject: (Transaction) -> Unit
) : RecyclerView.Adapter<OrderOwnerAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivItemImage: ImageView = view.findViewById(R.id.ivOrderItemImage)
        val tvItemTitle: TextView = view.findViewById(R.id.tvOrderItemTitle)
        val tvRenterName: TextView = view.findViewById(R.id.tvOrderRenterName)
        val tvDates: TextView = view.findViewById(R.id.tvOrderDates)
        val tvTotalPrice: TextView = view.findViewById(R.id.tvOrderTotalPrice)
        val tvStatus: TextView = view.findViewById(R.id.tvOrderStatus)
        val layoutActions: LinearLayout = view.findViewById(R.id.layoutOrderActions)
        val btnReject: TextView = view.findViewById(R.id.btnRejectOrder)
        val btnAccept: TextView = view.findViewById(R.id.btnAcceptOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        val tx = order.transaction

        holder.tvItemTitle.text = order.itemTitle.ifEmpty { "Item tidak diketahui" }
        holder.tvRenterName.text = "Penyewa: ${order.renterName.ifEmpty { "Memuat..." }}"

        val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))
        holder.tvTotalPrice.text = "Rp ${fmt.format(tx.totalPrice.toLong())}"

        // Dates
        val dateFmt = SimpleDateFormat("dd MMM", Locale("id", "ID"))
        val startStr = tx.startDate?.toDate()?.let { dateFmt.format(it) } ?: "?"
        val endStr = tx.endDate?.toDate()?.let { dateFmt.format(it) } ?: "?"
        holder.tvDates.text = "$startStr - $endStr"

        // Status badge
        val status = tx.status.lowercase()
        val statusDisplay = when (status) {
            "menunggu" -> "Menunggu"
            "disewa" -> "Disewa"
            "selesai" -> "Selesai"
            "ditolak" -> "Ditolak"
            else -> tx.status
        }
        holder.tvStatus.text = statusDisplay

        val statusBg = when (status) {
            "menunggu" -> R.drawable.bg_circle_yellow
            "disewa" -> R.drawable.bg_circle_yellow
            "selesai" -> R.drawable.bg_location_chip
            "ditolak" -> R.drawable.bg_mode_unselected
            else -> R.drawable.bg_circle_white
        }
        val statusColor = when (status) {
            "menunggu" -> ContextCompat.getColor(holder.itemView.context, R.color.black)
            "disewa" -> ContextCompat.getColor(holder.itemView.context, R.color.green_rentify)
            "selesai" -> ContextCompat.getColor(holder.itemView.context, R.color.gray_text)
            "ditolak" -> ContextCompat.getColor(holder.itemView.context, R.color.red_rentify)
            else -> ContextCompat.getColor(holder.itemView.context, R.color.black)
        }
        holder.tvStatus.setBackgroundResource(statusBg)
        holder.tvStatus.setTextColor(statusColor)

        // Item image
        if (order.itemImage.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(order.itemImage)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .centerCrop()
                .into(holder.ivItemImage)
        } else {
            holder.ivItemImage.setImageResource(R.drawable.ic_profile)
        }

        // Action buttons only for pending
        if (status == "menunggu") {
            holder.layoutActions.visibility = View.VISIBLE
            holder.btnAccept.setOnClickListener { onAccept(tx) }
            holder.btnReject.setOnClickListener { onReject(tx) }
        } else {
            holder.layoutActions.visibility = View.GONE
        }
    }

    override fun getItemCount() = orders.size

    fun updateData(newOrders: List<OrderWithDetails>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }
}
