package com.irfanjayadi.rentify.view.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Notification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private var notifications: List<Notification>,
    private val onNotifClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

    fun updateData(newNotifs: List<Notification>) {
        notifications = newNotifs
        notifyDataSetChanged()
    }

    inner class NotifViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view.findViewById(R.id.layoutNotifContainer)
        val indicator: View = view.findViewById(R.id.viewUnreadIndicator)
        val title: TextView = view.findViewById(R.id.tvNotifTitle)
        val message: TextView = view.findViewById(R.id.tvNotifMessage)
        val date: TextView = view.findViewById(R.id.tvNotifDate)

        fun bind(notif: Notification) {
            title.text = notif.title
            message.text = notif.massage // Menggunakan variabel 'massage' sesuai databasemu

            // Format timestamp menjadi tanggal yang mudah dibaca
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            date.text = sdf.format(Date(notif.createdAt))

            // Logika UI untuk dibaca vs belum dibaca
            if (notif.isRead) {
                indicator.visibility = View.INVISIBLE
                container.setBackgroundColor(Color.parseColor("#FFFFFF")) // Putih solid
            } else {
                indicator.visibility = View.VISIBLE
                container.setBackgroundColor(Color.parseColor("#F5F9F5")) // Sedikit kehijauan
            }

            container.setOnClickListener {
                onNotifClick(notif)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size
}