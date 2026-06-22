package com.irfanjayadi.rentify.view.renter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Item
import com.irfanjayadi.rentify.view.adapter.CategoryAdapter
import com.irfanjayadi.rentify.view.adapter.ItemRenterAdapter

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var itemAdapter: ItemRenterAdapter
    private val itemList = mutableListOf<Item>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth      = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val tvUserName     = view.findViewById<TextView>(R.id.tvUserName)
        val tvUserLocation = view.findViewById<TextView>(R.id.tvUserLocation)
        val rvItems        = view.findViewById<RecyclerView>(R.id.rvRecommendations)
        val layoutEmpty    = view.findViewById<LinearLayout>(R.id.layoutEmptyRecommendation)

        // Setup RecyclerView
        itemAdapter = ItemRenterAdapter(itemList) { item ->
            // Klik card → buka halaman detail
            val intent = Intent(requireContext(), ItemDetailActivity::class.java)
            intent.putExtra("item_id", item.itemId)
            startActivity(intent)
        }

        // Logika jika kotak pencarian di Home ditekan
        val etSearchHome = view.findViewById<android.widget.EditText>(R.id.etSearchQuery) // Pastikan ID ini ada di fragment_home.xml

        // Karena di home sifatnya hanya "pintu masuk", saat ditekan langsung pindah ke SearchFragment
        etSearchHome?.isFocusable = false // Agar tidak memunculkan keyboard di Home
        etSearchHome?.setOnClickListener {
            (activity as? HomeRenterActivity)?.navigateToSearchWithData()
        }

        val rvCategories = view.findViewById<RecyclerView>(R.id.rvCategories)

        val defaultCategories = listOf("Semua", "Motor", "Mobil", "Kamera", "Sepeda", "Console", "Alat Camping")
        val homeCategoryAdapter = CategoryAdapter(defaultCategories, "Semua") { selectedCategory ->
            (activity as? HomeRenterActivity)?.navigateToSearchWithData(category = selectedCategory)
        }

        rvCategories.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvCategories.adapter = homeCategoryAdapter
        rvCategories.visibility = View.VISIBLE

        rvItems.layoutManager = LinearLayoutManager(requireContext())
        rvItems.adapter = itemAdapter
        rvItems.isNestedScrollingEnabled = false

        // Load data user
        fetchUserData(tvUserName, tvUserLocation)

        // Load semua barang yang tersedia
        loadItems(rvItems, layoutEmpty)

        return view
    }

    private fun fetchUserData(tvName: TextView, tvLocation: TextView) {
        val userId = auth.currentUser?.uid ?: run {
            tvName.text     = "Halo, Tamu!"
            tvLocation.text = "Silakan login ulang"
            return
        }

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fullName  = document.getString("name") ?: "Pengguna"
                    val firstName = fullName.split(" ")[0]
                    tvName.text = "Halo, $firstName!"

                    val address = document.getString("address") ?: ""
                    tvLocation.text = if (address.isNotEmpty()) address else "Lokasi belum diatur"
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal memuat data profil", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadItems(rvItems: RecyclerView, layoutEmpty: LinearLayout) {
        // Ambil semua barang berstatus "Tersedia"
        firestore.collection("items")
            .whereEqualTo("status", "Tersedia")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val items = snapshot.toObjects(Item::class.java)
                itemAdapter.updateData(items)

                if (items.isEmpty()) {
                    rvItems.visibility     = View.GONE
                    layoutEmpty.visibility = View.VISIBLE
                } else {
                    rvItems.visibility     = View.VISIBLE
                    layoutEmpty.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(context, "Gagal memuat barang", Toast.LENGTH_SHORT).show()
            }
    }
}