package com.example.mhike.data.model

data class User(
    val id: Long? = null,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val avatar: String? = null
)
