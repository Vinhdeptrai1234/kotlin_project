package com.example.mhike.data.model

data class Observation(
    val id: Long? = null,
    val hikeId: Long,         // FK -> hikes.id
    val observedAt: Long,     // millis since epoch, map từ cột observed_at
    val note: String,         // NOT NULL
    val comments: String? = null
)
