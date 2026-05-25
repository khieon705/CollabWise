package com.collabwise.data.repository

import com.collabwise.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val userRepository: UserRepository
) {

    // ── Firebase state ────────────────────────────────────────────────

    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    val currentUid: String?
        get() = auth.currentUser?.uid

    // ✅ SINGLE SOURCE OF TRUTH (REACTIVE)
    val authState: Flow<Boolean> = callbackFlow {

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }

        auth.addAuthStateListener(listener)

        // initial value
        trySend(auth.currentUser != null)

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    // ── Register ─────────────────────────────────────────────────────

    suspend fun register(
        name: String,
        email: String,
        password: String,
    ): Result<User> = runCatching {

        val authResult =
            auth.createUserWithEmailAndPassword(email, password).await()

        val uid = authResult.user?.uid
            ?: error("No UID returned from Firebase")

        val user = User(
            uid = uid,
            name = name.trim(),
            email = email.trim().lowercase(),
            skillIds = emptyList()
        )

        try {
            db.collection("users").document(uid).set(user).await()
        } catch (e: Exception) {
            auth.currentUser?.delete()?.await()
            throw e
        }

        user
    }

    // ── Login ────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<Unit> =
        runCatching {
            auth.signInWithEmailAndPassword(
                email.trim(),
                password
            ).await()
        }

    // ── Logout ───────────────────────────────────────────────────────

    fun logout() {
        auth.signOut()
    }

    // ── User profile ──────────────────────────────────────────────────

    suspend fun getCurrentUserProfile(): User? {
        val uid = currentUid ?: return null
        return userRepository.getUserById(uid)
    }
}