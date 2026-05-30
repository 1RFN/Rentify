package com.irfanjayadi.rentify.view.renter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.view.shared.ProfileFragment


class HomeRenterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_renter)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set fragment awal saat aplikasi dibuka
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_search -> loadFragment(SearchFragment()) // Nanti dikerjakan Farhan
                R.id.nav_history -> loadFragment(HistoryFragment()) // Nanti dikerjakan Nanang
                R.id.nav_profile -> loadFragment(ProfileFragment()) // Nanti Anda kerjakan (Role Switcher)
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}