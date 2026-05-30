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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import de.hdodenhof.circleimageview.CircleImageView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private fun uploadImageAndSave(uri: Uri, name: String, phone: String, address: String, userId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("Tidak bisa membuka file")
                val bytes = inputStream.readBytes()
                inputStream.close()

                // Format spesifik untuk Cloudinary
                val base64Image = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

                // TODO: PASTIKAN ANDA SUDAH MENGGANTI INI DENGAN DATA DARI CLOUDINARY ANDA
                val cloudName    = "dalwy5um8" // Contoh: "dxyz123"
                val uploadPreset = "g1tohcnv" // Contoh: "ml_default"

                val url        = java.net.URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod  = "POST"
                    doOutput       = true
                    // INI BARIS YANG DITAMBAHKAN AGAR CLOUDINARY BISA MEMBACA DATANYA
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                }

                val postData = "upload_preset=$uploadPreset&file=" + java.net.URLEncoder.encode(base64Image, "UTF-8")
                connection.outputStream.use { it.write(postData.toByteArray(Charsets.UTF_8)) }

                if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json     = org.json.JSONObject(response)

                    // Mengambil URL aman dari Cloudinary
                    val imageUrl = json.getString("secure_url")

                    withContext(Dispatchers.Main) {
                        saveDataToFirestore(name, phone, address, imageUrl, userId)
                    }
                } else {
                    // Membaca pesan error detail dari server Cloudinary jika gagal
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error tidak diketahui"
                    throw Exception("HTTP ${connection.responseCode}: $errorResponse")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this@EditProfileActivity, "Gagal upload: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
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