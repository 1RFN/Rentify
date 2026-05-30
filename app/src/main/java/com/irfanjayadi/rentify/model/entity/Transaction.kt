package com.irfanjayadi.rentify.model.entity

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Transaction(
    @get:PropertyName("transaction_id") @set:PropertyName("transaction_id") var transactionId: String = "",
    @get:PropertyName("item_id") @set:PropertyName("item_id") var itemId: String = "",
    @get:PropertyName("renter_id") @set:PropertyName("renter_id") var renterId: String = "",
    @get:PropertyName("owner_id") @set:PropertyName("owner_id") var ownerId: String = "",
    @get:PropertyName("start_date") @set:PropertyName("start_date") var startDate: Timestamp? = null,
    @get:PropertyName("end_date") @set:PropertyName("end_date") var endDate: Timestamp? = null,
    @get:PropertyName("total_price") @set:PropertyName("total_price") var totalPrice: Double = 0.0,
    var status: String = "Menunggu Konfirmasi"
)