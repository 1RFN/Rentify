package com.irfanjayadi.rentify.view.shared

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.irfanjayadi.rentify.R
import com.irfanjayadi.rentify.view.auth.LoginActivity
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvProfileName: TextView
    private lateinit var ivProfilePicture: CircleImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth      = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        tvProfileName    = view.findViewById(R.id.tvProfileName)
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture)

        val btnModeOwner   = view.findViewById<LinearLayout>(R.id.btnModeOwner)
        val btnLogout      = view.findViewById<LinearLayout>(R.id.btnLogout)
        val btnEditProfile = view.findViewById<View>(R.id.btnEditProfile)

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        btnModeOwner.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name    = document.getString("name")    ?: ""
                        val phone   = document.getString("phone")   ?: ""
                        val address = document.getString("address") ?: ""
                        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                            showIncompleteProfileAlert()
                        } else {
                            switchToOwnerMode(userId)
                        }
                    }
                }
        }

        btnLogout.setOnClickListener { showLogoutConfirmation() }

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchUserProfileData()
    }

    private fun fetchUserProfileData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document == null || !document.exists() || !isAdded) return@addOnSuccessListener

                tvProfileName.text = document.getString("name") ?: "Pengguna"

                val photoUrl = document.getString("profilePhotoUrl") ?: ""
                if (photoUrl.isNotEmpty()) {
                    loadProfileImage(photoUrl)
                } else {
                    ivProfilePicture.setImageResource(R.drawable.ic_profile)
                }
            }
    }

    private fun loadProfileImage(url: String) {
        if (!isAdded) return

        Glide.with(requireContext())
            .load(url)
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(ivProfilePicture)
    }

    private fun showIncompleteProfileAlert() {
        AlertDialog.Builder(requireContext())
            .setTitle("Profil Belum Lengkap")
            .setMessage("Untuk menjaga keamanan transaksi, Anda harus melengkapi data profil sebelum dapat menyewakan barang.")
            .setPositiveButton("Mengerti") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun switchToOwnerMode(userId: String) {
        firestore.collection("users").document(userId)
            .update("role", "owner")
            .addOnSuccessListener {
                Toast.makeText(context, "Berhasil beralih ke Mode Pemilik!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), com.irfanjayadi.rentify.view.owner.DashboardOwnerActivity::class.java))
                activity?.finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal beralih mode: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            .setPositiveButton("Ya, Keluar") { _, _ ->
                auth.signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}