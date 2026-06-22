package com.irfanjayadi.rentify.view.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.User
import com.irfanjayadi.rentify.view.renter.HomeRenterActivity // Import ditambahkan

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val etName = findViewById<TextInputEditText>(R.id.etNameReg)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmailReg)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhoneReg)
        val etPassword = findViewById<TextInputEditText>(R.id.etPasswordReg)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etPasswordConfirmReg)

        val btnSubmit = findViewById<Button>(R.id.btnRegisterSubmit)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        // Pindah ke halaman Login
        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua kolom harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(this, "Password minimal 8 karakter!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Konfirmasi password tidak cocok!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Proses Mendaftar
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: ""

                        // Masukkan ke Data Model. Password tidak disimpan!
                        val newUser = User(
                            uid = userId,
                            name = name,
                            email = email,
                            phone = phone,
                            address = "",
                            role = "renter",
                            profilePhotoUrl = ""
                        )

                        firestore.collection("users").document(userId).set(newUser)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Berhasil Mendaftar!", Toast.LENGTH_SHORT).show()
                                // Langsung pindah ke Dashboard
                                startActivity(Intent(this@RegisterActivity, HomeRenterActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal menyimpan data profil: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}