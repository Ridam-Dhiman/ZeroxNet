package com.example.data.model

data class UserProfile(
    val deviceId: String,
    val displayName: String,
    val isEmergencyMode: Boolean = false
)
