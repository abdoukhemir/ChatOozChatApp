package com.codewithfk.chatooz_android

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    // Required empty constructor for Firebase
    constructor() : this("", "", "", "", 0)
}