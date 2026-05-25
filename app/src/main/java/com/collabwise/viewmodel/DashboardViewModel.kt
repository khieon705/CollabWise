package com.collabwise.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collabwise.data.model.Group
import com.collabwise.data.model.User
import com.collabwise.data.repository.AuthRepository
import com.collabwise.data.repository.GroupRepository
import com.collabwise.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.count
import kotlin.collections.plus
import kotlin.onFailure
import kotlin.onSuccess
import kotlin.runCatching
import kotlin.text.isBlank
import kotlin.text.trim

// ── UI State ──────────────────────────────────────────────────────────────────

data class DashboardUiState(
    val isLoading: Boolean = true,
    val currentUser: User? = null,
    val groups: List<Group> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null,
    // create organization dialog
    val showCreateGroup: Boolean = false,
    val isCreatingGroup: Boolean = false,
    val createGroupError: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    // ── LOAD USER ───────────────────────────────────────────────────────────

    private fun loadUser() {
        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true, error = null) }

            val user = authRepository.getCurrentUserProfile()

            if (user == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Session expired. Please log in again.",
                        currentUser = null,
                        groups = emptyList(),
                        unreadCount = 0
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(currentUser = user) }

            loadGroups(user.uid)
            observeNotifications(user.uid)
        }
    }

    // ── LOAD GROUPS ──────────────────────────────────────────────────────────

    private fun loadGroups(uid: String) {
        viewModelScope.launch {
            runCatching {
                groupRepository.getGroupsForUser(uid)
            }.onSuccess { groups ->
                _uiState.update {
                    it.copy(
                        groups = groups,
                        isLoading = false
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    // ── NOTIFICATIONS (FIXED: single active collector) ──────────────────────

    private var notificationJob: kotlinx.coroutines.Job? = null

    private fun observeNotifications(uid: String) {
        notificationJob?.cancel()

        notificationJob = viewModelScope.launch {
            notificationRepository.observeForUser(uid)
                .catch { }
                .collect { notifs ->
                    _uiState.update {
                        it.copy(
                            unreadCount = notifs.count { n -> !n.isRead }
                        )
                    }
                }
        }
    }

    // ── REFRESH ──────────────────────────────────────────────────────────────

    fun refresh() {
        loadUser()
    }

    // ── CREATE GROUP ────────────────────────────────────────────────────────

    fun showCreateGroup() {
        _uiState.update {
            it.copy(showCreateGroup = true, createGroupError = null)
        }
    }

    fun dismissCreateGroup() {
        _uiState.update {
            it.copy(showCreateGroup = false, createGroupError = null)
        }
    }

    fun createGroup(name: String, description: String) {
        val user = _uiState.value.currentUser ?: return

        if (name.isBlank()) {
            _uiState.update {
                it.copy(createGroupError = "Group name cannot be empty.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isCreatingGroup = true, createGroupError = null)
            }

            runCatching {
                groupRepository.createGroup(
                    name.trim(),
                    description.trim(),
                    user
                )
            }.onSuccess { newGroup ->
                _uiState.update {
                    it.copy(
                        groups = it.groups + newGroup,
                        isCreatingGroup = false,
                        showCreateGroup = false
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isCreatingGroup = false,
                        createGroupError = e.message
                            ?: "Failed to create group. Please try again."
                    )
                }
            }
        }
    }

    // ── ERROR ───────────────────────────────────────────────────────────────

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        notificationJob?.cancel()
        super.onCleared()
    }
}