package com.collabwise.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collabwise.data.model.Group
import com.collabwise.data.model.Project
import com.collabwise.data.model.User
import com.collabwise.data.repository.AuthRepository
import com.collabwise.data.repository.GroupRepository
import com.collabwise.data.repository.ProjectRepository
import com.collabwise.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class GroupUiState(
    val isLoading: Boolean = true,
    val group: Group? = null,
    val members: List<User> = emptyList(),
    val projects: List<Project> = emptyList(),
    val currentUser: User? = null,
    val isLeader: Boolean = false,
    val error: String? = null,
    // invite dialog
    val showInvite: Boolean = false,
    val isInviting: Boolean = false,
    val inviteError: String? = null,
    // create project dialog
    val showCreateProject: Boolean = false,
    val isCreatingProject: Boolean = false,
    val createProjectError: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class GroupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle[Screen.Group.ARG_GROUP_ID])

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    private var projectsJob: Job? = null

    init { load() }

    // ── Load ──────────────────────────────────────────────────────────────────

    private fun load() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserProfile()
            val group = groupRepository.getGroupById(groupId)

            if (group == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Group not found.")
                }
                return@launch
            }

            val members  = groupRepository.getMembersAsUsers(groupId)
            val isLeader = group.leaderId == user?.uid

            _uiState.update {
                it.copy(
                    group       = group,
                    members     = members,
                    currentUser = user,
                    isLeader    = isLeader,
                    isLoading   = false
                )
            }

            // Observe projects in real time
            observeProjects(groupId)
        }
    }

    private fun observeProjects(groupId: String) {
        projectsJob?.cancel()

        projectsJob = viewModelScope.launch {
            projectRepository.observeProjectsForGroup(groupId)
                .catch { Log.e("PROJECT_FLOW", it.message ?: "") }
                .collect { projects ->
                    Log.d("PROJECT_FLOW", "Firestore emitted size = ${projects.size}")
                    _uiState.update { it.copy(projects = projects) }
                }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        load()
    }

    // ── Invite member ─────────────────────────────────────────────────────────

    fun showInvite() {
        _uiState.update { it.copy(showInvite = true, inviteError = null) }
    }

    fun dismissInvite() {
        _uiState.update { it.copy(showInvite = false, inviteError = null) }
    }

    fun inviteMember(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(inviteError = "Email cannot be empty.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isInviting = true, inviteError = null) }
            runCatching { groupRepository.inviteMember(groupId, email.trim()) }
                .onSuccess { newUser ->
                    _uiState.update {
                        it.copy(
                            members    = it.members + newUser,
                            isInviting = false,
                            showInvite = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isInviting  = false,
                            inviteError = e.message ?: "Failed to invite member."
                        )
                    }
                }
        }
    }

    // ── Remove member ─────────────────────────────────────────────────────────

    fun removeMember(userId: String) {
        val state = _uiState.value
        // Prevent leader from removing themselves
        if (userId == state.group?.leaderId) {
            _uiState.update { it.copy(error = "The group leader cannot be removed.") }
            return
        }
        viewModelScope.launch {
            runCatching { groupRepository.removeMember(groupId, userId) }
                .onSuccess {
                    _uiState.update { s ->
                        s.copy(members = s.members.filter { it.uid != userId })
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to remove member.") }
                }
        }
    }

    // ── Create project ────────────────────────────────────────────────────────

    fun showCreateProject() {
        _uiState.update { it.copy(showCreateProject = true, createProjectError = null) }
    }

    fun dismissCreateProject() {
        _uiState.update { it.copy(showCreateProject = false, createProjectError = null) }
    }

    fun createProject(name: String, description: String) {
        val user = _uiState.value.currentUser ?: return
        if (name.isBlank()) {
            _uiState.update { it.copy(createProjectError = "Project name cannot be empty.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingProject = true, createProjectError = null) }
            runCatching {
                projectRepository.createProject(
                    groupId     = groupId,
                    name        = name.trim(),
                    description = description.trim(),
                    leaderId    = user.uid
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(isCreatingProject = false, showCreateProject = false)
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isCreatingProject  = false,
                        createProjectError = e.message ?: "Failed to create project."
                    )
                }
            }
        }
    }

    // ── Error ─────────────────────────────────────────────────────────────────

    fun clearError() { _uiState.update { it.copy(error = null) } }
}
