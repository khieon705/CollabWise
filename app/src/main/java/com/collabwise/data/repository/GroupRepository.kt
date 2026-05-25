package com.collabwise.data.repository

import com.collabwise.data.model.Group
import com.collabwise.data.model.Member
import com.collabwise.data.model.MemberRole
import com.collabwise.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val userRepository: UserRepository
) {
    private val groupsRef = db.collection("groups")

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates a new group document and automatically adds the leader as the
     * first member in the members subcollection.
     */
    suspend fun createGroup(
        name: String,
        description: String,
        leader: User
    ): Group {
        val group = Group(
            name        = name,
            description = description,
            leaderId    = leader.uid
        )
        val ref     = groupsRef.add(group).await()
        val created = group.copy(id = ref.id)

        ref.set(created).await()
        // Leader is automatically a member
        ref.collection("members")
            .document(leader.uid)
            .set(
                Member(
                    groupId = ref.id,
                    userId = leader.uid,
                    role = MemberRole.LEADER.name,
                    joinedAt = System.currentTimeMillis()
                )
            )
            .await()

        return created
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Fetches a single group by ID.
     * Returns null if not found.
     */
    suspend fun getGroupById(groupId: String): Group? =
        groupsRef.document(groupId)
            .get().await()
            .toObject(Group::class.java)

    /**
     * Fetches all groups that the given user belongs to.
     * Checks each group's members subcollection.
     *
     * Note: Firestore does not support collection group queries with subcollection
     * filters in a simple way, so we fetch all groups and filter by membership.
     * For small-scale student orgs this is acceptable. A more scalable approach
     * would maintain a separate user_groups flat collection.
     */
    suspend fun getGroupsForUser(uid: String): List<Group> {
        val allGroups = groupsRef.get().await().toObjects(Group::class.java)
        return allGroups.filter { group ->
            groupsRef.document(group.id)
                .collection("members")
                .document(uid)
                .get().await()
                .exists()
        }
    }

    /**
     * Real-time observer for groups the user belongs to.
     * Emits on any change to the groups collection.
     */
    fun observeGroupsForUser(uid: String): Flow<List<Group>> = callbackFlow {

        val listener = groupsRef.addSnapshotListener { snap, _ ->
            val allGroups = snap?.toObjects(Group::class.java) ?: emptyList()

            kotlinx.coroutines.GlobalScope.launch {

                val filtered = allGroups.filter { group ->
                    try {
                        groupsRef.document(group.id)
                            .collection("members")
                            .document(uid)
                            .get()
                            .await()
                            .exists()
                    } catch (e: Exception) {
                        false
                    }
                }

                trySend(filtered)
            }
        }

        awaitClose { listener.remove() }
    }

    // ── Members ───────────────────────────────────────────────────────────────

    /**
     * Fetches all GroupMember entries for a group.
     */
    suspend fun getMembers(groupId: String): List<Member> =
        groupsRef.document(groupId)
            .collection("members")
            .get().await()
            .toObjects(Member::class.java)

    /**
     * Fetches all group members as full User objects.
     * Combines getMembers() with UserRepository lookups.
     */
    suspend fun getMembersAsUsers(groupId: String): List<User> {
        val members = getMembers(groupId)
        return members.mapNotNull { userRepository.getUserById(it.userId) }
    }

    /**
     * Checks whether a specific user is a member of a group.
     */
    suspend fun isMember(groupId: String, uid: String): Boolean =
        groupsRef.document(groupId)
            .collection("members")
            .document(uid)
            .get().await()
            .exists()

    // ── Invite ────────────────────────────────────────────────────────────────

    /**
     * Invites a user to the group by their email address.
     * Throws IllegalArgumentException if no user has that email.
     * Throws IllegalStateException if the user is already a member.
     * Returns the newly added User on success.
     */
    suspend fun inviteMember(groupId: String, email: String): User {
        val invitee = userRepository.getUserByEmail(email)
            ?: throw IllegalArgumentException("No CollabWise account found for $email.")

        val memberRef = groupsRef.document(groupId)
            .collection("members")
            .document(invitee.uid)

        if (memberRef.get().await().exists()) {
            throw IllegalStateException("${invitee.name} is already a member of this group.")
        }

        memberRef.set(Member(groupId = groupId, userId = invitee.uid, role = MemberRole.MEMBER.name, joinedAt = System.currentTimeMillis())).await()
        return invitee
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    /**
     * Removes a member from the group by their UID.
     */
    suspend fun removeMember(groupId: String, uid: String) {
        groupsRef.document(groupId)
            .collection("members")
            .document(uid)
            .delete().await()
    }
}
