package com.riftbound.recon.domain.model

data class UserProfile(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false,
    val isGuest: Boolean = false
)
