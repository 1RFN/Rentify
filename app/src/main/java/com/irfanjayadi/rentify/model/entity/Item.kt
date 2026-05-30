package com.irfanjayadi.rentify.model.entity

import com.google.firebase.firestore.PropertyName


data class Item(
    @get:PropertyName("item_id") @set:PropertyName("item_id") var itemId: String = "",
    @get:PropertyName("owner_id") @set:PropertyName("owner_id") var ownerId: String = "",
    var title: String = "",
    var description: String = "",
    @get:PropertyName("category_name") @set:PropertyName("category_name") var categoryName: String = "",
    @get:PropertyName("price_per_day") @set:PropertyName("price_per_day") var pricePerDay: Double = 0.0,
    var stock: Int = 0,
    var status: String = "Tersedia",
    var media: List<String> = emptyList()
)