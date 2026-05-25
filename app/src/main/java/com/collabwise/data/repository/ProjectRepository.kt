package com.collabwise.data.repository

import com.collabwise.data.model.Project
import com.collabwise.data.model.ProjectStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val db: FirebaseFirestore
) {

    private val projectsRef = db.collection("projects")
    // ── Create ────────────────────────────────────────────────

    suspend fun createProject(
        groupId: String,
        name: String,
        description: String,
        leaderId: String
    ): Project {

        val projectId = projectsRef.document().id

        val project = Project(
            id = projectId,
            groupId = groupId,
            name = name.trim(),
            description = description.trim(),
            leaderId = leaderId,
            status = ProjectStatus.ACTIVE.name,
            createdAt = System.currentTimeMillis()
        )

        try {
            projectsRef.document(projectId)
                .set(project)
                .await()

            println("🔥 WRITE SUCCESS")
        } catch (e: Exception) {
            println("❌ WRITE FAILED")
            e.printStackTrace()
        }

        return project
    }

    // ── Read ────────────────────────────────────────────────

    suspend fun getProjectById(projectId: String): Project? {
        return projectsRef.document(projectId)
            .get()
            .await()
            .toObject(Project::class.java)
            ?.copy(id = projectId)
    }

    suspend fun getProjectsForGroup(groupId: String): List<Project> {
        return projectsRef
            .whereEqualTo("groupId", groupId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(Project::class.java)?.copy(id = doc.id)
            }
    }

    fun observeProjectsForGroup(groupId: String): Flow<List<Project>> =
        callbackFlow {

            val listener = projectsRef
                .whereEqualTo("groupId", groupId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snap, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val projects = snap?.documents?.mapNotNull { doc ->
                        doc.toObject(Project::class.java)?.copy(id = doc.id)
                    } ?: emptyList()

                    trySend(projects)
                }

            awaitClose { listener.remove() }
        }

    // ── Update ────────────────────────────────────────────────

    suspend fun updateProjectStatus(
        projectId: String,
        status: ProjectStatus
    ) {
        projectsRef.document(projectId)
            .update("status", status.name)
            .await()
    }

    // ── Delete ────────────────────────────────────────────────

    suspend fun deleteProject(projectId: String) {
        projectsRef.document(projectId)
            .delete()
            .await()
    }
}