package com.irfanjayadi.rentify.view.renter

import android.os.Bundle
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.model.entity.Item
import com.irfanjayadi.rentify.model.entity.Review

class AddReviewActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_review)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val itemId = intent.getStringExtra("item_id") ?: return finish()

        val ratingBar = findViewById<RatingBar>(R.id.ratingBarInput)
        val etComment = findViewById<EditText>(R.id.etReviewComment)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnSubmitReview)

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            val comment = etComment.text.toString().trim()

            if (rating == 0f) {
                Toast.makeText(this, "Silakan berikan bintang", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitReview(itemId, rating, comment, btnSubmit)
        }
    }

    private fun submitReview(itemId: String, rating: Float, comment: String, btn: MaterialButton) {
        btn.isEnabled = false
        btn.text = "Mengirim..."

        val userId = auth.currentUser?.uid ?: return

        // Ambil data Renter untuk disimpan di Review
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val renterName = userDoc.getString("name") ?: "Penyewa"
                val renterPhoto = userDoc.getString("profilePhotoUrl") ?: ""

                val reviewRef = firestore.collection("reviews").document()
                val reviewData = Review(
                    reviewId = reviewRef.id,
                    itemId = itemId,
                    renterId = userId,
                    renterName = renterName,
                    renterPhotoUrl = renterPhoto,
                    rating = rating,
                    comment = comment,
                    createdAt = System.currentTimeMillis()
                )

                // Simpan ulasan ke tabel "reviews"
                reviewRef.set(reviewData).addOnSuccessListener {
                    updateItemRating(itemId, rating)
                }.addOnFailureListener {
                    btn.isEnabled = true
                    btn.text = "Kirim Ulasan"
                    Toast.makeText(this, "Gagal mengirim ulasan", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateItemRating(itemId: String, newRating: Float) {
        val itemRef = firestore.collection("items").document(itemId)

        // Gunakan Transaction Firestore agar penghitungan rata-rata rating akurat
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(itemRef)
            val item = snapshot.toObject(Item::class.java)

            if (item != null) {
                val currentRating = item.rating
                val currentCount = item.reviewCount

                // Rumus Rata-rata Rating Baru
                val newCount = currentCount + 1
                val calculatedRating = ((currentRating * currentCount) + newRating) / newCount

                transaction.update(itemRef, "rating", calculatedRating)
                transaction.update(itemRef, "review_count", newCount)
            }
        }.addOnSuccessListener {
            Toast.makeText(this, "Ulasan berhasil dikirim!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal memperbarui rating barang", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}