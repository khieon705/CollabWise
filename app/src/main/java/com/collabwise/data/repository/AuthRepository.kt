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

// ── AUTH STATE ───────────────────────────────────────────────────────────────

sealed class AuthState {
    data object LoggedOut : AuthState()
    data class LoggedIn(val user: User) : AuthState()
}

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val userRepository: UserRepository
) {

    // ── Firebase shortcuts ────────────────────────────────────────────────

    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    val currentUid: String?
        get() = auth.currentUser?.uid

    // ── SINGLE SOURCE OF TRUTH (AUTH STATE) ───────────────────────────────

    val authState: Flow<AuthState> = callbackFlow {

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->

            val firebaseUser = firebaseAuth.currentUser

            if (firebaseUser == null) {
                trySend(AuthState.LoggedOut)
                return@AuthStateListener
            }

            // Fetch user profile from Firestore
            db.collection("users")
                .document(firebaseUser.uid)
                .get()
                .addOnSuccessListener { snapshot ->

                    val user = snapshot.toObject(User::class.java)

                    if (user != null) {
                        trySend(AuthState.LoggedIn(user))
                    } else {
                        trySend(AuthState.LoggedOut)
                    }
                }
                .addOnFailureListener {
                    trySend(AuthState.LoggedOut)
                }
        }

        // register listener
        auth.addAuthStateListener(listener)

        // initial emission (prevents empty UI state)
        val initialUser = auth.currentUser

        if (initialUser == null) {
            trySend(AuthState.LoggedOut)
        }

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    // ── REGISTER ───────────────────────────────────────────────────────────

    suspend fun register(
        name: String,
        email: String,
        password: String
    ): Result<User> = runCatching {

        val result = auth.createUserWithEmailAndPassword(
            email.trim(),
            password
        ).await()

        val uid = result.user?.uid
            ?: error("Firebase did not return UID")

        val user = User(
            uid = uid,
            name = name.trim(),
            email = email.trim().lowercase(),
            skillIds = emptyList()
        )

        try {
            db.collection("users")
                .document(uid)
                .set(user)
                .await()
        } catch (e: Exception) {
            // rollback auth if firestore fails
            auth.currentUser?.delete()?.await()
            throw e
        }

        user
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────

    suspend fun login(
        email: String,
        password: String
    ): Result<Unit> = runCatching {

        auth.signInWithEmailAndPassword(
            email.trim(),
            password
        ).await()
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────

    fun logout() {
        auth.signOut()
    }

    // ── USER PROFILE FETCH (fallback only) ────────────────────────────────

    suspend fun getCurrentUserProfile(): User? {
        val uid = currentUid ?: return null
        return userRepository.getUserById(uid)
    }
}