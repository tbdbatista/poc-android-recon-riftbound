package com.riftbound.recon.domain.model

sealed interface AuthState {
    data object Initializing : AuthState
    data object Unauthenticated : AuthState
    data object Guest : AuthState
    data class Authenticated(val user: UserProfile) : AuthState
}
