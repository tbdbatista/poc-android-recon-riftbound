package com.riftbound.recon.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.riftbound.recon.data.local.AppPreferences
import com.riftbound.recon.domain.model.AuthState
import com.riftbound.recon.domain.model.UserProfile
import com.riftbound.recon.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val appPreferences: AppPreferences
) : AuthRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val currentUser: UserProfile?
        get() = firebaseAuth.currentUser?.toDomainModel(isGuest = false) ?: if (appPreferences.isGuestMode) {
            UserProfile(
                uid = "guest_${android.os.Build.ID}",
                email = null,
                displayName = "Convidado",
                isGuest = true
            )
        } else null

    override val isGuestMode: Boolean
        get() = appPreferences.isGuestMode

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                appPreferences.isGuestMode = false
                _authState.value = AuthState.Authenticated(user.toDomainModel(isGuest = false))
            } else if (appPreferences.isGuestMode) {
                _authState.value = AuthState.Guest
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user was null after sign in.")
            appPreferences.isGuestMode = false
            val profile = user.toDomainModel(isGuest = false)
            _authState.value = AuthState.Authenticated(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user was null after registration.")
            
            if (displayName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
                user.updateProfile(profileUpdates).await()
            }
            
            appPreferences.isGuestMode = false
            val profile = user.toDomainModel(isGuest = false)
            _authState.value = AuthState.Authenticated(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user was null after Google sign in.")
            appPreferences.isGuestMode = false
            val profile = user.toDomainModel(isGuest = false)
            _authState.value = AuthState.Authenticated(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun linkWithGoogle(idToken: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val user = firebaseAuth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("Nenhum usuário conectado para vincular conta."))

        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = user.linkWithCredential(credential).await()
            val updatedUser = authResult.user ?: user
            val profile = updatedUser.toDomainModel(isGuest = false)
            _authState.value = AuthState.Authenticated(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlinkGoogle(): Result<UserProfile> = withContext(Dispatchers.IO) {
        val user = firebaseAuth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("Nenhum usuário conectado."))

        try {
            val updatedUser = user.unlink(GoogleAuthProvider.PROVIDER_ID).await().user ?: user
            val profile = updatedUser.toDomainModel(isGuest = false)
            _authState.value = AuthState.Authenticated(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setGuestMode(enabled: Boolean) = withContext(Dispatchers.IO) {
        appPreferences.isGuestMode = enabled
        if (enabled) {
            if (firebaseAuth.currentUser != null) {
                firebaseAuth.signOut()
            }
            _authState.value = AuthState.Guest
        } else {
            if (firebaseAuth.currentUser == null) {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        appPreferences.isGuestMode = false
        firebaseAuth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.currentUser?.delete()?.await()
            appPreferences.isGuestMode = false
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun FirebaseUser.toDomainModel(isGuest: Boolean): UserProfile {
        val providerIds = providerData.mapNotNull { it.providerId }
        return UserProfile(
            uid = uid,
            email = email,
            displayName = displayName ?: email?.substringBefore("@") ?: "Colecionador",
            photoUrl = photoUrl?.toString(),
            isAnonymous = isAnonymous,
            isGuest = isGuest,
            providers = providerIds
        )
    }
}
