package com.riftbound.recon.ui.auth

import com.riftbound.recon.domain.model.AuthState
import com.riftbound.recon.domain.model.UserProfile
import com.riftbound.recon.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_isLoginMode_andNotLoading() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSignUpMode)
        assertNull(state.errorMessage)
        assertFalse(state.showGuestDisclaimerDialog)
    }

    @Test
    fun toggleAuthMode_switchesBetweenLoginAndSignUp() {
        viewModel.toggleAuthMode()
        assertTrue(viewModel.uiState.value.isSignUpMode)

        viewModel.toggleAuthMode()
        assertFalse(viewModel.uiState.value.isSignUpMode)
    }

    @Test
    fun signInWithEmail_withEmptyCredentials_showsErrorMessage() {
        viewModel.signInWithEmail("", "")
        assertEquals("Por favor, preencha o email e a senha.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signUpWithEmail_withShortPassword_showsErrorMessage() {
        viewModel.signUpWithEmail("test@email.com", "123", "Test User")
        assertEquals("A senha deve ter pelo menos 6 caracteres.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun guestModeFlow_requestShowsDialog_andConfirmSetsGuestState() = runTest {
        viewModel.requestGuestMode()
        assertTrue(viewModel.uiState.value.showGuestDisclaimerDialog)

        viewModel.confirmGuestMode()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showGuestDisclaimerDialog)
        assertTrue(fakeAuthRepository.isGuestMode)
        assertEquals(AuthState.Guest, fakeAuthRepository.authState.value)
    }

    @Test
    fun signInWithEmail_successful_updatesAuthState() = runTest {
        viewModel.signInWithEmail("user@test.com", "password123")
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        val auth = viewModel.authState.value
        assertTrue(auth is AuthState.Authenticated)
        assertEquals("user@test.com", (auth as AuthState.Authenticated).user.email)
    }
}

private class FakeAuthRepository : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState

    override var currentUser: UserProfile? = null
    override var isGuestMode: Boolean = false

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> {
        val profile = UserProfile(uid = "fake_123", email = email, displayName = "Test User")
        currentUser = profile
        isGuestMode = false
        _authState.value = AuthState.Authenticated(profile)
        return Result.success(profile)
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<UserProfile> {
        val profile = UserProfile(uid = "fake_123", email = email, displayName = displayName)
        currentUser = profile
        isGuestMode = false
        _authState.value = AuthState.Authenticated(profile)
        return Result.success(profile)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> {
        val profile = UserProfile(uid = "fake_google_123", email = "google@test.com", displayName = "Google User")
        currentUser = profile
        isGuestMode = false
        _authState.value = AuthState.Authenticated(profile)
        return Result.success(profile)
    }

    override suspend fun setGuestMode(enabled: Boolean) {
        isGuestMode = enabled
        if (enabled) {
            currentUser = UserProfile(uid = "guest", email = null, displayName = "Convidado", isGuest = true)
            _authState.value = AuthState.Guest
        } else {
            currentUser = null
            _authState.value = AuthState.Unauthenticated
        }
    }

    override suspend fun signOut() {
        isGuestMode = false
        currentUser = null
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun deleteAccount(): Result<Unit> {
        signOut()
        return Result.success(Unit)
    }
}
