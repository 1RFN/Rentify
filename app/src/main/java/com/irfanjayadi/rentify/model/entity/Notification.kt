package com.irfanjayadi.rentify.model.entity

import com.google.firebase.firestore.PropertyName

data class Notification(
    @get:PropertyName("notification_id") @set:PropertyName("notification_id")
    var notificationId: String = "",

    // UBAH BAGIAN INI MENJADI user_id
    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",

    var title: String = "",

    @get:PropertyName("massage") @set:PropertyName("massage")
    var massage: String = "", // Tetap biarkan jika di database masih "massage"

    var type: String = "",

    @get:PropertyName("is_read") @set:PropertyName("is_read")
    var isRead: Boolean = false,

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Long = 0L
)