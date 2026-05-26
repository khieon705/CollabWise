package com.collabwise.data.seed

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FireStoreSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val TAG = "FireStoreSeeder"

        // Category IDs
        private const val TECHNOLOGY = "technology"
        private const val BUSINESS = "business_finance"
        private const val EVENTS = "events_marketing"
        private const val ACADEMIC = "academic"
        private const val SERVICE = "community_service"
        private const val LEADERSHIP = "leadership"
    }

    private val categoriesCollection =
        firestore.collection("skill_categories")

    private val skillsCollection =
        firestore.collection("skills")

    /**
     * Main entry point.
     * Safe to call every app launch because it checks if data already exists.
     */
    suspend fun seedDatabase() {

        try {

            // Prevent reseeding
            val existing =
                categoriesCollection.limit(1).get().await()

            if (!existing.isEmpty) {
                Log.d(TAG, "Database already seeded.")
                return
            }

            seedCategories()
            seedSkills()

            Log.d(TAG, "Database seeded successfully.")

        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding database", e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Categories
    // ─────────────────────────────────────────────────────────────

    private suspend fun seedCategories() {

        val categories = listOf(
            category(
                id = TECHNOLOGY,
                name = "Technology",
                order = 0
            ),
            category(
                id = BUSINESS,
                name = "Business/Finance",
                order = 1
            ),
            category(
                id = EVENTS,
                name = "Events/Marketing",
                order = 2
            ),
            category(
                id = ACADEMIC,
                name = "Academic",
                order = 3
            ),
            category(
                id = SERVICE,
                name = "Community Service",
                order = 4
            ),
            category(
                id = LEADERSHIP,
                name = "Leadership",
                order = 5
            )
        )

        val batch = firestore.batch()

        for (category in categories) {

            val document =
                categoriesCollection.document(category["id"] as String)

            batch.set(document, category)
        }

        batch.commit().await()
    }

    // ─────────────────────────────────────────────────────────────
    // Skills
    // ─────────────────────────────────────────────────────────────

    private suspend fun seedSkills() {

        val allSkills = buildList {

            addAll(
                createSkills(
                    skills = technologySkills,
                    categoryId = TECHNOLOGY
                )
            )

            addAll(
                createSkills(
                    skills = businessSkills,
                    categoryId = BUSINESS
                )
            )

            addAll(
                createSkills(
                    skills = eventSkills,
                    categoryId = EVENTS
                )
            )

            addAll(
                createSkills(
                    skills = academicSkills,
                    categoryId = ACADEMIC
                )
            )

            addAll(
                createSkills(
                    skills = serviceSkills,
                    categoryId = SERVICE
                )
            )

            addAll(
                createSkills(
                    skills = leadershipSkills,
                    categoryId = LEADERSHIP
                )
            )
        }

        val batch = firestore.batch()

        for (skill in allSkills) {

            val document =
                skillsCollection.document(skill["id"] as String)

            batch.set(document, skill)
        }

        batch.commit().await()
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private fun category(
        id: String,
        name: String,
        order: Int
    ): Map<String, Any> {

        return mapOf(
            "id" to id,
            "name" to name,
            "displayOrder" to order
        )
    }

    private fun createSkills(
        skills: List<String>,
        categoryId: String
    ): List<Map<String, Any>> {

        return skills.map { skillName ->

            mapOf(
                "id" to skillName.toSkillId(),
                "name" to skillName,
                "categoryId" to categoryId
            )
        }
    }

    private fun String.toSkillId(): String {
        return lowercase()
            .replace("/", "_")
            .replace(".", "")
            .replace(" ", "_")
    }

    // ─────────────────────────────────────────────────────────────
    // Seed Data
    // ─────────────────────────────────────────────────────────────

    private val technologySkills = listOf(
        "Java",
        "Python",
        "JavaScript",
        "TypeScript",
        "Kotlin",
        "React",
        "Vue.js",
        "SpringBoot",
        "Node.js",
        "SQL",
        "PostgreSQL",
        "MongoDB",
        "Firebase",
        "Git",
        "Docker",
        "UI/UX Design",
        "Figma",
        "Testing",
        "Algorithms",
        "Data Structures",
        "Machine Learning",
        "Cybersecurity",
        "Network Administration"
    )

    private val businessSkills = listOf(
        "Accounting",
        "Auditing",
        "Budgeting",
        "Financial Analysis",
        "Financial Reporting",
        "MS Excel",
        "Business Writing",
        "Documentation",
        "Research",
        "Data Entry",
        "Bookkeeping",
        "Taxation",
        "Cost Accounting",
        "Presentation",
        "Project Management"
    )

    private val eventSkills = listOf(
        "Event Planning",
        "Graphic Design",
        "Social Media Management",
        "Copywriting",
        "Photography",
        "Video Editing",
        "Sponsorship",
        "Public Relations",
        "Content Creation",
        "Adobe Photoshop",
        "Adobe Premiere",
        "Canva",
        "Brand Management",
        "Script Writing",
        "Emceeing"
    )

    private val academicSkills = listOf(
        "Technical Writing",
        "Data Analysis",
        "Statistics",
        "Literature Review",
        "Research Methodology",
        "Academic Writing",
        "Peer Review",
        "Citation Management",
        "Presentation",
        "Proofreading",
        "Curriculum Development"
    )

    private val serviceSkills = listOf(
        "Outreach Planning",
        "Logistics",
        "Coordination",
        "First Aid",
        "Community Organizing",
        "Fundraising",
        "Volunteer Management",
        "Liaison",
        "Public Speaking",
        "Facilitation"
    )

    private val leadershipSkills = listOf(
        "Project Management",
        "Strategic Planning",
        "Team Building",
        "Decision Making",
        "Conflict Resolution",
        "Mentoring",
        "Time Management",
        "Delegation",
        "Risk Management",
        "Stakeholder Management"
    )
}