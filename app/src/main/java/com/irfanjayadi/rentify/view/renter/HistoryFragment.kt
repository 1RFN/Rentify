package com.irfanjayadi.rentify.view.renter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Transaction
import com.irfanjayadi.rentify.view.adapter.HistoryOrderAdapter

class HistoryFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var historyAdapter: HistoryOrderAdapter
    private val transactionList = mutableListOf<Transaction>()

    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmptyHistory: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        rvHistory = view.findViewById(R.id.rvHistory)
        layoutEmptyHistory = view.findViewById(R.id.layoutEmptyHistory)

        historyAdapter = HistoryOrderAdapter(transactionList) { transaction ->
            val intent = Intent(requireContext(), AddReviewActivity::class.java)
            intent.putExtra("item_id", transaction.itemId)
            intent.putExtra("transaction_id", transaction.transactionId) // Kirim ID Transaksi
            startActivity(intent)
        }
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter

        return view
    }

    // Pindah ke onResume agar selalu update saat tab dibuka!
    override fun onResume() {
        super.onResume()
        loadHistoryData()
    }

    private fun loadHistoryData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("transactions")
            .whereEqualTo("renter_id", userId)
            .orderBy("start_date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val transactions = snapshot.toObjects(Transaction::class.java)
                historyAdapter.updateData(transactions)

                if (transactions.isEmpty()) {
                    rvHistory.visibility = View.GONE
                    layoutEmptyHistory.visibility = View.VISIBLE
                } else {
                    rvHistory.visibility = View.VISIBLE
                    layoutEmptyHistory.visibility = View.GONE

                    // --- CEK POPUP ULASAN ---
                    val unreviewedTx = transactions.firstOrNull { it.status == "Selesai" && !it.isReviewed }
                    if (unreviewedTx != null) {
                        showReviewPopup(unreviewedTx)
                    }
                }
            }
    }

    private fun showReviewPopup(transaction: Transaction) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Pesanan Selesai!")
            .setMessage("Masa sewa Anda telah selesai. Yuk, berikan ulasan untuk membantu penyewa lain!")
            .setPositiveButton("Beri Ulasan") { _, _ ->
                val intent = Intent(requireContext(), AddReviewActivity::class.java)
                intent.putExtra("item_id", transaction.itemId)
                intent.putExtra("transaction_id", transaction.transactionId)
                startActivity(intent)
            }
            .setNegativeButton("Nanti Saja", null)
            .show()
    }
}