package com.irfanjayadi.rentify.view.shared

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.irfanjayadi.rentify.R

class SecurityActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnChangePassword: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security)

        auth = FirebaseAuth.getInstance()

        // Initialize views
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnChangePassword = findViewById(R.id.btnChangePassword)

        btnBack.setOnClickListener { finish() }

        btnChangePassword.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Validasi input
        if (currentPassword.isEmpty()) {
            etCurrentPassword.error = "Password saat ini harus diisi"
            etCurrentPassword.requestFocus()
            return
        }

        if (newPassword.isEmpty()) {
            etNewPassword.error = "Password baru harus diisi"
            etNewPassword.requestFocus()
            return
        }

        if (newPassword.length < 8) {
            etNewPassword.error = "Password minimal 8 karakter"
            etNewPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Konfirmasi password harus diisi"
            etConfirmPassword.requestFocus()
            return
        }

        if (newPassword != confirmPassword) {
            etConfirmPassword.error = "Password tidak cocok"
            etConfirmPassword.requestFocus()
            Toast.makeText(this, "Konfirmasi password tidak cocok!", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentPassword == newPassword) {
            etNewPassword.error = "Password baru harus berbeda dengan password saat ini"
            etNewPassword.requestFocus()
            Toast.makeText(this, "Password baru tidak boleh sama dengan password lama!", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button saat proses
        btnChangePassword.isEnabled = false
        btnChangePassword.text = "Memproses..."

        // Re-authenticate user
        val user = auth.currentUser
        val email = user?.email

        if (user == null || email == null) {
            Toast.makeText(this, "User tidak ditemukan. Silakan login ulang.", Toast.LENGTH_SHORT).show()
            btnChangePassword.isEnabled = true
            btnChangePassword.text = "Ubah Password"
            return
        }

        // Buat kredensial untuk re-authentication
        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        // Re-authenticate
        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Jika re-authentication berhasil, update password
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Password berhasil diubah!",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Clear input fields
                        etCurrentPassword.text?.clear()
                        etNewPassword.text?.clear()
                        etConfirmPassword.text?.clear()
                        
                        // Enable button
                        btnChangePassword.isEnabled = true
                        btnChangePassword.text = "Ubah Password"
                        
                        // Kembali ke profile
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Gagal mengubah password: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        btnChangePassword.isEnabled = true
                        btnChangePassword.text = "Ubah Password"
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Password saat ini salah!",
                    Toast.LENGTH_SHORT
                ).show()
                etCurrentPassword.error = "Password salah"
                etCurrentPassword.requestFocus()
                btnChangePassword.isEnabled = true
                btnChangePassword.text = "Ubah Password"
            }
    }
}
