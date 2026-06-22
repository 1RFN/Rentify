package com.irfanjayadi.rentify.view.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.view.renter.HomeRenterActivity // Import ditambahkan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        // Menggunakan Coroutines untuk delay 2.5 detik tanpa membuat aplikasi freeze
        lifecycleScope.launch {
            delay(2500L) // Waktu tunggu 2500 milidetik (2.5 detik)

            // Cek apakah user sudah login sebelumnya
            if (auth.currentUser != null) {
                // Jika sudah login, langsung masuk ke dalam aplikasi utama
                startActivity(Intent(this@SplashActivity, HomeRenterActivity::class.java))
            } else {
                // Jika belum pernah login (atau sudah logout), arahkan ke halaman Login
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }

            // Tutup SplashActivity agar user tidak bisa kembali ke halaman ini dengan tombol "Back"
            finish()
        }
    }
}