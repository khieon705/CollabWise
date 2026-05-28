package com.collabwise.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collabwise.data.model.Notification
import com.collabwise.data.repository.AuthRepository
import com.collabwise.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class NotificationUiState(
    val isLoading: Boolean = true,
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init { observe() }

    // ── Observe real-time ─────────────────────────────────────────────────────

    private fun observe() {
        val uid = authRepository.currentUid ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            notificationRepository.observeForUser(uid)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message)
                    }
                }
                .collect { notifs ->
                    _uiState.update {
                        it.copy(
                            isLoading     = false,
                            notifications = notifs,
                            unreadCount   = notifs.count { n -> !n.isRead }
                        )
                    }
                }
        }
    }

    // ── Mark read ─────────────────────────────────────────────────────────────

    fun markAsRead(notifId: String) {
        // Optimistically update UI before Firestore confirms
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map { n ->
                    if (n.id == notifId) n.copy(isRead = true) else n
                },
                unreadCount = maxOf(0, state.unreadCount - 1)
            )
        }
        viewModelScope.launch {
            runCatching { notificationRepository.markAsRead(notifId) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun markAllAsRead() {
        val uid = authRepository.currentUid ?: return
        // Optimistically flip all to read
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map { it.copy(isRead = true) },
                unreadCount   = 0
            )
        }
        viewModelScope.launch {
            runCatching { notificationRepository.markAllAsRead(uid) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}