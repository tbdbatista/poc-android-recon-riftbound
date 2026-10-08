package com.riftbound.recon.domain.repository

import com.riftbound.recon.domain.model.AuthState
import com.riftbound.recon.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentUser: UserProfile?
    val isGuestMode: Boolean

    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<UserProfile>
    suspend fun signInWithGoogle(idToken: String): Result<UserProfile>
    suspend fun setGuestMode(enabled: Boolean)
    suspend fun signOut()
    suspend fun deleteAccount(): Result<Unit>
}
