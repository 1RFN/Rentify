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
import com.google.android.material.switchmaterial.SwitchMaterial
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
    private lateinit var tvSwitchRoleTitle: TextView
    private lateinit var tvSwitchRoleSubtitle: TextView
    private lateinit var switchRole: SwitchMaterial

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth      = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        tvProfileName       = view.findViewById(R.id.tvProfileName)
        ivProfilePicture    = view.findViewById(R.id.ivProfilePicture)
        tvSwitchRoleTitle   = view.findViewById(R.id.tvSwitchRoleTitle)
        tvSwitchRoleSubtitle = view.findViewById(R.id.tvSwitchRoleSubtitle)
        switchRole          = view.findViewById(R.id.switchRole)

        val btnLogout         = view.findViewById<LinearLayout>(R.id.btnLogout)
        val btnEditProfile    = view.findViewById<View>(R.id.btnEditProfile)
        val btnEditProfileMenu = view.findViewById<View>(R.id.btnEditProfileMenu)
        val btnSecurity       = view.findViewById<View>(R.id.btnSecurityAccount)
        val btnSwitchRole     = view.findViewById<View>(R.id.btnSwitchRole)

        updateSwitchRoleUI("renter")

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        btnEditProfileMenu.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        btnSecurity.setOnClickListener {
            startActivity(Intent(requireContext(), SecurityActivity::class.java))
        }

        btnSwitchRole.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (!isAdded) return@addOnSuccessListener
                    val currentRole = doc.getString("role") ?: "renter"
                    val targetRole = if (currentRole == "renter") "owner" else "renter"

                    if (targetRole == "owner") {
                        val name    = doc.getString("name")    ?: ""
                        val phone   = doc.getString("phone")   ?: ""
                        val address = doc.getString("address") ?: ""
                        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                            showIncompleteProfileAlert()
                            return@addOnSuccessListener
                        }
                    }
                    switchToRole(userId, targetRole)
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

                val role = document.getString("role") ?: "renter"
                updateSwitchRoleUI(role)
            }
    }

    private fun updateSwitchRoleUI(currentRole: String) {
        if (!isAdded) return
        if (currentRole == "renter") {
            tvSwitchRoleTitle.text = "Ganti ke Mode Pemilik"
            tvSwitchRoleSubtitle.text = "Kelola dan sewakan properti Anda"
            switchRole.isChecked = false
        } else {
            tvSwitchRoleTitle.text = "Ganti ke Mode Penyewa"
            tvSwitchRoleSubtitle.text = "Cari dan sewa barang"
            switchRole.isChecked = true
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
