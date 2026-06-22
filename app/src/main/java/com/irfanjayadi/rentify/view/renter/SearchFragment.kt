package com.irfanjayadi.rentify.view.renter

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Item
import com.irfanjayadi.rentify.presenter.SearchPresenter
import com.irfanjayadi.rentify.presenter.contract.SearchContract
import com.irfanjayadi.rentify.view.adapter.CategoryAdapter
import com.irfanjayadi.rentify.view.adapter.ItemRenterAdapter

class SearchFragment : Fragment(), SearchContract.View {

    private lateinit var presenter: SearchPresenter
    private lateinit var itemAdapter: ItemRenterAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    // UI Components
    private lateinit var etSearchQuery: EditText
    private lateinit var ivClearSearch: ImageView
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var layoutEmptySearch: LinearLayout

    // Komponen Filter & Sort
    private lateinit var spinnerSortPrice: Spinner
    private lateinit var spinnerSortRating: Spinner
    private lateinit var tvResetFilter: TextView

    // State Variables
    private var currentKeyword = ""
    private var currentCategory = "Semua"
    private var currentSortBy = "default"

    // Mencegah trigger pencarian berulang saat reset filter via kode
    private var isProgrammaticChange = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        presenter = SearchPresenter(this)
        initViews(view)
        setupAdapters()
        setupListeners()

        // Ambil data yang dilempar dari HomeFragment (jika ada)
        arguments?.let {
            currentKeyword = it.getString("keyword", "")
            currentCategory = it.getString("category", "Semua")
        }

        etSearchQuery.setText(currentKeyword)
        categoryAdapter.setSelectedCategory(currentCategory)

        // Jalankan pencarian awal
        performSearch()

        return view
    }

    private fun initViews(view: View) {
        etSearchQuery = view.findViewById(R.id.etSearchQuery)
        ivClearSearch = view.findViewById(R.id.ivClearSearch)
        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        layoutEmptySearch = view.findViewById(R.id.layoutEmptySearch)

        // Setup Dropdown (Spinner) dan Reset
        spinnerSortPrice = view.findViewById(R.id.spinnerSortPrice)
        spinnerSortRating = view.findViewById(R.id.spinnerSortRating)
        tvResetFilter = view.findViewById(R.id.tvResetFilter)

        val priceOptions = arrayOf("Harga: Default", "Termurah", "Termahal")
        val ratingOptions = arrayOf("Rating: Default", "Tertinggi", "Terendah")

        spinnerSortPrice.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priceOptions)
        spinnerSortRating.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ratingOptions)

        // Setup Kategori
        val rvSearchCategories = view.findViewById<RecyclerView>(R.id.rvSearchCategories)
        val defaultCategories = listOf("Semua", "Motor", "Mobil", "Kamera", "Sepeda", "Console", "Alat Camping")
        categoryAdapter = CategoryAdapter(defaultCategories, currentCategory) { selectedCategory ->
            currentCategory = selectedCategory
            performSearch()
        }
        rvSearchCategories.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvSearchCategories.adapter = categoryAdapter
    }

    private fun setupAdapters() {
        itemAdapter = ItemRenterAdapter(mutableListOf()) { item ->
            val intent = Intent(requireContext(), ItemDetailActivity::class.java)
            intent.putExtra("item_id", item.itemId)
            startActivity(intent)
        }
        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        rvSearchResults.adapter = itemAdapter
    }

    private fun setupListeners() {
        // Logika ketik di search bar
        etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                ivClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Logika hapus teks
        ivClearSearch.setOnClickListener {
            etSearchQuery.text.clear()
            currentKeyword = ""
            performSearch()
        }

        // Logika Enter di Keyboard
        etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentKeyword = etSearchQuery.text.toString().trim()
                performSearch()
                true
            } else {
                false
            }
        }

        // Logika Dropdown Sort Harga
        spinnerSortPrice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isProgrammaticChange) return

                if (position != 0) {
                    // Jika user pilih filter Harga, matikan filter rating (kembali ke default)
                    isProgrammaticChange = true
                    spinnerSortRating.setSelection(0)
                    isProgrammaticChange = false

                    currentSortBy = if (position == 1) "price_asc" else "price_desc"
                } else if (spinnerSortRating.selectedItemPosition == 0) {
                    // Jika keduanya kembali ke posisi 0
                    currentSortBy = "default"
                }
                performSearch()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Logika Dropdown Sort Rating
        spinnerSortRating.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isProgrammaticChange) return

                if (position != 0) {
                    // Jika user pilih filter Rating, matikan filter harga (kembali ke default)
                    isProgrammaticChange = true
                    spinnerSortPrice.setSelection(0)
                    isProgrammaticChange = false

                    currentSortBy = if (position == 1) "rating_desc" else "rating_asc"
                } else if (spinnerSortPrice.selectedItemPosition == 0) {
                    // Jika keduanya kembali ke posisi 0
                    currentSortBy = "default"
                }
                performSearch()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Tombol Reset Atur Ulang
        tvResetFilter.setOnClickListener {
            // Ubah state dropdown tanpa mentrigger pencarian ganda
            isProgrammaticChange = true
            spinnerSortPrice.setSelection(0)
            spinnerSortRating.setSelection(0)
            isProgrammaticChange = false

            // Kembalikan semua state ke awal
            currentSortBy = "default"
            categoryAdapter.setSelectedCategory("Semua")
            currentCategory = "Semua"
            etSearchQuery.text.clear()
            currentKeyword = ""

            // Lakukan satu pencarian bersih
            performSearch()
        }
    }

    private fun performSearch() {
        presenter.searchItems(currentKeyword, currentCategory, currentSortBy)
    }

    // --- Implementasi SearchContract.View ---

    override fun showLoading() {
        rvSearchResults.visibility = View.GONE
        layoutEmptySearch.visibility = View.GONE
    }

    override fun hideLoading() {
    }

    override fun showSearchResults(items: List<Item>) {
        rvSearchResults.visibility = View.VISIBLE
        layoutEmptySearch.visibility = View.GONE
        itemAdapter.updateData(items)
    }

    override fun showEmptyState() {
        rvSearchResults.visibility = View.GONE
        layoutEmptySearch.visibility = View.VISIBLE
    }

    override fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}