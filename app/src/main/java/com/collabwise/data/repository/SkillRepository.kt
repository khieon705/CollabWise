package com.collabwise.data.repository

import com.collabwise.data.model.Skill
import com.collabwise.data.model.SkillCategory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val categoriesRef = db.collection("skill_categories")
    private val skillsRef     = db.collection("skills")

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Fetches all skill categories sorted by displayOrder.
     */
    suspend fun getAllCategories(): List<SkillCategory> =
        categoriesRef
            .orderBy("displayOrder")
            .get().await()
            .toObjects(SkillCategory::class.java)

    /**
     * Fetches all skills sorted alphabetically by name.
     */
    suspend fun getAllSkills(): List<Skill> =
        skillsRef
            .orderBy("name")
            .get().await()
            .toObjects(Skill::class.java)

    /**
     * Fetches specific skills by their document IDs.
     * Used when loading a member's declared skills for display.
     * Batches automatically to respect Firestore's 10-item whereIn limit.
     */
    suspend fun getSkillsByIds(ids: List<String>): List<Skill> {
        if (ids.isEmpty()) return emptyList()
        return ids
            .chunked(10)
            .flatMap { chunk ->
                skillsRef
                    .whereIn("id", chunk)
                    .get().await()
                    .toObjects(Skill::class.java)
            }
    }

    /**
     * Returns skills grouped by their category, both sorted.
     * Categories sorted by displayOrder.
     * Skills within each category sorted alphabetically.
     * Used by the Profile screen skill browser.
     */
    suspend fun getSkillsGrouped(): Map<SkillCategory, List<Skill>> {
        val allCategories = getAllCategories()
        val allSkills     = getAllSkills()

        return allCategories.associateWith { category ->
            allSkills
                .filter { it.categoryId == category.id }
                .sortedBy { it.name }
        }
    }
}
