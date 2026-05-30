package com.irfanjayadi.rentify.view.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.view.renter.HomeRenterActivity // Import ditambahkan

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Cek apakah user sudah login sebelumnya, jika ya langsung ke Home
        if (auth.currentUser != null) {
            startActivity(Intent(this, HomeRenterActivity::class.java))
            finish()
        }

        val etEmail = findViewById<TextInputEditText>(R.id.etEmailLogin)
        val etPassword = findViewById<TextInputEditText>(R.id.etPasswordLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                        // Pindah ke Dashboard/Home setelah berhasil
                        startActivity(Intent(this, HomeRenterActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Login Gagal: Periksa kembali email dan password", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}