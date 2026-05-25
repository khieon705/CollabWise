package com.collabwise.data.repository

import com.collabwise.data.model.Task
import com.collabwise.data.model.TaskStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val tasksRef = db.collection("tasks")

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Persists a new task document to Firestore.
     * Returns the saved Task with its Firestore-generated ID.
     * assignedMemberId and status are already set by ProjectViewModel
     * before calling this.
     */
    suspend fun createTask(task: Task): Task {
        val ref = tasksRef.document()

        val taskWithId = task.copy(id = ref.id)

        ref.set(taskWithId).await()

        return taskWithId
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Fetches a single task by ID.
     * Returns null if not found.
     */
    suspend fun getTaskById(taskId: String): Task? =
        tasksRef.document(taskId)
            .get().await()
            .toObject(Task::class.java)?.copy(id = taskId)

    /**
     * Fetches all tasks for a project as a one-time snapshot.
     * Used by BfsUnlocker and TopologicalSort which need the full list.
     */
    suspend fun getTasksForProject(projectId: String): List<Task> =
        tasksRef
            .whereEqualTo("projectId", projectId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get().await()
            .toObjects(Task::class.java)

    /**
     * Real-time observer for all tasks in a project.
     * Emits whenever any task is added, modified, or removed.
     * ProjectViewModel applies MergeSort on each emission before updating UI state.
     */
    fun observeTasksForProject(projectId: String): Flow<List<Task>> = callbackFlow {
        val listener = tasksRef
            .whereEqualTo("projectId", projectId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) return@addSnapshotListener
                trySend(snap?.toObjects(Task::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    /**
     * Fetches all tasks assigned to a specific member across all groups.
     * Used for a "my tasks" view if added in future.
     */
    suspend fun getTasksForMember(memberId: String): List<Task> =
        tasksRef
            .whereEqualTo("assignedMemberId", memberId)
            .get().await()
            .toObjects(Task::class.java)

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Updates only the status field of a task.
     * Called when a member starts or completes a task.
     */
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        tasksRef.document(taskId)
            .update("status", status.name)
            .await()
    }

    /**
     * Overwrites the full task document.
     * Used when updating dependsOn list after adding/removing a dependency.
     */
    suspend fun updateTask(task: Task) {
        tasksRef.document(task.id).set(task).await()
    }

    /**
     * Updates only the assigned member fields.
     * Called by GreedyScheduler result after computing the best member.
     */
    suspend fun updateAssignment(
        taskId: String,
        memberId: String,
        memberName: String
    ) {
        tasksRef.document(taskId)
            .update(
                mapOf(
                    "assignedMemberId"   to memberId,
                    "assignedMemberName" to memberName
                )
            ).await()
    }

    /**
     * Batch-updates the status of multiple tasks in a single Firestore write.
     * Used by BfsUnlocker to flip multiple BLOCKED tasks to AVAILABLE atomically.
     *
     * @param updates Map of taskId → new TaskStatus
     */
    suspend fun batchUpdateStatuses(updates: Map<String, TaskStatus>) {
        if (updates.isEmpty()) return
        val batch = db.batch()
        updates.forEach { (taskId, status) ->
            batch.update(tasksRef.document(taskId), "status", status.name)
        }
        batch.commit().await()
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes a task document.
     */
    suspend fun deleteTask(taskId: String) {
        tasksRef.document(taskId).delete().await()
    }
}
