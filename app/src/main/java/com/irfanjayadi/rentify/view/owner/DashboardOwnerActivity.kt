package com.irfanjayadi.rentify.view.owner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.view.shared.ProfileFragment


class DashboardOwnerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_owner)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation_owner)

        // Set fragment awal ke Dashboard saat aplikasi dibuka
        if (savedInstanceState == null) {
            loadFragment(DashboardOwnerFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_owner_dashboard -> loadFragment(DashboardOwnerFragment())

                // Dua menu di bawah ini biarkan di-comment dulu sampai Anda atau tim membuatnya
                // R.id.nav_owner_items -> loadFragment(MyItemsFragment())
                // R.id.nav_owner_orders -> loadFragment(IncomingOrdersFragment())

                R.id.nav_owner_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_owner, fragment)
            .commit()
    }
}