package com.irfanjayadi.rentify.model.entity

data class Review(
    var reviewId: String = "",
    var itemId: String = "",
    var renterId: String = "",
    var renterName: String = "",
    var renterPhotoUrl: String = "",
    var rating: Float = 0f,
    var comment: String = "",
    var createdAt: Long = 0L
)