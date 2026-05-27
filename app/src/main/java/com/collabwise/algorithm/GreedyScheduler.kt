package com.collabwise.algorithm

import com.collabwise.data.model.Task
import com.collabwise.data.model.User

data class AssignmentResult(
    val assignedMemberId: String,
    val assignedMemberName: String,
    val score: Int
)

object GreedyScheduler {

    /**
     * Binary Search
     * Checks whether a sorted skill list contains the target skill.
     */
    private fun binarySearch(
        sortedSkills: List<String>,
        target: String
    ): Boolean {

        var left = 0
        var right = sortedSkills.lastIndex

        while (left <= right) {
            val mid = (left + right) / 2
            val comparison = sortedSkills[mid].compareTo(target)

            when {
                comparison == 0 -> return true
                comparison < 0 -> left = mid + 1
                else -> right = mid - 1
            }
        }

        return false
    }

    /**
     * Calculates how many required skills the member has.
     */
    private fun skillMatchScore(
        task: Task,
        user: User
    ): Int {

        if (task.requiredSkillIds.isEmpty()) {
            return 0
        }

        val sortedSkills = user.skillIds.sorted()

        var score = 0

        for (requiredSkill in task.requiredSkillIds) {
            if (binarySearch(sortedSkills, requiredSkill)) {
                score++
            }
        }

        return score
    }

    /**
     * Greedy Assignment Algorithm
     *
     * Priority:
     * 1. Highest skill match score
     * 2. Lowest workload
     * 3. First matching member
     */
    fun assign(
        task: Task,
        members: List<User>,
        taskCountMap: Map<String?, Int>
    ): AssignmentResult? {

        if (members.isEmpty()) {
            return null
        }

        var bestMember: User? = null
        var bestScore = -1
        var lowestWorkload = Int.MAX_VALUE

        for (member in members) {
            val score = skillMatchScore(task, member)
            val workload = taskCountMap[member.uid] ?: 0

            val shouldReplace =
                score > bestScore ||
                        (
                                score == bestScore &&
                                        workload < lowestWorkload
                                )

            if (shouldReplace) {
                bestMember = member
                bestScore = score
                lowestWorkload = workload
            }
        }

        return bestMember?.let {
            AssignmentResult(
                assignedMemberId = it.uid,
                assignedMemberName = it.name,
                score = bestScore
            )
        }
    }
}