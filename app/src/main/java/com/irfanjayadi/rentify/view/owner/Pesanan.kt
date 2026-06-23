package com.irfanjayadi.rentify.view.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Transaction
import com.irfanjayadi.rentify.view.adapter.OrderOwnerAdapter
import com.irfanjayadi.rentify.view.adapter.OrderWithDetails

class Pesanan : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var rvOrders: RecyclerView
    private lateinit var layoutEmptyOrders: View
    private lateinit var tvOrderCount: TextView

    private lateinit var chipSemua: TextView
    private lateinit var chipMenunggu: TextView
    private lateinit var chipDisewa: TextView
    private lateinit var chipSelesai: TextView
    private lateinit var chipDitolak: TextView

    private val allOrders = mutableListOf<OrderWithDetails>()
    private lateinit var adapter: OrderOwnerAdapter
    private var currentFilter = "semua"

    private val chips = mutableListOf<TextView>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pesanan, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        rvOrders = view.findViewById(R.id.rvOrders)
        layoutEmptyOrders = view.findViewById(R.id.layoutEmptyOrders)
        tvOrderCount = view.findViewById(R.id.tvOrderCount)

        chipSemua = view.findViewById(R.id.chipSemua)
        chipMenunggu = view.findViewById(R.id.chipMenunggu)
        chipDisewa = view.findViewById(R.id.chipDisewa)
        chipSelesai = view.findViewById(R.id.chipSelesai)
        chipDitolak = view.findViewById(R.id.chipDitolak)

        chips.addAll(listOf(chipSemua, chipMenunggu, chipDisewa, chipSelesai, chipDitolak))

        setupChips()
        setupAdapter()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun setupChips() {
        chipSemua.setOnClickListener { selectFilter("semua", chipSemua) }
        chipMenunggu.setOnClickListener { selectFilter("menunggu", chipMenunggu) }
        chipDisewa.setOnClickListener { selectFilter("disewa", chipDisewa) }
        chipSelesai.setOnClickListener { selectFilter("selesai", chipSelesai) }
        chipDitolak.setOnClickListener { selectFilter("ditolak", chipDitolak) }
    }

    private fun selectFilter(filter: String, selected: TextView) {
        currentFilter = filter
        for (chip in chips) {
            chip.setBackgroundResource(
                if (chip == selected) R.drawable.bg_mode_selected else R.drawable.bg_mode_unselected
            )
            chip.setTextColor(
                if (chip == selected) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            )
        }
        applyFilter()
    }

    private fun setupAdapter() {
        adapter = OrderOwnerAdapter(
            orders = mutableListOf(),
            onAccept = { tx -> updateOrderStatus(tx, "Disewa") },
            onReject = { tx -> updateOrderStatus(tx, "Ditolak") },
            // Asumsi di OrderOwnerAdapter nanti ada callback ke-3 untuk Selesai
            onFinish = { tx -> updateOrderStatus(tx, "Selesai") }
        )
        rvOrders.layoutManager = LinearLayoutManager(requireContext())
        rvOrders.adapter = adapter
    }

    private fun loadOrders() {
        val userId = auth.currentUser?.uid ?: return

        // PERBAIKAN 1: Menggunakan tabel "transactions"
        firestore.collection("transactions")
            .whereEqualTo("owner_id", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val transactions = snapshot.toObjects(Transaction::class.java)

                if (transactions.isEmpty()) {
                    allOrders.clear()
                    adapter.updateData(allOrders)
                    showEmptyState()
                    tvOrderCount.text = "0 pesanan"
                    return@addOnSuccessListener
                }

                // FITUR OTOMATIS: Cek jika waktu sewa sudah habis (Selesai Otomatis)
                val nowSeconds = Timestamp.now().seconds
                for (tx in transactions) {
                    if (tx.status.equals("Disewa", true) && tx.endDate != null) {
                        if (tx.endDate!!.seconds < nowSeconds) {
                            // Waktu habis, update database secara background
                            updateOrderStatus(tx, "Selesai")
                            tx.status = "Selesai" // Update UI lokal sementara
                        }
                    }
                }

                val itemIds = transactions.map { it.itemId }.distinct()
                val renterIds = transactions.map { it.renterId }.distinct()

                val itemTasks = itemIds.chunked(10).map { chunk ->
                    firestore.collection("items").whereIn("item_id", chunk).get()
                }
                val userTasks = renterIds.chunked(10).map { chunk ->
                    firestore.collection("users").whereIn("uid", chunk).get()
                }

                val allTasks = itemTasks + userTasks

                com.google.android.gms.tasks.Tasks.whenAll(allTasks)
                    .addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener

                        val itemMap = itemTasks.flatMap { task ->
                            task.result?.documents?.mapNotNull { doc ->
                                val id = doc.getString("item_id") ?: return@mapNotNull null
                                id to Pair(
                                    doc.getString("title") ?: "",
                                    (doc.get("media") as? List<*>)?.firstOrNull()?.toString() ?: ""
                                )
                            } ?: emptyList()
                        }.toMap()

                        val userMap = userTasks.flatMap { task ->
                            task.result?.documents?.mapNotNull { doc ->
                                val id = doc.getString("uid") ?: return@mapNotNull null
                                id to (doc.getString("name") ?: "Pengguna")
                            } ?: emptyList()
                        }.toMap()

                        val combined = transactions.map { tx ->
                            val (title, image) = itemMap[tx.itemId] ?: Pair("", "")
                            val name = userMap[tx.renterId] ?: ""
                            OrderWithDetails(tx, title, image, name)
                        }

                        allOrders.clear()
                        allOrders.addAll(combined)
                        allOrders.sortByDescending { it.transaction.startDate?.toDate()?.time ?: 0L }
                        applyFilter()
                    }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(context, "Gagal memuat pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilter() {
        val filtered = if (currentFilter == "semua") {
            allOrders.toList()
        } else {
            allOrders.filter {
                // PERBAIKAN: Gunakan .contains dan ignoreCase agar kebal salah ketik/huruf besar
                val status = it.transaction.status
                when (currentFilter) {
                    "menunggu" -> status.contains("menunggu", ignoreCase = true)
                    "disewa"   -> status.contains("disewa", ignoreCase = true)
                    "selesai"  -> status.contains("selesai", ignoreCase = true)
                    "ditolak"  -> status.contains("ditolak", ignoreCase = true)
                    else -> true
                }
            }
        }

        tvOrderCount.text = "${filtered.size} pesanan"
        adapter.updateData(filtered)

        if (filtered.isEmpty()) {
            showEmptyState()
        } else {
            rvOrders.visibility = View.VISIBLE
            layoutEmptyOrders.visibility = View.GONE
        }
    }

    private fun showEmptyState() {
        rvOrders.visibility = View.GONE
        layoutEmptyOrders.visibility = View.VISIBLE
    }

    private fun updateOrderStatus(tx: Transaction, newStatus: String) {
        // PERBAIKAN 3: Memperbaiki logika update dan penambahan/pengurangan stok
        firestore.collection("transactions").document(tx.transactionId)
            .update("status", newStatus)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener

                // Jangan munculkan Toast jika ini Selesai Otomatis (dipicu oleh sistem)
                if (newStatus != "Selesai") {
                    val msg = if (newStatus == "Disewa") "Pesanan diterima" else "Pesanan ditolak"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }

                val itemRef = firestore.collection("items").document(tx.itemId)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(itemRef)
                    val currentStock = snapshot.getLong("stock")?.toInt() ?: 0

                    if (newStatus == "Disewa") {
                        // Jika diterima, kurangi stok
                        val newStock = if (currentStock > 0) currentStock - 1 else 0
                        transaction.update(itemRef, "stock", newStock)
                        if (newStock == 0) transaction.update(itemRef, "status", "Disewa")
                    } else if (newStatus == "Selesai") {
                        // Jika selesai, kembalikan stok
                        val newStock = currentStock + 1
                        transaction.update(itemRef, "stock", newStock)
                        transaction.update(itemRef, "status", "Tersedia")
                    }
                }.addOnSuccessListener {
                    // Hanya reload jika perubahannya manual (bukan dari fungsi loop otomatis di atas)
                    if(newStatus != "Selesai") loadOrders()
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}