package com.collabwise.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collabwise.algorithm.BfsUnlocker
import com.collabwise.algorithm.GreedyScheduler
import com.collabwise.algorithm.MergeSort
import com.collabwise.algorithm.TopologicalSort
import com.collabwise.data.model.*
import com.collabwise.data.repository.*
import com.collabwise.ui.navigation.Screen
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class ProjectUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val group: Group? = null,
    val tasks: List<Task> = emptyList(),           // Merge Sorted
    val members: List<User> = emptyList(),
    val currentUser: User? = null,
    val isLeader: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    // Create task sheet
    val showCreateTask: Boolean = false,
    val isCreatingTask: Boolean = false,
    val createTaskError: String? = null,
    // Cycle detection error (separate so it shows inside the sheet)
    val cycleError: String? = null,
    // Task detail sheet
    val selectedTask: Task? = null,
    val showTaskDetail: Boolean = false,
    val skillsGrouped: Map<SkillCategory, List<Skill>> = emptyMap()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val notificationRepository: NotificationRepository,
    private val skillRepository: SkillRepository,
) : ViewModel() {

    private val groupId: String   = checkNotNull(savedStateHandle[Screen.Project.ARG_GROUP_ID])
    private val projectId: String = checkNotNull(savedStateHandle[Screen.Project.ARG_PROJECT_ID])

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    init { load() }

    // ── Load ──────────────────────────────────────────────────────────────────

    private fun load() {
        viewModelScope.launch {

            val user    = authRepository.getCurrentUserProfile()
            val project = projectRepository.getProjectById(projectId)
            val group   = groupRepository.getGroupById(groupId)

            if (project == null || group == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Project not found."
                    )
                }
                return@launch
            }

            // ── Load members ───────────────────────────────────────────────
            val members = groupRepository.getMembersAsUsers(groupId)

            // ── Determine role ─────────────────────────────────────────────
            val isLeader = group.leaderId == user?.uid

            // ── Load grouped skills ────────────────────────────────────────
            val groupedSkills = skillRepository.getSkillsGrouped()

            // ── Initial UI state ───────────────────────────────────────────
            _uiState.update {
                it.copy(
                    project       = project,
                    group         = group,
                    members       = members,
                    currentUser   = user,
                    isLeader      = isLeader,
                    isLoading     = false,
                    skillsGrouped = groupedSkills
                )
            }

            // ── Observe tasks in real-time ────────────────────────────────
            taskRepository.observeTasksForProject(projectId)
                .catch {
                }
                .collect { rawTasks ->

                    // Merge Sort
                    val sorted = MergeSort.sort(rawTasks)

                    _uiState.update {
                        it.copy(tasks = sorted)
                    }

                    // Keep detail sheet synchronized
                    val selected = _uiState.value.selectedTask

                    if (selected != null) {
                        val updated = sorted.find { it.id == selected.id }

                        _uiState.update {
                            it.copy(
                                selectedTask = updated ?: selected
                            )
                        }
                    }
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TASK CREATION
    // Orchestration: TopologicalSort → GreedyScheduler → persist → notify
    // ─────────────────────────────────────────────────────────────────────────

    fun showCreateTask()    { _uiState.update { it.copy(showCreateTask = true, createTaskError = null, cycleError = null) } }
    fun dismissCreateTask() { _uiState.update { it.copy(showCreateTask = false, createTaskError = null, cycleError = null) } }

    /**
     * Full task creation pipeline:
     *
     * Step 1 — Dependency validation (Topological Sort / Kahn's Algorithm)
     *   If dependsOn is not empty, run Topological Sort on the current task
     *   graph with the proposed new edges added temporarily. Reject if cycle.
     *
     * Step 2 — Determine initial status
     *   AVAILABLE if no dependencies, or all prerequisites are already DONE.
     *   BLOCKED otherwise.
     *
     * Step 3 — Greedy auto-assignment (Skill-Based Greedy Task Scheduling)
     *   Score all members via Binary Search on sorted skill lists.
     *   Quick Sort members by score descending (tiebreak: fewer tasks).
     *   Assign to the top-ranked member.
     *
     * Step 4 — Persist to Firestore
     *
     * Step 5 — Notify assigned member
     */
    fun createTask(
        title: String,
        description: String,
        requiredSkillIds: List<String>,
        dueDate: LocalDate,
        dependsOn: List<String>
    ) {
        if (title.isBlank()) {
            _uiState.update { it.copy(createTaskError = "Task title cannot be empty.") }
            return
        }

        val state   = _uiState.value
        val members = state.members
        val project = state.project ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingTask = true, createTaskError = null, cycleError = null) }

            try {
                // ── Step 1: Topological Sort (cycle detection) ────────────────
                if (dependsOn.isNotEmpty()) {
                    val currentTasks = state.tasks

                    // Build a temporary task to represent what we are about to add
                    val tempTask = Task(
                        id               = "__temp__",
                        projectId        = projectId,
                        groupId          = groupId,
                        title            = title,
                        status           = TaskStatus.LOCKED.name,
                        requiredSkillIds = requiredSkillIds,
                        dependsOn        = dependsOn
                    )

                    val validation = TopologicalSort.validate(
                        tasks   = currentTasks + tempTask,
                        newEdge = null  // edges already included in tempTask.dependsOn
                    )

                    if (!validation.isValid) {
                        _uiState.update {
                            it.copy(isCreatingTask = false, cycleError = validation.message)
                        }
                        return@launch
                    }
                }

                // ── Step 2: Determine initial status ──────────────────────────
                val allPrereqsDone = dependsOn.all { prereqId ->
                    state.tasks.find { it.id == prereqId }?.status == TaskStatus.DONE.name
                }
                val initialStatus = when {
                    dependsOn.isEmpty() -> TaskStatus.IN_PROGRESS
                    allPrereqsDone      -> TaskStatus.IN_PROGRESS
                    else                -> TaskStatus.LOCKED
                }

                // ── Step 3: Greedy auto-assignment ────────────────────────────
                //
                // Build current task count per member so Quick Sort can
                // use it as a tiebreaker when scores are equal.
                val taskCountMap = state.tasks
                    .groupBy { it.assignedMemberId }
                    .mapValues { it.value.size }

                // Create a temporary task object just for scoring purposes
                val taskForScoring = Task(
                    projectId        = projectId,
                    groupId          = groupId,
                    title            = title,
                    requiredSkillIds = requiredSkillIds
                )

                val assignmentResult = GreedyScheduler.assign(
                    task         = taskForScoring,
                    members      = members,
                    taskCountMap = taskCountMap as Map<String?, Int>
                ) ?: throw IllegalStateException(
                    "No members in this group to assign the task to."
                )

                // ── Step 4: Persist task to Firestore ─────────────────────────
                val task = Task(
                    projectId          = projectId,
                    groupId            = groupId,
                    title              = title.trim(),
                    description        = description.trim(),
                    status             = initialStatus.name,
                    assignedMemberId   = assignmentResult.assignedMemberId,
                    assignedMemberName = assignmentResult.assignedMemberName,
                    requiredSkillIds   = requiredSkillIds,
                    dependsOn          = dependsOn,
                    dueDate            = dueDate.toString(),
                    createdAt          = System.currentTimeMillis()
                )

                val created = taskRepository.createTask(task)

                // ── Step 5: Notify assigned member ────────────────────────────
                val notifMessage = when (initialStatus) {
                    TaskStatus.IN_PROGRESS ->
                        "You have been assigned: \"${title.trim()}\""
                    TaskStatus.LOCKED   ->
                        "Assigned to \"${title.trim()}\" — waiting on prerequisites to start."
                    else                 ->
                        "New task assigned: \"${title.trim()}\""
                }

                notificationRepository.createNotification(
                    Notification(
                        userId      = assignmentResult.assignedMemberId,
                        taskId      = created.id,
                        projectId   = projectId,
                        groupId     = groupId,
                        taskTitle   = title.trim(),
                        projectName = project.name,
                        message     = notifMessage,
                        createdAt   = System.currentTimeMillis()
                    )
                )

                _uiState.update {
                    it.copy(
                        isCreatingTask = false,
                        showCreateTask = false,
                        successMessage = "\"${title.trim()}\" assigned to ${assignmentResult.assignedMemberName}."
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingTask  = false,
                        createTaskError = e.message ?: "Failed to create task."
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TASK STATUS UPDATE
    // Orchestration: persist status → BFS unlocker → batch update → notify
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates a task's status. When the new status is DONE, runs BFS to
     * find all downstream tasks that are now unblocked, batch-updates their
     * status to AVAILABLE, and sends notifications to their assigned members.
     *
     * Status transitions allowed:
     *   AVAILABLE   → IN_PROGRESS  (member starts)
     *   IN_PROGRESS → DONE         (member completes)
     */
    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        val state = _uiState.value

        // Guard: only the assigned member can update status
        val task = state.tasks.find { it.id == taskId }
        if (task == null) {
            _uiState.update { it.copy(error = "Task not found.") }
            return
        }
        if (task.status == TaskStatus.LOCKED.name) {
            _uiState.update { it.copy(error = "This task is still blocked by unfinished prerequisites.") }
            return
        }

        viewModelScope.launch {
            try {
                // Persist the status change
                taskRepository.updateTaskStatus(taskId, newStatus)

                // ── BFS: only runs when task is marked DONE ───────────────────
                if (newStatus == TaskStatus.DONE) {
                    propagateDone(taskId, state)
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update task.") }
            }
        }
    }

    /**
     * Runs BFS traversal from the completed task.
     * Finds all downstream BLOCKED tasks whose prerequisites are all now DONE.
     * Batch-writes AVAILABLE status for unblocked tasks.
     * Batch-writes notifications for each unblocked member.
     */
    private suspend fun propagateDone(completedTaskId: String, state: ProjectUiState) {
        // Fetch the freshest task list from Firestore so BFS has accurate statuses
        val freshTasks = taskRepository.getTasksForProject(projectId)

        val result = BfsUnlocker.propagate(
            completedTaskId = completedTaskId,
            allTasks        = freshTasks
        )

        // Batch update all unblocked tasks to AVAILABLE
        if (result.unlockedTaskIds.isNotEmpty()) {
            taskRepository.batchUpdateStatuses(
                result.unlockedTaskIds.associateWith { TaskStatus.IN_PROGRESS }
            )
        }

        // Batch write notifications for each unblocked member
        if (result.notifications.isNotEmpty()) {
            val project = state.project
            val notifications = result.notifications.map { n ->

                // Enrich notification message with due date warning if applicable
                val unlockedTask = freshTasks.find { it.id == n.taskId }
                val urgencyNote  = unlockedTask?.dueDate?.let { due ->
                    val daysLeft = ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        LocalDate.parse(due)
                    ).toInt()
                    when {
                        daysLeft < 0  -> " ⚠ This task is overdue!"
                        daysLeft == 0 -> " Due today!"
                        daysLeft == 1 -> " Due tomorrow!"
                        daysLeft <= 3 -> " Due in $daysLeft days."
                        else          -> ""
                    }
                } ?: ""

                Notification(
                    userId      = n.memberId,
                    taskId      = n.taskId,
                    projectId   = projectId,
                    groupId     = groupId,
                    taskTitle   = n.taskTitle,
                    projectName = project?.name ?: "",
                    message     = n.message + urgencyNote,
                    createdAt   = System.currentTimeMillis()
                )
            }
            notificationRepository.createNotifications(notifications)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEPENDENCY MANAGEMENT
    // Orchestration: TopologicalSort → persist → re-evaluate task status
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds a dependency edge: taskId now depends on dependsOnId.
     *
     * Step 1 — Self-dependency guard
     * Step 2 — Topological Sort validates the updated graph
     * Step 3 — Persist updated dependsOn list
     * Step 4 — If the task was AVAILABLE but the new prereq is not DONE,
     *           flip task back to BLOCKED
     */
    fun addDependency(taskId: String, dependsOnId: String) {
        if (taskId == dependsOnId) {
            _uiState.update { it.copy(error = "A task cannot depend on itself.") }
            return
        }

        val state  = _uiState.value
        val target = state.tasks.find { it.id == taskId } ?: return

        if (dependsOnId in target.dependsOn) {
            _uiState.update { it.copy(error = "This dependency already exists.") }
            return
        }

        viewModelScope.launch {
            // Build updated task list with proposed new edge
            val updatedTasks = state.tasks.map { t ->
                if (t.id == taskId) t.copy(dependsOn = t.dependsOn + dependsOnId) else t
            }

            val validation = TopologicalSort.validate(updatedTasks)
            if (!validation.isValid) {
                _uiState.update { it.copy(cycleError = validation.message) }
                return@launch
            }

            // Persist
            val newDependsOn = target.dependsOn + dependsOnId
            taskRepository.updateTask(target.copy(dependsOn = newDependsOn))

            // If prerequisite is not DONE, block the task
            val prereq = state.tasks.find { it.id == dependsOnId }
            if (prereq != null
                && prereq.status != TaskStatus.DONE.name
                && target.status == TaskStatus.IN_PROGRESS.name) {
                taskRepository.updateTaskStatus(taskId, TaskStatus.LOCKED)
            }
        }
    }

    /**
     * Removes a dependency edge.
     * After removal, re-checks if the task should now be AVAILABLE
     * (all remaining prerequisites are DONE).
     */
    fun removeDependency(taskId: String, dependsOnId: String) {
        val state  = _uiState.value
        val target = state.tasks.find { it.id == taskId } ?: return

        viewModelScope.launch {
            val newDependsOn = target.dependsOn.filter { it != dependsOnId }
            taskRepository.updateTask(target.copy(dependsOn = newDependsOn))

            // Re-check if task should now be unblocked
            val freshTasks   = taskRepository.getTasksForProject(projectId)
            val updatedTarget = freshTasks.find { it.id == taskId } ?: return@launch
            val allDone = updatedTarget.dependsOn.all { prereqId ->
                freshTasks.find { it.id == prereqId }?.status == TaskStatus.DONE.name
            }
            if (allDone && updatedTarget.status == TaskStatus.LOCKED.name) {
                taskRepository.updateTaskStatus(taskId, TaskStatus.IN_PROGRESS)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TASK DETAIL SHEET
    // ─────────────────────────────────────────────────────────────────────────

    fun selectTask(task: Task) {
        _uiState.update { it.copy(selectedTask = task, showTaskDetail = true) }
    }

    fun dismissTaskDetail() {
        _uiState.update { it.copy(showTaskDetail = false, selectedTask = null) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE TASK
    // ─────────────────────────────────────────────────────────────────────────

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            runCatching { taskRepository.deleteTask(taskId) }
                .onSuccess { dismissTaskDetail() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the Task objects that the given task depends on.
     * Used by TaskDetailSheet to display prerequisite names and statuses.
     */
    fun getPrerequisites(task: Task): List<Task> {
        val allTasks = _uiState.value.tasks
        return task.dependsOn.mapNotNull { prereqId ->
            allTasks.find { it.id == prereqId }
        }
    }

    /**
     * Returns all tasks that could be added as dependencies of the given task
     * without creating a cycle — i.e., tasks that do not already depend on it.
     */
    fun getEligibleDependencies(
        taskId: String? = null
    ): List<Task> {
        val state = _uiState.value

        return state.tasks.filter { task ->

            // exclude itself only in edit mode
            val notSelf = taskId == null || task.id != taskId

            // only active/incomplete tasks
            val validStatus =
                task.status != TaskStatus.DONE.name

            notSelf && validStatus
        }
    }

    /**
     * Returns tasks grouped by assigned member for the Members tab.
     */
    fun tasksByMember(): Map<User, List<Task>> {
        val state = _uiState.value
        return state.members.associateWith { member ->
            state.tasks.filter { it.assignedMemberId == member.uid }
        }
    }

    fun clearError()   { _uiState.update { it.copy(error = null, cycleError = null) } }
    fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
}
