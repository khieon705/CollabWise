package com.collabwise.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collabwise.data.model.Skill
import com.collabwise.data.model.SkillCategory
import com.collabwise.data.model.User
import com.collabwise.data.repository.AuthRepository
import com.collabwise.data.repository.SkillRepository
import com.collabwise.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,

    // All skills grouped by category — for the skill browser
    val skillsGrouped: Map<SkillCategory, List<Skill>> = emptyMap(),

    // The IDs the user has currently toggled on (may differ from saved)
    val selectedSkillIds: Set<String> = emptySet(),

    // Whether the user has unsaved changes
    val isDirty: Boolean = false,

    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val skillRepository: SkillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Keep track of the originally saved skill IDs so we can detect dirty state
    private var savedSkillIds: Set<String> = emptySet()

    init { load() }

    // ── Load ──────────────────────────────────────────────────────────────────

    private fun load() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserProfile()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "Could not load profile.") }
                return@launch
            }

            val skillsGrouped = skillRepository.getSkillsGrouped()
            savedSkillIds     = user.skillIds.toSet()

            _uiState.update {
                it.copy(
                    isLoading        = false,
                    user             = user,
                    skillsGrouped    = skillsGrouped,
                    selectedSkillIds = savedSkillIds,
                    isDirty          = false
                )
            }
        }
    }

    // ── Skill toggle ──────────────────────────────────────────────────────────

    /**
     * Toggles a skill on or off.
     * Updates isDirty so the Save button becomes active only when
     * the selection differs from what is saved in Firestore.
     */
    fun toggleSkill(skillId: String) {
        val current = _uiState.value.selectedSkillIds.toMutableSet()
        if (skillId in current) current.remove(skillId) else current.add(skillId)
        _uiState.update {
            it.copy(
                selectedSkillIds = current,
                isDirty          = current != savedSkillIds
            )
        }
    }

    /**
     * Selects all skills in a given category at once.
     */
    fun selectAllInCategory(category: SkillCategory) {
        val categorySkillIds = _uiState.value.skillsGrouped[category]
            ?.map { it.id }?.toSet() ?: return
        val current = _uiState.value.selectedSkillIds + categorySkillIds
        _uiState.update {
            it.copy(
                selectedSkillIds = current,
                isDirty          = current != savedSkillIds
            )
        }
    }

    /**
     * Clears all selected skills in a given category.
     */
    fun clearCategory(category: SkillCategory) {
        val categorySkillIds = _uiState.value.skillsGrouped[category]
            ?.map { it.id }?.toSet() ?: return
        val current = _uiState.value.selectedSkillIds - categorySkillIds
        _uiState.update {
            it.copy(
                selectedSkillIds = current,
                isDirty          = current != savedSkillIds
            )
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Persists the selected skill IDs to Firestore.
     * Updates savedSkillIds and clears dirty state on success.
     */
    fun saveSkills() {
        val uid    = authRepository.currentUid ?: return
        val skills = _uiState.value.selectedSkillIds.toList()

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            runCatching { userRepository.updateSkills(uid, skills) }
                .onSuccess {
                    savedSkillIds = skills.toSet()
                    _uiState.update {
                        it.copy(
                            isSaving       = false,
                            isDirty        = false,
                            successMessage = "Skills saved successfully."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error    = e.message ?: "Failed to save skills."
                        )
                    }
                }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * How many skills in the given category are currently selected.
     */
    fun selectedCountInCategory(category: SkillCategory): Int {
        val categorySkillIds = _uiState.value.skillsGrouped[category]
            ?.map { it.id } ?: return 0
        return categorySkillIds.count { it in _uiState.value.selectedSkillIds }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }
}
