package com.irfanjayadi.rentify.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.irfanjayadi.rentify.R

class CategoryAdapter(
    private var categories: List<String>,
    private var selectedCategory: String = "Semua", // Default yang terpilih
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategoryName.text = category

        // Cek apakah item ini adalah kategori yang sedang dipilih
        val isSelected = category == selectedCategory

        // Ubah tampilan berdasarkan status isSelected
        if (isSelected) {
            holder.tvCategoryName.setBackgroundResource(R.drawable.bg_mode_selected)
            holder.tvCategoryName.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.white)
            )
        } else {
            holder.tvCategoryName.setBackgroundResource(R.drawable.bg_mode_unselected)
            holder.tvCategoryName.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.black) // Sesuaikan jika ada warna text abu-abu
            )
        }

        // Aksi ketika kategori ditekan
        holder.itemView.setOnClickListener {
            // Jangan lakukan apa-apa jika menekan kategori yang sama
            if (category != selectedCategory) {
                val previousCategory = selectedCategory
                selectedCategory = category

                // Refresh tampilan item yang berubah statusnya saja agar lebih ringan
                notifyItemChanged(categories.indexOf(previousCategory))
                notifyItemChanged(position)

                // Kirim data kategori yang diklik ke Fragment
                onCategoryClick(category)
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    // Fungsi untuk memperbarui data dari Fragment
    fun updateData(newCategories: List<String>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    // Fungsi untuk mengatur kategori yang aktif dari luar (misal dari lemparan HomeFragment)
    fun setSelectedCategory(category: String) {
        val previousIndex = categories.indexOf(selectedCategory)
        selectedCategory = category
        val newIndex = categories.indexOf(selectedCategory)

        if (previousIndex != -1) notifyItemChanged(previousIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }
}