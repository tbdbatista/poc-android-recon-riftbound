package com.riftbound.recon.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riftbound.recon.domain.model.AuthState
import com.riftbound.recon.domain.model.UserProfile
import com.riftbound.recon.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignUpMode: Boolean = false,
    val errorMessage: String? = null,
    val showGuestDisclaimerDialog: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun toggleAuthMode() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = !_uiState.value.isSignUpMode,
            errorMessage = null
        )
    }

    fun setSignUpMode(isSignUp: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = isSignUp,
            errorMessage = null
        )
    }

    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, preencha o email e a senha.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInWithEmail(email, password)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapFirebaseError(error)
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, preencha todos os campos.")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "A senha deve ter pelo menos 6 caracteres.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signUpWithEmail(email, password, displayName)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapFirebaseError(error)
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInWithGoogle(idToken)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapFirebaseError(error)
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
            }
        }
    }

    fun requestGuestMode() {
        _uiState.value = _uiState.value.copy(showGuestDisclaimerDialog = true)
    }

    fun dismissGuestDialog() {
        _uiState.value = _uiState.value.copy(showGuestDisclaimerDialog = false)
    }

    fun confirmGuestMode() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showGuestDisclaimerDialog = false, isLoading = true)
            authRepository.setGuestMode(true)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun mapFirebaseError(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("The email address is already in use", ignoreCase = true) -> "Este email já está cadastrado em outra conta."
            msg.contains("The email address is badly formatted", ignoreCase = true) -> "O formato do email é inválido."
            msg.contains("There is no user record", ignoreCase = true) || msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) || msg.contains("wrong password", ignoreCase = true) -> "Email ou senha incorretos."
            msg.contains("network error", ignoreCase = true) -> "Erro de conexão. Verifique sua internet."
            msg.contains("blocked all requests", ignoreCase = true) -> "Muitas tentativas. Aguarde alguns instantes."
            else -> e.localizedMessage ?: "Ocorreu um erro ao autenticar. Tente novamente."
        }
    }
}
