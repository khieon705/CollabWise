package com.collabwise.data.repository

import com.collabwise.data.model.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val notifRef = db.collection("notifications")

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Saves a single notification document to Firestore.
     * Used when a task is created (assignment notification).
     */
    suspend fun createNotification(notification: Notification) {
        notifRef.add(notification).await()
    }

    /**
     * Saves multiple notifications in a single Firestore batch write.
     * Used by BfsUnlocker when multiple tasks are unblocked at once.
     */
    suspend fun createNotifications(notifications: List<Notification>) {
        if (notifications.isEmpty()) return
        val batch = db.batch()
        notifications.forEach { n ->
            batch.set(notifRef.document(), n)
        }
        batch.commit().await()
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Real-time observer for all notifications belonging to a user.
     * Sorted by createdAt descending so newest appear first.
     * Emits on every add or update (e.g. isRead changes).
     */
    fun observeForUser(uid: String): Flow<List<Notification>> = callbackFlow {

        val listener = notifRef
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->

                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (snap == null) {
                    return@addSnapshotListener
                }

                val notifications = snap.documents.mapNotNull { doc ->

                    doc.toObject(Notification::class.java)
                        ?.copy(id = doc.id)
                }

                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }

    /**
     * One-time fetch of unread notifications for a user.
     * Used to compute the badge count without starting a listener.
     */
    suspend fun getUnreadCount(uid: String): Int =
        notifRef
            .whereEqualTo("userId", uid)
            .whereEqualTo("isRead", false)
            .get().await()
            .size()

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Marks a single notification as read.
     */
    suspend fun markAsRead(notifId: String) {
        notifRef.document(notifId)
            .update("isRead", true)
            .await()
    }

    /**
     * Marks ALL unread notifications for a user as read in one batch.
     * Called from the "Mark all as read" button in NotificationScreen.
     */
    suspend fun markAllAsRead(uid: String) {
        val unread = notifRef
            .whereEqualTo("userId", uid)
            .whereEqualTo("isRead", false)
            .get().await()

        if (unread.isEmpty) return

        val batch = db.batch()
        unread.documents.forEach { doc ->
            batch.update(doc.reference, "isRead", true)
        }
        batch.commit().await()
    }
}
