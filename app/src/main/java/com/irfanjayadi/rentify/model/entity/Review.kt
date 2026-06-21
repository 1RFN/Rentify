package com.irfanjayadi.rentify.model.entity

data class Review(
    val reviewId: String = "",
    val itemId: String = "",
    val renterId: String = "",
    val renterName: String = "",
    val renterPhotoUrl: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Long = 0L
)