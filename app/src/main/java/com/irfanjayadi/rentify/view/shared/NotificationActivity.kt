package com.irfanjayadi.rentify.view.shared

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Notification
import com.irfanjayadi.rentify.view.adapter.NotificationAdapter

class NotificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var rvNotifications: RecyclerView
    private lateinit var layoutEmptyNotif: View
    private lateinit var tvMarkAllRead: TextView
    private lateinit var ivBack: ImageView

    private lateinit var adapter: NotificationAdapter
    private val notifList = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        rvNotifications = findViewById(R.id.rvNotifications)
        layoutEmptyNotif = findViewById(R.id.layoutEmptyNotif)
        tvMarkAllRead = findViewById(R.id.tvMarkAllRead)
        ivBack = findViewById(R.id.ivBack)

        ivBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadNotifications()

        tvMarkAllRead.setOnClickListener {
            markAllAsRead()
        }

        findViewById<ImageView>(R.id.ivDeleteAll).setOnClickListener {
            deleteAllNotifications()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(mutableListOf()) { clickedNotif ->
            // Jika notif diklik, tandai spesifik notif ini menjadi terbaca
            if (!clickedNotif.isRead) {
                firestore.collection("notifications")
                    .document(clickedNotif.notificationId)
                    .update("is_read", true)
            }
            // Kamu juga bisa menambahkan Intent di sini berdasarkan clickedNotif.type
            // Misal: jika type == "MINTA_ULASAN", arahkan ke HistoryFragment
        }
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter
    }

    private fun loadNotifications() {
        val currentUserId = auth.currentUser?.uid ?: return

        // PERBAIKAN: Gunakan "user_id"
        firestore.collection("notifications")
            .whereEqualTo("user_id", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotifError", "Error Firestore: ${error.message}")
                    Toast.makeText(this, "Gagal memuat notifikasi", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    notifList.clear()
                    val rawList = snapshot.toObjects(Notification::class.java)
                    val sortedList = rawList.sortedWith(compareBy<Notification> { it.isRead }.thenByDescending { it.createdAt })

                    notifList.addAll(sortedList)
                    adapter.updateData(notifList)

                    if (notifList.isEmpty()) {
                        layoutEmptyNotif.visibility = View.VISIBLE
                        rvNotifications.visibility = View.GONE
                    } else {
                        layoutEmptyNotif.visibility = View.GONE
                        rvNotifications.visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun markAllAsRead() {
        val currentUserId = auth.currentUser?.uid ?: return

        // PERBAIKAN: Gunakan "user_id"
        firestore.collection("notifications")
            .whereEqualTo("user_id", currentUserId)
            .whereEqualTo("is_read", false)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "Semua sudah dibaca", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()
                for (document in snapshot.documents) {
                    batch.update(document.reference, "is_read", true)
                }

                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, "Semua notifikasi ditandai dibaca", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun deleteAllNotifications() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("notifications")
            .whereEqualTo("user_id", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) return@addOnSuccessListener

                val batch = firestore.batch()
                for (document in snapshot.documents) {
                    batch.delete(document.reference) // Perintah hapus dokumen
                }

                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, "Semua notifikasi dihapus", Toast.LENGTH_SHORT).show()
                }
            }
    }
}