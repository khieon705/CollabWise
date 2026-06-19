package com.collabwise.algorithm

import android.util.Log
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
     * Calculate how many skills match sa bawat member.
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

        // To record the overall ranking when assigning a task
        val rankings = mutableListOf<Triple<User, Int, Int>>()

        for (member in members) {
            val score = skillMatchScore(task, member)
            val workload = taskCountMap[member.uid] ?: 0

            rankings.add(Triple(member, score, workload))

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

        Log.d("GreedyScheduler","=== Task: ${task.title} Ranking ===")

        rankings
            .sortedWith(
                compareByDescending<Triple<User, Int, Int>> { it.second }
                    .thenBy { it.third }
            )
            .forEachIndexed { index, (member, score, workload) ->
                Log.d("GreedyScheduler", "${index + 1}. ${member.name} | Skill Score: $score | Current Tasks: $workload")
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