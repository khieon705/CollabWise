package com.collabwise.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collabwise.data.model.User
import com.collabwise.data.repository.AuthRepository
import com.collabwise.data.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // ── UI STATE ───────────────────────────────────────────────

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── AUTH STATE (SOURCE OF TRUTH) ───────────────────────────

    val authState = authRepository.authState
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AuthState.Loading
        )

    init {

        viewModelScope.launch {

            authState.collect { state ->

                when (state) {

                    AuthState.Authenticated -> {
                        loadCurrentUser()
                    }

                    AuthState.Unauthenticated -> {
                        _currentUser.value = null
                    }

                    AuthState.Loading -> Unit
                }
            }
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────

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

    // ── REGISTER ──────────────────────────────────────────────

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

    // ── LOGOUT ────────────────────────────────────────────────

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
    }

    // ── LOAD CURRENT USER ─────────────────────────────────────

    fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value =
                authRepository.getCurrentUserProfile()
        }
    }
}