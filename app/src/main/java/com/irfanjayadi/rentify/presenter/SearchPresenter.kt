package com.irfanjayadi.rentify.presenter

import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.model.entity.Item
import com.irfanjayadi.rentify.presenter.contract.SearchContract

class SearchPresenter(private val view: SearchContract.View) : SearchContract.Presenter {

    private val firestore = FirebaseFirestore.getInstance()

    // Cache data lokal agar tidak request berkali-kali ke server
    private var allItems = listOf<Item>()
    private var isDataLoaded = false

    override fun searchItems(keyword: String, category: String, sortBy: String) {
        if (!isDataLoaded) {
            view.showLoading()
            // Tarik semua barang yang "Tersedia" SAJA (sekali di awal)
            firestore.collection("items")
                .whereEqualTo("status", "Tersedia")
                .get()
                .addOnSuccessListener { snapshot ->
                    allItems = snapshot.toObjects(Item::class.java)
                    isDataLoaded = true
                    view.hideLoading()

                    // Setelah data terkumpul, lempar ke fungsi penyaring lokal
                    applyFiltersLocally(keyword, category, sortBy)
                }
                .addOnFailureListener { e ->
                    view.hideLoading()
                    view.showError(e.message ?: "Terjadi kesalahan saat memuat data")
                }
        } else {
            // Jika data sudah ditarik sebelumnya, langsung saring saja (Instan)
            applyFiltersLocally(keyword, category, sortBy)
        }
    }

    private fun applyFiltersLocally(keyword: String, category: String, sortBy: String) {
        var filteredList = allItems

        // 1. Saring berdasarkan Kategori
        if (category.isNotEmpty() && category != "Semua") {
            filteredList = filteredList.filter { it.categoryName == category }
        }

        // 2. Saring berdasarkan Teks Pencarian
        if (keyword.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.title.contains(keyword, ignoreCase = true) ||
                        it.description.contains(keyword, ignoreCase = true)
            }
        }

        // 3. Urutkan Data (Kotlin pintar mengurutkan meski isi cuma 1 barang)
        filteredList = when (sortBy) {
            "price_asc"   -> filteredList.sortedBy { it.pricePerDay }
            "price_desc"  -> filteredList.sortedByDescending { it.pricePerDay }
            "rating_desc" -> filteredList.sortedByDescending { it.rating }
            "rating_asc"  -> filteredList.sortedBy { it.rating }
            // Default: Urutkan berdasarkan waktu ditambahkan (Paling Baru)
            else          -> filteredList.sortedByDescending { it.created_at }
        }

        // 4. Tampilkan Hasil
        if (filteredList.isEmpty()) {
            view.showEmptyState()
        } else {
            view.showSearchResults(filteredList)
        }
    }
}