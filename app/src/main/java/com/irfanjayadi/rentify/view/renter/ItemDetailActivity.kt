package com.irfanjayadi.rentify.view.renter

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Item
import com.irfanjayadi.rentify.view.adapter.ImageSliderAdapter
import de.hdodenhof.circleimageview.CircleImageView
import me.relex.circleindicator.CircleIndicator3
import java.text.NumberFormat
import java.util.Locale
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)

        firestore = FirebaseFirestore.getInstance()

        val itemId = intent.getStringExtra("item_id") ?: run {
            Toast.makeText(this, "ID barang tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Init views
        val vpImages          = findViewById<ViewPager2>(R.id.vpItemImages)
        val imageIndicator    = findViewById<CircleIndicator3>(R.id.imageIndicator)
        val tvTitle           = findViewById<TextView>(R.id.tvDetailTitle)
        val tvStatus          = findViewById<TextView>(R.id.tvDetailStatus)
        val tvCategory        = findViewById<TextView>(R.id.tvDetailCategory)
        val tvPrice           = findViewById<TextView>(R.id.tvDetailPrice)
        val tvStock           = findViewById<TextView>(R.id.tvDetailStock)
        val tvDescription     = findViewById<TextView>(R.id.tvDetailDescription)
        val tvOwnerName       = findViewById<TextView>(R.id.tvOwnerName)
        val tvOwnerLocation   = findViewById<TextView>(R.id.tvOwnerLocation)
        val ivOwnerPhoto      = findViewById<CircleImageView>(R.id.ivOwnerPhoto)
        val btnBack           = findViewById<ImageButton>(R.id.btnBack)
        val btnBookNow        = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBookNow)
        val rvReviews        = findViewById<RecyclerView>(R.id.rvReviews)
        val tvSeeAllReviews  = findViewById<TextView>(R.id.tvSeeAllReviews)
        val tvEmptyReviews   = findViewById<TextView>(R.id.tvEmptyReviews)

        rvReviews.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        btnBack.setOnClickListener { finish() }

        // Load data barang
        firestore.collection("items").document(itemId).get()
            .addOnSuccessListener { doc ->
                val item = doc.toObject(Item::class.java) ?: run {
                    Toast.makeText(this, "Barang tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Isi data ke view
                tvTitle.text    = item.title
                tvCategory.text = item.categoryName
                tvStock.text    = item.stock.toString()
                tvDescription.text = item.description

                val formatted = NumberFormat.getNumberInstance(Locale("id", "ID"))
                    .format(item.pricePerDay)
                tvPrice.text = "Rp $formatted/hari"

                // Status + warna
                tvStatus.text = item.status
                val statusColor = when (item.status.lowercase()) {
                    "tersedia" -> getColor(R.color.green_rentify)
                    "disewa"   -> getColor(android.R.color.holo_orange_dark)
                    else       -> getColor(android.R.color.darker_gray)
                }
                tvStatus.setTextColor(statusColor)

                // Setup galeri foto (ViewPager2)
                if (item.media.isNotEmpty()) {
                    val sliderAdapter = ImageSliderAdapter(item.media)
                    vpImages.adapter = sliderAdapter
                    imageIndicator.setViewPager(vpImages)

                    // Sembunyikan indikator kalau hanya 1 foto
                    imageIndicator.visibility =
                        if (item.media.size > 1) View.VISIBLE else View.GONE
                } else {
                    imageIndicator.visibility = View.GONE
                }

                // Load data pemilik barang
                loadOwnerData(item.ownerId, tvOwnerName, tvOwnerLocation, ivOwnerPhoto)

                // Tombol sewa — nonaktif jika barang tidak tersedia
                if (item.status.equals("Tersedia", ignoreCase = true)) {
                    btnBookNow.isEnabled = true
                    btnBookNow.alpha = 1f
                    btnBookNow.setOnClickListener {
                        // TODO: navigasi ke halaman booking
                        Toast.makeText(this, "Fitur booking segera hadir!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    btnBookNow.isEnabled = false
                    btnBookNow.alpha = 0.5f
                    btnBookNow.text = "Sedang Disewa"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat detail: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadOwnerData(
        ownerId: String,
        tvName: TextView,
        tvLocation: TextView,
        ivPhoto: CircleImageView
    ) {
        if (ownerId.isEmpty()) return

        firestore.collection("users").document(ownerId).get()
            .addOnSuccessListener { doc ->
                tvName.text     = doc.getString("name")    ?: "Pemilik"
                tvLocation.text = doc.getString("address") ?: "Lokasi tidak tersedia"

                val photoUrl = doc.getString("profilePhotoUrl") ?: ""
                if (photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(ivPhoto)
                }
            }

    }

    private fun loadReviews(itemId: String, rv: RecyclerView, tvEmpty: TextView, tvSeeAll: TextView) {
        // Asumsi field relasi di tabel reviews adalah 'itemId' dan tanggal adalah 'createdAt'
        firestore.collection("reviews")
            .whereEqualTo("itemId", itemId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(4) // Ambil 4 untuk mengecek apakah komentarnya lebih dari 3
            .get()
            .addOnSuccessListener { snapshot ->
                val reviews = snapshot.toObjects(com.irfanjayadi.rentify.model.entity.Review::class.java)

                if (reviews.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                    tvSeeAll.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rv.visibility = View.VISIBLE

                    // Potong menjadi maksimal 3 saja untuk ditampilkan di halaman ini
                    val displayReviews = if (reviews.size > 3) reviews.take(3) else reviews

                    // Jika ada lebih dari 3, munculkan tombol "Lihat Semua"
                    tvSeeAll.visibility = if (reviews.size > 3) View.VISIBLE else View.GONE

                    val reviewAdapter = com.irfanjayadi.rentify.view.adapter.ReviewAdapter(displayReviews)
                    rv.adapter = reviewAdapter
                }
            }
            .addOnFailureListener {
                tvEmpty.text = "Gagal memuat ulasan."
                tvEmpty.visibility = View.VISIBLE
            }
    }
}