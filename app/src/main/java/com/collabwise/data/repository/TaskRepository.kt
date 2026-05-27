package com.collabwise.data.repository

import android.util.Log
import com.collabwise.data.model.Task
import com.collabwise.data.model.TaskStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestoreException
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

    // ── Helper: extract Firestore index URL ────────────────────────────────
    private fun extractIndexUrl(message: String?): String? {
        if (message == null) return null
        return Regex("https://console\\.firebase\\.google\\.com[^\\s]+")
            .find(message)
            ?.value
    }

    private fun logFirestoreError(tag: String, e: Exception) {
        val url = extractIndexUrl(e.message)
        if (url != null) {
            Log.e(tag, "🔥 Firestore Index URL: $url", e)
        } else {
            Log.e(tag, "🔥 Firestore error (no index url)", e)
        }
    }

    // ── Create ─────────────────────────────────────────────────────────────

    suspend fun createTask(task: Task): Task {
        val ref = tasksRef.document()
        val taskWithId = task.copy(id = ref.id)

        ref.set(taskWithId).await()
        return taskWithId
    }

    // ── Read ───────────────────────────────────────────────────────────────

    suspend fun getTaskById(taskId: String): Task? {
        return try {
            tasksRef.document(taskId)
                .get().await()
                .toObject(Task::class.java)
                ?.copy(id = taskId)
        } catch (e: Exception) {
            logFirestoreError("getTaskById", e)
            null
        }
    }

    suspend fun getTasksForProject(projectId: String): List<Task> {
        return try {
            tasksRef
                .whereEqualTo("projectId", projectId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get().await()
                .toObjects(Task::class.java)
        } catch (e: Exception) {
            logFirestoreError("getTasksForProject", e)
            emptyList()
        }
    }

    fun observeTasksForProject(projectId: String): Flow<List<Task>> = callbackFlow {
        val listener = tasksRef
            .whereEqualTo("projectId", projectId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("FirestoreListener", "Error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val tasks = snap?.toObjects(Task::class.java).orEmpty()
                trySend(tasks)
            }

        awaitClose { listener.remove() }
    }

    suspend fun getTasksForMember(memberId: String): List<Task> {
        return try {
            tasksRef
                .whereEqualTo("assignedMemberId", memberId)
                .get().await()
                .toObjects(Task::class.java)
        } catch (e: Exception) {
            logFirestoreError("getTasksForMember", e)
            emptyList()
        }
    }

    // ── Update ─────────────────────────────────────────────────────────────

    suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        tasksRef.document(taskId)
            .update("status", status.name)
            .await()
    }

    suspend fun updateTask(task: Task) {
        tasksRef.document(task.id).set(task).await()
    }

    suspend fun updateAssignment(
        taskId: String,
        memberId: String,
        memberName: String
    ) {
        tasksRef.document(taskId)
            .update(
                mapOf(
                    "assignedMemberId" to memberId,
                    "assignedMemberName" to memberName
                )
            ).await()
    }

    suspend fun batchUpdateStatuses(updates: Map<String, TaskStatus>) {
        if (updates.isEmpty()) return

        val batch = db.batch()
        updates.forEach { (taskId, status) ->
            batch.update(tasksRef.document(taskId), "status", status.name)
        }
        batch.commit().await()
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    suspend fun deleteTask(taskId: String) {
        tasksRef.document(taskId).delete().await()
    }
}