package com.irfanjayadi.rentify.view.renter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Item
import com.irfanjayadi.rentify.model.entity.Transaction
import com.irfanjayadi.rentify.view.adapter.ImageSliderAdapter
import com.irfanjayadi.rentify.view.adapter.ReviewAdapter
import de.hdodenhof.circleimageview.CircleImageView
import me.relex.circleindicator.CircleIndicator3
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Variabel state untuk menyimpan data saat ini
    private var currentItem: Item? = null
    private var ownerPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val itemId = intent.getStringExtra("item_id") ?: run {
            Toast.makeText(this, "ID barang tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews(itemId)
    }

    private fun initViews(itemId: String) {
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
        val btnBookNow        = findViewById<MaterialButton>(R.id.btnBookNow)
        val rvReviews         = findViewById<RecyclerView>(R.id.rvReviews)
        val tvSeeAllReviews   = findViewById<TextView>(R.id.tvSeeAllReviews)
        val tvEmptyReviews    = findViewById<TextView>(R.id.tvEmptyReviews)
        val fabWhatsApp       = findViewById<FloatingActionButton>(R.id.fabWhatsApp)

        rvReviews.layoutManager = LinearLayoutManager(this)
        btnBack.setOnClickListener { finish() }

        // Fitur Chat WhatsApp
        fabWhatsApp.setOnClickListener {
            openWhatsAppChat()
        }

        // Load data barang
        firestore.collection("items").document(itemId).get()
            .addOnSuccessListener { doc ->
                currentItem = doc.toObject(Item::class.java)

                if (currentItem == null) {
                    Toast.makeText(this, "Barang tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                tvTitle.text = currentItem!!.title
                tvCategory.text = currentItem!!.categoryName
                tvStock.text = currentItem!!.stock.toString()
                tvDescription.text = currentItem!!.description

                val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(currentItem!!.pricePerDay)
                tvPrice.text = "Rp $formatted/hari"

                tvStatus.text = currentItem!!.status
                val statusColor = when (currentItem!!.status.lowercase()) {
                    "tersedia" -> getColor(R.color.green_rentify)
                    "disewa"   -> getColor(android.R.color.holo_orange_dark)
                    else       -> getColor(android.R.color.darker_gray)
                }
                tvStatus.setTextColor(statusColor)

                if (currentItem!!.media.isNotEmpty()) {
                    vpImages.adapter = ImageSliderAdapter(currentItem!!.media)
                    imageIndicator.setViewPager(vpImages)
                    imageIndicator.visibility = if (currentItem!!.media.size > 1) View.VISIBLE else View.GONE
                } else {
                    imageIndicator.visibility = View.GONE
                }

                loadOwnerData(currentItem!!.ownerId, tvOwnerName, tvOwnerLocation, ivOwnerPhoto)

                // Logika Tombol Sewa
                val currentUserUid = auth.currentUser?.uid
                if (currentItem!!.status.equals("Tersedia", ignoreCase = true)) {
                    if (currentItem!!.ownerId == currentUserUid) {
                        btnBookNow.isEnabled = false
                        btnBookNow.alpha = 0.5f
                        btnBookNow.text = "Ini Barang Anda"
                    } else {
                        btnBookNow.isEnabled = true
                        btnBookNow.alpha = 1f
                        btnBookNow.setOnClickListener { showDatePicker() }
                    }
                } else {
                    btnBookNow.isEnabled = false
                    btnBookNow.alpha = 0.5f
                    btnBookNow.text = "Sedang Disewa"
                }
            }

        loadReviews(itemId, rvReviews, tvEmptyReviews, tvSeeAllReviews)
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Pilih Tanggal Sewa")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDateMillis = selection.first
            val endDateMillis = selection.second

            // Hitung durasi hari (Minimal 1 hari jika sewa dan kembali di hari yang sama)
            val diffMillis = endDateMillis - startDateMillis
            val days = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt() + 1

            val totalPrice = days * (currentItem?.pricePerDay ?: 0.0)

            showConfirmationDialog(startDateMillis, endDateMillis, days, totalPrice)
        }
        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun showConfirmationDialog(startMillis: Long, endMillis: Long, days: Int, totalPrice: Double) {
        val formattedTotal = NumberFormat.getNumberInstance(Locale("id", "ID")).format(totalPrice)

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Sewa")
            .setMessage("Anda akan menyewa '${currentItem?.title}' selama $days hari.\n\nTotal Biaya: Rp $formattedTotal\nLanjutkan pemesanan?")
            .setPositiveButton("Ya, Sewa") { _, _ ->
                processBooking(startMillis, endMillis, totalPrice)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun processBooking(startMillis: Long, endMillis: Long, totalPrice: Double) {
        val renterId = auth.currentUser?.uid ?: return
        val ownerId = currentItem?.ownerId ?: return
        val itemId = currentItem?.itemId ?: return

        val transactionRef = firestore.collection("transactions").document()

        val transaction = Transaction(
            transactionId = transactionRef.id,
            itemId = itemId,
            renterId = renterId,
            ownerId = ownerId,
            startDate = Timestamp(Date(startMillis)),
            endDate = Timestamp(Date(endMillis)),
            totalPrice = totalPrice,
            status = "Menunggu Konfirmasi"
        )

        transactionRef.set(transaction)
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil! Menunggu konfirmasi pemilik.", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal membuat pesanan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadOwnerData(ownerId: String, tvName: TextView, tvLocation: TextView, ivPhoto: CircleImageView) {
        if (ownerId.isEmpty()) return

        firestore.collection("users").document(ownerId).get()
            .addOnSuccessListener { doc ->
                tvName.text = doc.getString("name") ?: "Pemilik"
                tvLocation.text = doc.getString("address") ?: "Lokasi tidak tersedia"

                // Simpan nomor HP pemilik untuk fitur WhatsApp
                ownerPhone = doc.getString("phone") ?: ""

                val photoUrl = doc.getString("profilePhotoUrl") ?: ""
                if (photoUrl.isNotEmpty()) {
                    Glide.with(this).load(photoUrl).placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(ivPhoto)
                }
            }
    }

    private fun openWhatsAppChat() {
        if (ownerPhone.isEmpty()) {
            Toast.makeText(this, "Nomor pemilik belum tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        // Format nomor dari 08xx ke 628xx
        var formattedPhone = ownerPhone.trim()
        if (formattedPhone.startsWith("08")) {
            formattedPhone = "628" + formattedPhone.substring(2)
        } else if (formattedPhone.startsWith("+62")) {
            formattedPhone = "62" + formattedPhone.substring(3)
        }

        val itemName = currentItem?.title ?: "barang ini"
        val message = "Halo, saya tertarik untuk menyewa '$itemName' yang Anda iklankan di Rentify."

        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka WhatsApp. Pastikan aplikasi WhatsApp terinstal.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadReviews(itemId: String, rv: RecyclerView, tvEmpty: TextView, tvSeeAll: TextView) {
        firestore.collection("reviews")
            .whereEqualTo("itemId", itemId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(4)
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
                    val displayReviews = if (reviews.size > 3) reviews.take(3) else reviews
                    tvSeeAll.visibility = if (reviews.size > 3) View.VISIBLE else View.GONE
                    rv.adapter = ReviewAdapter(displayReviews)
                }
            }
    }
}