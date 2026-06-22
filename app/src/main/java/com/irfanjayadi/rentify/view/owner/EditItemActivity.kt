package com.irfanjayadi.rentify.view.owner

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R

class EditItemActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private var itemId: String = ""

    // URL foto yang sudah ada di Firestore
    private val existingImageUrls = mutableListOf<String>()
    // URI foto baru yang dipilih user
    private val newImageUris = mutableListOf<Uri>()
    // URL final (existing + new yang di-upload)
    private val finalImageUrls = mutableListOf<String>()

    private lateinit var llPhotoPreview: LinearLayout
    private lateinit var tvPhotoCount: TextView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvLoadingText: TextView

    private val pickMultipleImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                val totalCurrent = existingImageUrls.size + newImageUris.size
                val toAdd = uris.take(5 - totalCurrent)
                newImageUris.addAll(toAdd)
                updatePhotoPreview()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item) // Reuse layout yang sama

        try { MediaManager.init(applicationContext, mapOf("cloud_name" to "dalwy5um8")) }
        catch (e: Exception) {}

        firestore    = FirebaseFirestore.getInstance()
        itemId       = intent.getStringExtra("item_id") ?: ""

        llPhotoPreview = findViewById(R.id.llPhotoPreview)
        tvPhotoCount   = findViewById(R.id.tvPhotoCount)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        tvLoadingText  = findViewById(R.id.tvLoadingText)

        val etTitle       = findViewById<TextInputEditText>(R.id.etTitle)
        val etDescription = findViewById<TextInputEditText>(R.id.etDescription)
        val etPrice       = findViewById<TextInputEditText>(R.id.etPrice)
        val etStock       = findViewById<TextInputEditText>(R.id.etStock)
        val etCategory    = findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.etCategoryDropdown)
        val btnSave       = findViewById<MaterialButton>(R.id.btnSaveItem)

        btnSave.text = "Simpan Perubahan"

        // Load data item yang ada
        if (itemId.isNotEmpty()) {
            firestore.collection("items").document(itemId).get()
                .addOnSuccessListener { doc ->
                    etTitle.setText(doc.getString("title"))
                    etDescription.setText(doc.getString("description"))
                    etPrice.setText(doc.getDouble("price_per_day")?.toLong()?.toString() ?: "0")
                    etStock.setText(doc.getLong("stock")?.toString() ?: "1")
                    etCategory.setText(doc.getString("category_name"), false)

                    @Suppress("UNCHECKED_CAST")
                    val media = doc.get("media") as? List<String> ?: emptyList()
                    existingImageUrls.addAll(media)
                    updatePhotoPreview()
                }
        }

        // Load kategori
        firestore.collection("categories").get().addOnSuccessListener { snapshot ->
            val list = snapshot.documents.map { it.getString("name") ?: "" }
            etCategory.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, list)
            )
        }

        findViewById<MaterialButton>(R.id.btnPickPhoto).setOnClickListener {
            val total = existingImageUrls.size + newImageUris.size
            if (total >= 5) Toast.makeText(this, "Maksimal 5 foto", Toast.LENGTH_SHORT).show()
            else pickMultipleImages.launch("image/*")
        }

        btnSave.setOnClickListener {
            val title       = etTitle.text.toString().trim()
            val category    = etCategory.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val priceStr    = etPrice.text.toString().trim()
            val stockStr    = etStock.text.toString().trim()

            if (title.isEmpty() || category.isEmpty() || description.isEmpty()
                || priceStr.isEmpty() || stockStr.isEmpty()) {
                Toast.makeText(this, "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (existingImageUrls.isEmpty() && newImageUris.isEmpty()) {
                Toast.makeText(this, "Tambahkan minimal 1 foto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadingOverlay.visibility = View.VISIBLE
            finalImageUrls.clear()
            finalImageUrls.addAll(existingImageUrls) // Foto lama tetap dipakai

            val price = priceStr.toDoubleOrNull() ?: 0.0
            val stock = stockStr.toIntOrNull() ?: 1

            if (newImageUris.isEmpty()) {
                // Tidak ada foto baru, langsung update
                updateFirestore(title, category, description, price, stock)
            } else {
                // Upload foto baru dulu, baru update
                uploadNewImages(0, title, category, description, price, stock)
            }
        }
    }

    private fun uploadNewImages(
        index: Int,
        title: String, category: String, description: String,
        price: Double, stock: Int
    ) {
        if (index >= newImageUris.size) {
            tvLoadingText.text = "Menyimpan perubahan..."
            updateFirestore(title, category, description, price, stock)
            return
        }

        val uri = newImageUris[index]
        val total = newImageUris.size
        tvLoadingText.text = "Mengunggah foto baru ${index + 1}/$total..."

        MediaManager.get().upload(uri)
            .unsigned("g1tohcnv")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val pct = (bytes.toDouble() / totalBytes * 100).toInt()
                    tvLoadingText.text = "Mengunggah foto ${index + 1}/$total ($pct%)..."
                }
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    finalImageUrls.add(resultData["secure_url"] as String)
                    uploadNewImages(index + 1, title, category, description, price, stock)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this@EditItemActivity, "Gagal upload foto: ${error.description}", Toast.LENGTH_LONG).show()
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    private fun updateFirestore(
        title: String, category: String, description: String,
        price: Double, stock: Int
    ) {
        val updates = mapOf(
            "title"         to title,
            "category_name" to category,
            "description"   to description,
            "price_per_day" to price,
            "stock"         to stock,
            "media"         to finalImageUrls
        )
        firestore.collection("items").document(itemId).update(updates)
            .addOnSuccessListener {
                loadingOverlay.visibility = View.GONE
                Toast.makeText(this, "Barang berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                loadingOverlay.visibility = View.GONE
                Toast.makeText(this, "Gagal memperbarui: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updatePhotoPreview() {
        llPhotoPreview.removeAllViews()
        val total = existingImageUrls.size + newImageUris.size
        tvPhotoCount.text = "$total/5 foto"

        // Tampilkan foto existing (dari URL)
        for ((i, url) in existingImageUrls.withIndex()) {
            val container = FrameLayout(this)
            container.layoutParams = LinearLayout.LayoutParams(200, 200).apply { marginEnd = 8 }

            val iv = ImageView(this).apply {
                layoutParams = ViewGroup.LayoutParams(200, 200)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            Glide.with(this).load(url).centerCrop().into(iv)
            container.addView(iv)

            val btnRemove = buildRemoveButton()
            val idx = i
            btnRemove.setOnClickListener {
                existingImageUrls.removeAt(idx)
                updatePhotoPreview()
            }
            container.addView(btnRemove)
            llPhotoPreview.addView(container)
        }

        // Tampilkan foto baru (dari URI)
        for ((i, uri) in newImageUris.withIndex()) {
            val container = FrameLayout(this)
            container.layoutParams = LinearLayout.LayoutParams(200, 200).apply { marginEnd = 8 }

            val iv = ImageView(this).apply {
                layoutParams = ViewGroup.LayoutParams(200, 200)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
            }
            container.addView(iv)

            val btnRemove = buildRemoveButton()
            val idx = i
            btnRemove.setOnClickListener {
                newImageUris.removeAt(idx)
                updatePhotoPreview()
            }
            container.addView(btnRemove)
            llPhotoPreview.addView(container)
        }
    }

    private fun buildRemoveButton(): TextView {
        val btn = TextView(this)
        btn.text = "✕"
        btn.textSize = 10f
        btn.setTextColor(android.graphics.Color.WHITE)
        btn.setBackgroundColor(0xAACC0000.toInt())
        btn.setPadding(6, 2, 6, 2)
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = android.view.Gravity.TOP or android.view.Gravity.END
        btn.layoutParams = lp
        return btn
    }
}