package com.irfanjayadi.rentify.view.shared

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import de.hdodenhof.circleimageview.CircleImageView

class EditProfileActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var ivEditPhoto: CircleImageView
    private lateinit var loadingOverlay: FrameLayout

    private var imageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                imageUri = uri
                ivEditPhoto.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // 1. Inisialisasi Mesin Cloudinary
        try {
            val config = mapOf("cloud_name" to "dalwy5um8")
            MediaManager.init(this, config)
        } catch (e: Exception) {
            // Abaikan jika MediaManager sudah pernah diinisialisasi sebelumnya
        }

        firestore = FirebaseFirestore.getInstance()
        auth      = FirebaseAuth.getInstance()

        val etName        = findViewById<TextInputEditText>(R.id.etEditName)
        val etEmail       = findViewById<TextInputEditText>(R.id.etEditEmail)
        val etPhone       = findViewById<TextInputEditText>(R.id.etEditPhone)
        val etAddress     = findViewById<TextInputEditText>(R.id.etEditAddress)
        val tvChangePhoto = findViewById<TextView>(R.id.tvChangePhoto)
        val btnSave       = findViewById<MaterialButton>(R.id.btnSaveProfile)
        val btnBack       = findViewById<ImageView>(R.id.btnBack)

        ivEditPhoto    = findViewById(R.id.ivEditPhoto)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        btnBack.setOnClickListener { finish() }

        val selectImageAction = View.OnClickListener { pickImageLauncher.launch("image/*") }
        tvChangePhoto.setOnClickListener(selectImageAction)
        ivEditPhoto.setOnClickListener(selectImageAction)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        etName.setText(doc.getString("name"))
                        etEmail.setText(doc.getString("email"))
                        etPhone.setText(doc.getString("phone"))
                        etAddress.setText(doc.getString("address"))

                        val currentPhotoUrl = doc.getString("profilePhotoUrl") ?: ""
                        if (currentPhotoUrl.isNotEmpty()) {
                            loadProfileImage(currentPhotoUrl)
                        }
                    }
                }
        }

        btnSave.setOnClickListener {
            val newName    = etName.text.toString().trim()
            val newPhone   = etPhone.text.toString().trim()
            val newAddress = etAddress.text.toString().trim()

            if (newName.isEmpty() || newPhone.isEmpty() || newAddress.isEmpty()) {
                Toast.makeText(this, "Semua data wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (userId == null) return@setOnClickListener

            loadingOverlay.visibility = View.VISIBLE

            if (imageUri != null) {
                uploadImageAndSave(imageUri!!, newName, newPhone, newAddress, userId)
            } else {
                saveDataToFirestore(newName, newPhone, newAddress, null, userId)
            }
        }
    }

    private fun loadProfileImage(url: String) {
        Glide.with(this)
            .load(url)
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(ivEditPhoto)
    }

    // 2. Fungsi Upload Menggunakan Cloudinary SDK
    private fun uploadImageAndSave(uri: Uri, name: String, phone: String, address: String, userId: String) {
        MediaManager.get().upload(uri)
            .unsigned("g1tohcnv") // Gunakan preset unsigned Anda
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    // Proses dimulai (loading overlay sudah aktif dari btnSave)
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Anda bisa menambahkan fitur loading persentase di sini nanti untuk video/foto besar
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    // Ambil URL aman yang dikembalikan oleh Cloudinary
                    val imageUrl = resultData["secure_url"] as String
                    saveDataToFirestore(name, phone, address, imageUrl, userId)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this@EditProfileActivity, "Gagal upload: ${error.description}", Toast.LENGTH_LONG).show()
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Fungsi jika koneksi terputus dan dilanjutkan kembali
                }
            })
            .dispatch() // Eksekusi perintah
    }

    private fun saveDataToFirestore(name: String, phone: String, address: String, photoUrl: String?, userId: String) {
        val updates = mutableMapOf<String, Any>(
            "name"    to name,
            "phone"   to phone,
            "address" to address
        )
        if (photoUrl != null) updates["profilePhotoUrl"] = photoUrl

        firestore.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                loadingOverlay.visibility = View.GONE
                Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                loadingOverlay.visibility = View.GONE
                Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}