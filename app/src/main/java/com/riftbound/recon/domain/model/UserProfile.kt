package com.riftbound.recon.domain.model

data class UserProfile(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false,
    val isGuest: Boolean = false,
    val providers: List<String> = emptyList()
) {
    val isGoogleLinked: Boolean
        get() = providers.contains("google.com")

    val isPasswordLinked: Boolean
        get() = providers.contains("password")
}
