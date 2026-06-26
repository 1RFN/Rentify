package com.irfanjayadi.rentify.view.renter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.view.shared.ProfileFragment

class HomeRenterActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_renter)

        bottomNav = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_search -> loadFragment(SearchFragment()) // Nanti dikerjakan Farhan
                R.id.nav_history -> loadFragment(HistoryFragment()) // Nanti dikerjakan Nanang
                R.id.nav_profile -> loadFragment(ProfileFragment()) // Nanti Anda kerjakan
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun navigateToSearchWithData(keyword: String = "", category: String = "Semua", nearby: Boolean = false) {
        val searchFragment = SearchFragment()
        val bundle = Bundle()
        bundle.putString("keyword", keyword)
        bundle.putString("category", category)
        bundle.putBoolean("nearby", nearby)
        searchFragment.arguments = bundle

        // 2. Load fragmentnya secara manual
        loadFragment(searchFragment)

        // 3. Ubah indikator bottom navigation agar menunjuk ke tab Search tanpa memicu listener lagi
        bottomNav.setOnItemSelectedListener(null)
        bottomNav.selectedItemId = R.id.nav_search

        // hidupkan listener kembali
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_search -> loadFragment(SearchFragment())
                R.id.nav_history -> loadFragment(HistoryFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }
}