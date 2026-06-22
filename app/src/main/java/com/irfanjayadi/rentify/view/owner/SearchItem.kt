package com.irfanjayadi.rentify.view.owner

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
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
import com.irfanjayadi.rentify.view.adapter.ItemOwnerAdapter

class SearchItem : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etSearchItem: EditText
    private lateinit var ivClearSearch: ImageView
    private lateinit var rvItemCategories: RecyclerView
    private lateinit var rvSearchItemResults: RecyclerView
    private lateinit var layoutEmptyItems: View
    private lateinit var tvItemCount: TextView
    private lateinit var tvSortLabel: TextView

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var itemAdapter: ItemOwnerAdapter

    private val allItems = mutableListOf<Item>()
    private var currentKeyword = ""
    private var currentCategory = "Semua"
    private var currentSort = "terbaru"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_item, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etSearchItem = view.findViewById(R.id.etSearchItem)
        ivClearSearch = view.findViewById(R.id.ivClearSearch)
        rvItemCategories = view.findViewById(R.id.rvItemCategories)
        rvSearchItemResults = view.findViewById(R.id.rvSearchItemResults)
        layoutEmptyItems = view.findViewById(R.id.layoutEmptyItems)
        tvItemCount = view.findViewById(R.id.tvItemCount)
        tvSortLabel = view.findViewById(R.id.tvSortLabel)

        setupCategoryFilter()
        setupItemList()
        setupSearchListener()
        setupSortToggle()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    private fun setupCategoryFilter() {
        val initial = mutableListOf("Semua")
        categoryAdapter = CategoryAdapter(initial, currentCategory) { category ->
            currentCategory = category
            applyFilters()
        }
        rvItemCategories.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvItemCategories.adapter = categoryAdapter

        firestore.collection("categories").get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = mutableListOf("Semua")
                for (doc in snapshot) {
                    doc.getString("name")?.let { list.add(it) }
                }
                categoryAdapter.updateData(list)
            }
    }

    private fun setupItemList() {
        itemAdapter = ItemOwnerAdapter(
            items = allItems,
            onEdit = { item ->
                val intent = Intent(requireContext(), EditItemActivity::class.java)
                intent.putExtra("item_id", item.itemId)
                startActivity(intent)
            },
            onDelete = { item -> confirmDeleteItem(item) }
        )
        rvSearchItemResults.layoutManager = LinearLayoutManager(requireContext())
        rvSearchItemResults.adapter = itemAdapter
    }

    private fun setupSearchListener() {
        etSearchItem.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                ivClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        ivClearSearch.setOnClickListener {
            etSearchItem.text.clear()
            currentKeyword = ""
            applyFilters()
        }

        etSearchItem.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentKeyword = etSearchItem.text.toString().trim()
                applyFilters()
                true
            } else false
        }
    }

    private var sortOptions = arrayOf("Terbaru", "Termurah", "Termahal", "A-Z", "Z-A")
    private var sortIndex = 0

    private fun setupSortToggle() {
        tvSortLabel.text = sortOptions[sortIndex]
        tvSortLabel.setOnClickListener {
            sortIndex = (sortIndex + 1) % sortOptions.size
            tvSortLabel.text = sortOptions[sortIndex]
            currentSort = when (sortIndex) {
                0 -> "terbaru"
                1 -> "termurah"
                2 -> "termahal"
                3 -> "az"
                4 -> "za"
                else -> "terbaru"
            }
            applyFilters()
        }
    }

    private fun loadItems() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("items")
            .whereEqualTo("owner_id", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                allItems.clear()
                allItems.addAll(snapshot.toObjects(Item::class.java))
                applyFilters()
            }
    }

    private fun applyFilters() {
        var filtered = allItems.toList()

        if (currentCategory != "Semua") {
            filtered = filtered.filter { it.categoryName == currentCategory }
        }

        if (currentKeyword.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(currentKeyword, ignoreCase = true) ||
                        it.description.contains(currentKeyword, ignoreCase = true)
            }
        }

        filtered = when (currentSort) {
            "termurah" -> filtered.sortedBy { it.pricePerDay }
            "termahal" -> filtered.sortedByDescending { it.pricePerDay }
            "az" -> filtered.sortedBy { it.title.lowercase() }
            "za" -> filtered.sortedByDescending { it.title.lowercase() }
            else -> filtered.sortedByDescending { it.created_at }
        }

        tvItemCount.text = "${filtered.size} barang"
        itemAdapter.updateData(filtered)

        if (filtered.isEmpty()) {
            rvSearchItemResults.visibility = View.GONE
            layoutEmptyItems.visibility = View.VISIBLE
        } else {
            rvSearchItemResults.visibility = View.VISIBLE
            layoutEmptyItems.visibility = View.GONE
        }
    }

    private fun confirmDeleteItem(item: Item) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Barang")
            .setMessage("Yakin ingin menghapus \"${item.title}\"?")
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
                loadItems()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
