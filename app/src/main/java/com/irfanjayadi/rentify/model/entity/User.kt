package com.irfanjayadi.rentify.model.entity

import com.google.firebase.firestore.PropertyName

data class User(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var address: String = "",
    var role: String = "",
    @get:PropertyName("profile_photo_url")
    @set:PropertyName("profile_photo_url")
    var profilePhotoUrl: String = ""
)