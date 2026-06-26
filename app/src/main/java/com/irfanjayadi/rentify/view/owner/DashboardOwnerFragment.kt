package com.irfanjayadi.rentify.view.owner

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Item
import com.irfanjayadi.rentify.model.entity.Transaction
import com.irfanjayadi.rentify.view.adapter.IncomingOrderAdapter
import com.irfanjayadi.rentify.view.adapter.ItemOwnerAdapter
import java.text.NumberFormat
import java.util.Locale
import com.irfanjayadi.rentify.view.shared.NotificationActivity

class DashboardOwnerFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvDashboardName: TextView
    private lateinit var tvStatActiveItems: TextView
    private lateinit var tvStatIncomingOrders: TextView
    private lateinit var tvStatRevenue: TextView

    private lateinit var tvEmptyOrders: TextView
    private lateinit var tvEmptyItems: TextView
    private lateinit var rvIncomingOrders: RecyclerView
    private lateinit var rvMyItems: RecyclerView

    private lateinit var itemAdapter: ItemOwnerAdapter
    private lateinit var orderAdapter: IncomingOrderAdapter

    private val itemList = mutableListOf<Item>()
    private val orderList = mutableListOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_owner, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        tvDashboardName       = view.findViewById(R.id.tvDashboardName)
        tvStatActiveItems     = view.findViewById(R.id.tvStatActiveItems)
        tvStatIncomingOrders  = view.findViewById(R.id.tvStatIncomingOrders)
        tvStatRevenue         = view.findViewById(R.id.tvStatRevenue)
        tvEmptyOrders         = view.findViewById(R.id.tvEmptyOrders)
        tvEmptyItems          = view.findViewById(R.id.tvEmptyItems)
        rvIncomingOrders      = view.findViewById(R.id.rvIncomingOrders)
        rvMyItems             = view.findViewById(R.id.rvMyItems)

        itemAdapter = ItemOwnerAdapter(
            items    = itemList,
            onEdit   = { item -> openEditItem(item) },
            onDelete = { item -> confirmDeleteItem(item) }
        )
        rvMyItems.layoutManager = LinearLayoutManager(requireContext())
        rvMyItems.adapter = itemAdapter
        rvMyItems.isNestedScrollingEnabled = false

        orderAdapter = IncomingOrderAdapter(
            orders = orderList,
            onAccept = { transaction -> updateOrderStatus(transaction, "Disewa") },
            onReject = { transaction -> updateOrderStatus(transaction, "Ditolak") },
            onFinish = { transaction -> updateOrderStatus(transaction, "Selesai") }
        )
        rvIncomingOrders.layoutManager = LinearLayoutManager(requireContext())
        rvIncomingOrders.adapter = orderAdapter
        rvIncomingOrders.isNestedScrollingEnabled = false

        view.findViewById<FloatingActionButton>(R.id.fabAddItem).setOnClickListener {
            startActivity(Intent(requireContext(), AddItemActivity::class.java))
        }

        val btnNotif = view.findViewById<ImageView>(R.id.ivNotificationBell)
        btnNotif?.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        loadNotificationBadge(view)

        view.findViewById<TextView>(R.id.tvSeeAll).setOnClickListener {
            val activity = requireActivity()
            if (activity is DashboardOwnerActivity) {
                activity.switchToItemsTab()
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val name = doc.getString("name") ?: "Pemilik"
                tvDashboardName.text = "Dashboard ${name.split(" ")[0]}"
            }

        firestore.collection("items")
            .whereEqualTo("owner_id", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val items = snapshot.toObjects(Item::class.java)
                itemAdapter.updateData(items)

                val activeCount = items.count { it.status.equals("Tersedia", ignoreCase = true) }
                tvStatActiveItems.text = activeCount.toString()

                if (items.isEmpty()) {
                    tvEmptyItems.visibility = View.VISIBLE
                    rvMyItems.visibility = View.GONE
                } else {
                    tvEmptyItems.visibility = View.GONE
                    rvMyItems.visibility = View.VISIBLE
                }
            }

        firestore.collection("transactions")
            .whereEqualTo("owner_id", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val allTransactions = snapshot.toObjects(Transaction::class.java)
                val activeOrders = allTransactions.filter { it.status == "Menunggu Konfirmasi" }

                orderAdapter.updateData(activeOrders)
                tvStatIncomingOrders.text = activeOrders.size.toString()

                if (activeOrders.isEmpty()) {
                    tvEmptyOrders.visibility = View.VISIBLE
                    rvIncomingOrders.visibility = View.GONE
                } else {
                    tvEmptyOrders.visibility = View.GONE
                    rvIncomingOrders.visibility = View.VISIBLE
                }

                val completedOrders = allTransactions.filter { it.status == "Selesai" }
                val totalRevenue = completedOrders.sumOf { it.totalPrice }

                val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(totalRevenue)
                tvStatRevenue.text = "Rp $formatted"
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateOrderStatus(transaction: Transaction, newStatus: String, isAutoFinish: Boolean = false) {
        firestore.collection("transactions").document(transaction.transactionId)
            .update("status", newStatus)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener

                if (!isAutoFinish) {
                    val msg = when (newStatus) {
                        "Disewa" -> "Pesanan diterima"
                        "Ditolak" -> "Pesanan ditolak"
                        "Selesai" -> "Pesanan diselesaikan"
                        else -> "Status diperbarui"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }

                // Notifikasi
                val notifRef = firestore.collection("notifications").document("notif_${transaction.transactionId}_${newStatus}")
                val title = if (newStatus == "Disewa") "Pesanan Diterima! 🎉"
                else if (newStatus == "Ditolak") "Pesanan Ditolak 😔"
                else "Pesanan Selesai ✅"

                val message = if (newStatus == "Disewa") "Hore! Pemilik telah menyetujui pesananmu. Segera hubungi pemilik."
                else if (newStatus == "Ditolak") "Maaf, pemilik menolak pesananmu untuk saat ini."
                else "Pesanan telah selesai. Yuk beri ulasan untuk pengalamanmu!"

                val type = if (newStatus == "Selesai") "MINTA_ULASAN" else "STATUS_UPDATE"

                val notification = com.irfanjayadi.rentify.model.entity.Notification(
                    notificationId = notifRef.id,
                    userId = transaction.renterId,
                    title = title,
                    massage = message,
                    type = type
                )
                notifRef.set(notification)

                val itemRef = firestore.collection("items").document(transaction.itemId)

                // Mencegah error NotFound jika barang sudah dihapus
                firestore.runTransaction { firestoreTransaction ->
                    val snapshot = firestoreTransaction.get(itemRef)
                    if (snapshot.exists()) {
                        val currentStock = snapshot.getLong("stock")?.toInt() ?: 0

                        if (newStatus == "Disewa") {
                            val newStock = if (currentStock > 0) currentStock - 1 else 0
                            firestoreTransaction.update(itemRef, "stock", newStock)
                            if (newStock == 0) firestoreTransaction.update(itemRef, "status", "Disewa")
                        } else if (newStatus == "Selesai") {
                            val newStock = currentStock + 1
                            firestoreTransaction.update(itemRef, "stock", newStock)
                            firestoreTransaction.update(itemRef, "status", "Tersedia")
                        }
                    }
                }.addOnSuccessListener {
                    if (!isAutoFinish) loadDashboardData()
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(context, "Gagal mengubah status pesanan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openEditItem(item: Item) {
        val intent = Intent(requireContext(), EditItemActivity::class.java)
        intent.putExtra("item_id", item.itemId)
        startActivity(intent)
    }

    private fun confirmDeleteItem(item: Item) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Barang")
            .setMessage("Yakin ingin menghapus \"${item.title}\"? Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus") { _, _ -> deleteItem(item) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteItem(item: Item) {
        firestore.collection("items").document(item.itemId)
            .delete()
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Barang berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadDashboardData()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadNotificationBadge(view: View) {
        val userId = auth.currentUser?.uid ?: return
        val tvBadge = view.findViewById<TextView>(R.id.tvNotificationBadge)

        firestore.collection("notifications")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("is_read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || !isAdded) return@addSnapshotListener

                val unreadCount = snapshot?.size() ?: 0
                if (tvBadge != null) {
                    if (unreadCount > 0) {
                        tvBadge.visibility = View.VISIBLE
                        tvBadge.text = if (unreadCount > 9) "9+" else unreadCount.toString()
                    } else {
                        tvBadge.visibility = View.GONE
                    }
                }
            }
    }
}