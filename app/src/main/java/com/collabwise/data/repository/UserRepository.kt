package com.collabwise.data.repository

import com.collabwise.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val users = db.collection("users")

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Fetches a single user document by UID.
     * Returns null if the document does not exist.
     */
    suspend fun getUserById(uid: String): User? =
        users.document(uid)
            .get().await()
            .toObject(User::class.java)

    /**
     * Fetches a user document by email address.
     * Used by GroupRepository when inviting members.
     * Returns null if no user has that email.
     */
    suspend fun getUserByEmail(email: String): User? =
        users.whereEqualTo("email", email.trim().lowercase())
            .limit(1)
            .get().await()
            .toObjects(User::class.java)
            .firstOrNull()

    /**
     * Fetches multiple users by their UIDs.
     * Firestore whereIn supports a max of 10 items — batches automatically.
     */
    suspend fun getUsersByIds(uids: List<String>): List<User> {
        if (uids.isEmpty()) return emptyList()
        return uids
            .chunked(10)
            .flatMap { chunk ->
                users.whereIn("uid", chunk)
                    .get().await()
                    .toObjects(User::class.java)
            }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Replaces the user's declared skill IDs with the given list.
     */
    suspend fun updateSkills(uid: String, skillIds: List<String>) {
        users.document(uid)
            .update("skillIds", skillIds)
            .await()
    }

    /**
     * Updates the user's display name.
     */
    suspend fun updateName(uid: String, name: String) {
        users.document(uid)
            .update("name", name.trim())
            .await()
    }
}
