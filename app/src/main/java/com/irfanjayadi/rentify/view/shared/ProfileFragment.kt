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
    private lateinit var btnModeRenter: LinearLayout
    private lateinit var btnModeOwner: LinearLayout

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
        btnModeRenter    = view.findViewById(R.id.btnModeRenter)
        btnModeOwner     = view.findViewById(R.id.btnModeOwner)

        val btnLogout      = view.findViewById<LinearLayout>(R.id.btnLogout)
        val btnEditProfile = view.findViewById<View>(R.id.btnEditProfile)
        val btnSecurity    = view.findViewById<View>(R.id.btnSecurityAccount)

        // Set tampilan default ke renter SEBELUM Firestore selesai loading
        // Ini mencegah tampilan salah saat fragment pertama dibuka
        updateModeButtonsUI("renter")

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        // Tombol Keamanan Akun
        btnSecurity.setOnClickListener {
            startActivity(Intent(requireContext(), SecurityActivity::class.java))
        }

        // Tombol Mode Penyewa â†' beralih ke renter
        btnModeRenter.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    val currentRole = doc.getString("role") ?: "renter"
                    if (currentRole == "renter") {
                        Toast.makeText(
                            requireContext(),
                            "Anda sudah berada di Mode Penyewa",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        switchToRole(userId, "renter")
                    }
                }
        }

        // Tombol Mode Pemilik â†' beralih ke owner
        btnModeOwner.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    val currentRole = doc.getString("role") ?: "renter"
                    if (currentRole == "owner") {
                        Toast.makeText(
                            requireContext(),
                            "Anda sudah berada di Mode Pemilik",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val name    = doc.getString("name")    ?: ""
                        val phone   = doc.getString("phone")   ?: ""
                        val address = doc.getString("address") ?: ""
                        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                            showIncompleteProfileAlert()
                        } else {
                            switchToRole(userId, "owner")
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
                if (photoUrl.isNotEmpty()) loadProfileImage(photoUrl)
                else ivProfilePicture.setImageResource(R.drawable.ic_profile)

                // Update tombol sesuai role yang tersimpan di Firestore
                val role = document.getString("role") ?: "renter"
                updateModeButtonsUI(role)
            }
    }

    private fun updateModeButtonsUI(currentRole: String) {
        if (!isAdded) return
        if (currentRole == "renter") {
            btnModeRenter.setBackgroundResource(R.drawable.bg_mode_selected)
            btnModeOwner.setBackgroundResource(R.drawable.bg_mode_unselected)
        } else {
            btnModeRenter.setBackgroundResource(R.drawable.bg_mode_unselected)
            btnModeOwner.setBackgroundResource(R.drawable.bg_mode_selected)
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

    private fun switchToRole(userId: String, targetRole: String) {
        firestore.collection("users").document(userId)
            .update("role", targetRole)
            .addOnSuccessListener {
                val ctx = context ?: return@addOnSuccessListener
                if (!isAdded) return@addOnSuccessListener

                if (targetRole == "owner") {
                    Toast.makeText(ctx, "Beralih ke Mode Pemilik", Toast.LENGTH_SHORT).show()
                    val intent = Intent(
                        ctx,
                        com.irfanjayadi.rentify.view.owner.DashboardOwnerActivity::class.java
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                } else {
                    Toast.makeText(ctx, "Beralih ke Mode Penyewa", Toast.LENGTH_SHORT).show()
                    val intent = Intent(
                        ctx,
                        com.irfanjayadi.rentify.view.renter.HomeRenterActivity::class.java
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    activity?.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
            .addOnFailureListener { e ->
                val ctx = context ?: return@addOnFailureListener
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(ctx, "Gagal beralih: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya, Keluar") { _, _ ->
                auth.signOut()
                startActivity(
                    Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
