package com.irfanjayadi.rentify.view.renter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout fragment_home.xml
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val tvUserLocation = view.findViewById<TextView>(R.id.tvUserLocation)

        fetchUserData(tvUserName, tvUserLocation)

        return view
    }

    private fun fetchUserData(tvName: TextView, tvLocation: TextView) {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Mengambil nama dan memotongnya untuk mengambil nama depan saja
                        val fullName = document.getString("name") ?: "Pengguna"
                        val firstName = fullName.split(" ")[0]
                        tvName.text = "Halo, $firstName!"

                        // Mengambil lokasi (alamat)
                        val address = document.getString("address") ?: ""
                        if (address.isNotEmpty()) {
                            tvLocation.text = address
                        } else {
                            tvLocation.text = "Lokasi belum diatur"
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Gagal memuat data profil", Toast.LENGTH_SHORT).show()
                }
        } else {
            tvName.text = "Halo, Tamu!"
            tvLocation.text = "Silakan login ulang"
        }
    }
}