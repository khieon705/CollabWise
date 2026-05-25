package com.collabwise.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collabwise.data.model.User
import com.collabwise.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // ── UI state ───────────────────────────────────────────────

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // ✅ AUTH STATE (FROM FIREBASE ONLY)
    val isLoggedIn = authRepository.authState
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    init {
        loadCurrentUser()
    }

    // ── Login ───────────────────────────────────────────────

    fun login(
        email: String,
        password: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            authRepository.login(email, password)
                .onSuccess {
                    loadCurrentUser()
                    onSuccess?.invoke()
                }
                .onFailure {
                    _error.value = it.message ?: "Login failed"
                }

            _isLoading.value = false
        }
    }

    // ── Register ─────────────────────────────────────────────

    fun register(
        name: String,
        email: String,
        password: String,
        onSuccess: (() -> Unit)? = null
    ) {

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            authRepository.register(name, email, password)
                .onSuccess {
                    loadCurrentUser()
                    onSuccess?.invoke()
                }
                .onFailure {
                    _error.value = it.message ?: "Register failed"
                }

            _isLoading.value = false
        }
    }

    // ── Logout ───────────────────────────────────────────────

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
    }

    // ── Load user ─────────────────────────────────────────────

    fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value =
                authRepository.getCurrentUserProfile()
        }
    }
}