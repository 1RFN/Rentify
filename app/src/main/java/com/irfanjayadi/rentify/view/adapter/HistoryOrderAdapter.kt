package com.irfanjayadi.rentify.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryOrderAdapter(
    private val transactions: MutableList<Transaction>,
    private val onReviewClick: (Transaction) -> Unit
) : RecyclerView.Adapter<HistoryOrderAdapter.ViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView   = view.findViewById(R.id.tvHistoryItemName)
        val tvStatus: TextView     = view.findViewById(R.id.tvHistoryStatus)
        val tvOwnerName: TextView  = view.findViewById(R.id.tvHistoryOwnerName)
        val tvDates: TextView      = view.findViewById(R.id.tvHistoryDates)
        val tvPrice: TextView      = view.findViewById(R.id.tvHistoryPrice)
        val btnReview: MaterialButton = view.findViewById(R.id.btnGiveReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]

        // Format Harga
        val formattedPrice = NumberFormat.getNumberInstance(Locale("id", "ID")).format(transaction.totalPrice)
        holder.tvPrice.text = "Rp $formattedPrice"

        // Format Tanggal
        val sdf = SimpleDateFormat("dd MMM yyy", Locale("id", "ID"))
        val startStr = transaction.startDate?.toDate()?.let { sdf.format(it) } ?: "-"
        val endStr = transaction.endDate?.toDate()?.let { sdf.format(it) } ?: "-"
        holder.tvDates.text = "$startStr - $endStr"

        // Atur Status dan Warnanya
        holder.tvStatus.text = transaction.status
        val context = holder.itemView.context
        val statusColor = when (transaction.status.lowercase()) {
            "menunggu konfirmasi" -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            "disewa" -> ContextCompat.getColor(context, android.R.color.holo_blue_dark)
            "selesai" -> ContextCompat.getColor(context, R.color.green_rentify)
            "ditolak" -> ContextCompat.getColor(context, R.color.red_rentify)
            else -> ContextCompat.getColor(context, android.R.color.darker_gray)
        }
        holder.tvStatus.setTextColor(statusColor)

        // Tampilkan tombol Review HANYA jika status = Selesai
        if (transaction.status.equals("Selesai", ignoreCase = true) && !transaction.isReviewed) {
            holder.btnReview.visibility = View.VISIBLE
            holder.btnReview.setOnClickListener { onReviewClick(transaction) }
        } else {
            holder.btnReview.visibility = View.GONE
        }

        // Ambil Nama Pemilik
        firestore.collection("users").document(transaction.ownerId).get()
            .addOnSuccessListener { doc ->
                holder.tvOwnerName.text = "Pemilik: ${doc.getString("name") ?: "Tidak diketahui"}"
            }

        // Ambil Nama Barang
        firestore.collection("items").document(transaction.itemId).get()
            .addOnSuccessListener { doc ->
                holder.tvItemName.text = doc.getString("title") ?: "Barang tidak ditemukan"
            }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<Transaction>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        notifyDataSetChanged()
    }
}