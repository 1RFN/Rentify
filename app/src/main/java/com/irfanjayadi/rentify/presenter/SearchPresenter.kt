package com.irfanjayadi.rentify.presenter

import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.model.entity.Item
import com.irfanjayadi.rentify.presenter.contract.SearchContract

class SearchPresenter(private val view: SearchContract.View) : SearchContract.Presenter {

    private val firestore = FirebaseFirestore.getInstance()

    // Cache data lokal agar tidak request berkali-kali ke server
    private var allItems = listOf<Item>()
    private var isDataLoaded = false

    override fun searchItems(keyword: String, category: String, sortBy: String, location: String) {
        if (!isDataLoaded) {
            view.showLoading()
            firestore.collection("items")
                .whereEqualTo("status", "Tersedia")
                .get()
                .addOnSuccessListener { snapshot ->
                    allItems = snapshot.toObjects(Item::class.java)
                    isDataLoaded = true
                    view.hideLoading()
                    applyFiltersLocally(keyword, category, sortBy, location)
                }
                .addOnFailureListener { e ->
                    view.hideLoading()
                    view.showError(e.message ?: "Terjadi kesalahan saat memuat data")
                }
        } else {
            applyFiltersLocally(keyword, category, sortBy, location)
        }
    }

    private fun applyFiltersLocally(keyword: String, category: String, sortBy: String, location: String = "") {
        var filteredList = allItems

        if (category.isNotEmpty() && category != "Semua") {
            filteredList = filteredList.filter { it.categoryName == category }
        }

        if (keyword.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.title.contains(keyword, ignoreCase = true) ||
                        it.description.contains(keyword, ignoreCase = true)
            }
        }

        // Filter berdasarkan lokasi (jika mode "Barang Tersedia di Dekatmu")
        if (location.isNotEmpty()) {
            val locationLower = location.lowercase()
            filteredList = filteredList.filter {
                it.location.isNotEmpty() && it.location.lowercase().contains(locationLower)
            }
        }

        filteredList = when (sortBy) {
            "price_asc"   -> filteredList.sortedBy { it.pricePerDay }
            "price_desc"  -> filteredList.sortedByDescending { it.pricePerDay }
            "rating_desc" -> filteredList.sortedByDescending { it.rating }
            "rating_asc"  -> filteredList.sortedBy { it.rating }
            else          -> filteredList.sortedByDescending { it.created_at }
        }

        if (filteredList.isEmpty()) {
            view.showEmptyState()
        } else {
            view.showSearchResults(filteredList)
        }
    }
}