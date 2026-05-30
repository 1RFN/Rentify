package com.irfanjayadi.rentify.view.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R

class DashboardOwnerFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_owner, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val tvDashboardName = view.findViewById<TextView>(R.id.tvDashboardName)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: "Pemilik"
                    val firstName = name.split(" ")[0]
                    tvDashboardName.text = "Dashboard $firstName"
                }
        }

        // Logika untuk mengisi data statistik dan menyembunyikan empty state (tvEmptyOrders)
        // Akan dikerjakan selanjutnya saat menghubungkan dengan data transaksi

        return view
    }
}