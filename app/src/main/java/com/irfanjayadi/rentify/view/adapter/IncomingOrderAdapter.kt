package com.irfanjayadi.rentify.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class IncomingOrderAdapter(
    private val orders: MutableList<Transaction>,
    private val onAccept: (Transaction) -> Unit,
    private val onReject: (Transaction) -> Unit
) : RecyclerView.Adapter<IncomingOrderAdapter.ViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRenterName: TextView  = view.findViewById(R.id.tvOrderRenterName)
        val tvItemName: TextView    = view.findViewById(R.id.tvOrderItemName)
        val tvDates: TextView       = view.findViewById(R.id.tvOrderDates)
        val tvTotalPrice: TextView  = view.findViewById(R.id.tvOrderTotalPrice)
        val btnAccept: MaterialButton = view.findViewById(R.id.btnAccept)
        val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_incoming_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]

        // Format Harga
        val formattedPrice = NumberFormat.getNumberInstance(Locale("id", "ID")).format(order.totalPrice)
        holder.tvTotalPrice.text = "Rp $formattedPrice"

        // Format Tanggal
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val startStr = order.startDate?.toDate()?.let { sdf.format(it) } ?: "-"
        val endStr = order.endDate?.toDate()?.let { sdf.format(it) } ?: "-"
        holder.tvDates.text = "$startStr\ns.d.\n$endStr"

        // Ambil Nama Penyewa dari Firestore
        firestore.collection("users").document(order.renterId).get()
            .addOnSuccessListener { doc ->
                holder.tvRenterName.text = "Penyewa: ${doc.getString("name") ?: "Tidak diketahui"}"
            }

        // Ambil Nama Barang dari Firestore
        firestore.collection("items").document(order.itemId).get()
            .addOnSuccessListener { doc ->
                holder.tvItemName.text = doc.getString("title") ?: "Barang tidak diketahui"
            }

        // Aksi Tombol
        holder.btnAccept.setOnClickListener { onAccept(order) }
        holder.btnReject.setOnClickListener { onReject(order) }
    }

    override fun getItemCount() = orders.size

    fun updateData(newOrders: List<Transaction>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }
}