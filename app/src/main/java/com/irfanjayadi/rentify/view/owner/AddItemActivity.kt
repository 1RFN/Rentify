package com.irfanjayadi.rentify.view.owner

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R

class AddItemActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // List URI foto yang dipilih (maks 5)
    private val selectedImageUris = mutableListOf<Uri>()
    private val uploadedImageUrls = mutableListOf<String>()

    private lateinit var llPhotoPreview: LinearLayout
    private lateinit var tvPhotoCount: TextView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvLoadingText: TextView

    // Launcher untuk memilih BANYAK gambar sekaligus
    private val pickMultipleImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                // Batasi maksimal 5 foto
                val toAdd = uris.take(5 - selectedImageUris.size)
                selectedImageUris.addAll(toAdd)
                updatePhotoPreview()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        try {
            MediaManager.init(applicationContext, mapOf("cloud_name" to "dalwy5um8"))
        } catch (e: Exception) {}

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        llPhotoPreview  = findViewById(R.id.llPhotoPreview)
        tvPhotoCount    = findViewById(R.id.tvPhotoCount)
        loadingOverlay  = findViewById(R.id.loadingOverlay)
        tvLoadingText   = findViewById(R.id.tvLoadingText)

        val etTitle       = findViewById<TextInputEditText>(R.id.etTitle)
        val etDescription = findViewById<TextInputEditText>(R.id.etDescription)
        val etPrice       = findViewById<TextInputEditText>(R.id.etPrice)
        val etStock       = findViewById<TextInputEditText>(R.id.etStock)
        val etCategory    = findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.etCategoryDropdown)
        val etLocation    = findViewById<TextInputEditText>(R.id.etLocation)

        // Kategori statis
        val categoryList = listOf("Motor", "Mobil", "Kamera", "Sepeda", "Console", "Alat Camping", "Lainnya")
        etCategory.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryList)
        )

        // Isi lokasi otomatis dari alamat user
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val address = doc.getString("address") ?: ""
                    if (address.isNotEmpty()) {
                        etLocation.setText(address)
                    }
                }
        }

        // Tombol pilih foto (bisa lebih dari 1)
        findViewById<MaterialButton>(R.id.btnPickPhoto).setOnClickListener {
            if (selectedImageUris.size >= 5) {
                Toast.makeText(this, "Maksimal 5 foto", Toast.LENGTH_SHORT).show()
            } else {
                pickMultipleImages.launch("image/*")
            }
        }

        // Tombol simpan
        findViewById<MaterialButton>(R.id.btnSaveItem).setOnClickListener {
            val title       = etTitle.text.toString().trim()
            val category    = etCategory.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val priceStr    = etPrice.text.toString().trim()
            val stockStr    = etStock.text.toString().trim()
            val location    = etLocation.text.toString().trim()

            if (title.isEmpty() || category.isEmpty() || description.isEmpty()
                || priceStr.isEmpty() || stockStr.isEmpty()) {
                Toast.makeText(this, "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedImageUris.isEmpty()) {
                Toast.makeText(this, "Pilih minimal 1 foto barang", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ownerId = auth.currentUser?.uid ?: return@setOnClickListener

            loadingOverlay.visibility = View.VISIBLE
            uploadedImageUrls.clear()

            val price = priceStr.toDoubleOrNull() ?: 0.0
            val stock = stockStr.toIntOrNull() ?: 1

            uploadNextImage(0, title, category, description, price, stock, ownerId, location)
        }
    }

    /**
     * Upload foto secara rekursif: selesai 1 → lanjut ke index berikutnya
     */
    private fun uploadNextImage(
        index: Int,
        title: String, category: String, description: String,
        price: Double, stock: Int, ownerId: String, location: String = ""
    ) {
        if (index >= selectedImageUris.size) {
            // Semua foto selesai di-upload → simpan ke Firestore
            tvLoadingText.text = "Menyimpan ke database..."
            saveItemToFirestore(title, category, description, price, stock, ownerId, uploadedImageUrls, location)
            return
        }

        val uri = selectedImageUris[index]
        val total = selectedImageUris.size
        tvLoadingText.text = "Mengunggah foto ${index + 1} dari $total..."

        MediaManager.get().upload(uri)
            .unsigned("g1tohcnv")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val pct = (bytes.toDouble() / totalBytes * 100).toInt()
                    tvLoadingText.text = "Mengunggah foto ${index + 1}/$total ($pct%)..."
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as String
                    uploadedImageUrls.add(url)
                    // Lanjut upload foto berikutnya
                    uploadNextImage(index + 1, title, category, description, price, stock, ownerId, location)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(
                        this@AddItemActivity,
                        "Gagal upload foto ${index + 1}: ${error.description}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    private fun saveItemToFirestore(
        title: String, category: String, description: String,
        price: Double, stock: Int, ownerId: String, imageUrls: List<String>,
        location: String = ""
    ) {
        val ref = firestore.collection("items").document()
        val data = hashMapOf(
            "category_name" to category,
            "description"   to description,
            "item_id"       to ref.id,
            "media"         to imageUrls,
            "owner_id"      to ownerId,
            "price_per_day" to price,
            "status"        to "Tersedia",
            "stock"         to stock,
            "title"         to title,
            "location"      to location,
            "created_at"    to System.currentTimeMillis()
        )
        ref.set(data)
            .addOnSuccessListener {
                loadingOverlay.visibility = View.GONE
                Toast.makeText(this, "Barang berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                loadingOverlay.visibility = View.GONE
                Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /** Tampilkan thumbnail foto yang sudah dipilih */
    private fun updatePhotoPreview() {
        llPhotoPreview.removeAllViews()
        tvPhotoCount.text = "${selectedImageUris.size}/5 foto dipilih"

        val size = resources.getDimensionPixelSize(android.R.dimen.thumbnail_height) // ~96dp
        val margin = resources.getDimensionPixelSize(android.R.dimen.app_icon_size)  // ~48dp fallback

        for ((i, uri) in selectedImageUris.withIndex()) {
            val container = FrameLayout(this)
            val lp = LinearLayout.LayoutParams(200, 200)
            lp.marginEnd = 8
            container.layoutParams = lp

            // Thumbnail
            val iv = ImageView(this)
            iv.layoutParams = ViewGroup.LayoutParams(200, 200)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.setImageURI(uri)
            container.addView(iv)

            // Tombol hapus (X) di sudut kanan atas
            val btnRemove = TextView(this)
            btnRemove.text = "✕"
            btnRemove.textSize = 10f
            btnRemove.setTextColor(android.graphics.Color.WHITE)
            btnRemove.setBackgroundColor(0xAACC0000.toInt())
            btnRemove.setPadding(6, 2, 6, 2)
            val removeLp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            removeLp.gravity = android.view.Gravity.TOP or android.view.Gravity.END
            btnRemove.layoutParams = removeLp

            val idx = i
            btnRemove.setOnClickListener {
                selectedImageUris.removeAt(idx)
                updatePhotoPreview()
            }
            container.addView(btnRemove)
            llPhotoPreview.addView(container)
        }
    }
}