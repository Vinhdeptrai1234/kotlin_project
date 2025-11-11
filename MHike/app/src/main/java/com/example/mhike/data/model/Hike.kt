package com.example.mhike.data.model

data class Hike(
    val id: Long? = null,
    val userId: Long,                 // NEW: chủ sở hữu
    val name: String,
    val location: String,
    val hikeDateEpoch: Long,
    val parking: Boolean,
    val lengthKm: Double,
    val difficulty: String,
    val description: String? = null,
    val elevationGainM: Int? = null,
    val maxGroupSize: Int? = null,
    val coverImage: String? = null    // NEW: ảnh bìa (uri string)
)
