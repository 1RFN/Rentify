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
            startActivity(intent)
        }

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter

        loadHistoryData()

        return view
    }

    private fun loadHistoryData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("transactions")
            .whereEqualTo("renter_id", userId)
            // Karena memakai whereEqualTo dan orderBy, mungkin nanti butuh 1 indeks baru
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
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(context, "Gagal memuat riwayat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}