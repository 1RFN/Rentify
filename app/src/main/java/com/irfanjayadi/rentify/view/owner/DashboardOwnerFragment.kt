package com.irfanjayadi.rentify.view.owner

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.irfanjayadi.rentify.view.adapter.ItemOwnerAdapter
import java.text.NumberFormat
import java.util.Locale

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
    private val itemList = mutableListOf<Item>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_owner, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Init views
        tvDashboardName       = view.findViewById(R.id.tvDashboardName)
        tvStatActiveItems     = view.findViewById(R.id.tvStatActiveItems)
        tvStatIncomingOrders  = view.findViewById(R.id.tvStatIncomingOrders)
        tvStatRevenue         = view.findViewById(R.id.tvStatRevenue)
        tvEmptyOrders         = view.findViewById(R.id.tvEmptyOrders)
        tvEmptyItems          = view.findViewById(R.id.tvEmptyItems)
        rvIncomingOrders      = view.findViewById(R.id.rvIncomingOrders)
        rvMyItems             = view.findViewById(R.id.rvMyItems)

        // Setup RecyclerView barang
        itemAdapter = ItemOwnerAdapter(
            items    = itemList,
            onEdit   = { item -> openEditItem(item) },
            onDelete = { item -> confirmDeleteItem(item) }
        )
        rvMyItems.layoutManager = LinearLayoutManager(requireContext())
        rvMyItems.adapter = itemAdapter
        rvMyItems.isNestedScrollingEnabled = false

        // FAB tambah barang
        view.findViewById<FloatingActionButton>(R.id.fabAddItem).setOnClickListener {
            startActivity(Intent(requireContext(), AddItemActivity::class.java))
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val userId = auth.currentUser?.uid ?: return

        // 1. Nama user
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val name = doc.getString("name") ?: "Pemilik"
                tvDashboardName.text = "Dashboard ${name.split(" ")[0]}"
            }

        // 2. Barang milik owner
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

        // 3. Pesanan masuk (status = "pending" / "menunggu")
        firestore.collection("orders")
            .whereEqualTo("owner_id", userId)
            .whereEqualTo("status", "menunggu")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val orderCount = snapshot.size()
                tvStatIncomingOrders.text = orderCount.toString()

                if (orderCount == 0) {
                    tvEmptyOrders.visibility = View.VISIBLE
                    rvIncomingOrders.visibility = View.GONE
                } else {
                    tvEmptyOrders.visibility = View.GONE
                    rvIncomingOrders.visibility = View.VISIBLE
                    // TODO: setup order adapter jika sudah dibuat
                }
            }

        // 4. Pendapatan bulan ini (pesanan status = "selesai" bulan ini)
        firestore.collection("orders")
            .whereEqualTo("owner_id", userId)
            .whereEqualTo("status", "selesai")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                var totalRevenue = 0.0
                for (doc in snapshot.documents) {
                    totalRevenue += doc.getDouble("total_price") ?: 0.0
                }

                val formatted = NumberFormat.getNumberInstance(Locale("id", "ID"))
                    .format(totalRevenue)
                tvStatRevenue.text = "Rp $formatted"
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
}