package com.collabwise.algorithm

import com.collabwise.data.model.Task
import com.collabwise.data.model.User

object GreedyScheduler {
    // Binary Search for checking if the user has the required skills for the task
    fun binarySearch(skills: List<String>, target: String): Boolean {
        var left = 0
        var right = skills.size - 1

        while (left <= right) {
            val mid = (left + right) / 2
            val comparison = skills[mid].compareTo(target)

            when {
                comparison == 0 -> return true
                comparison < 0 -> left = mid + 1
                else -> right = mid - 1
            }
        }

        return false
    }

    // Compute for the score of the user for the task compatibility
    fun skillMatchScore(task: Task, user: User): Int {
        val sortedSkills = user.skillIds.sorted()
        var score = 0

        for (skill in task.requiredSkillIds) {
            if (binarySearch(sortedSkills, skill)) {
                score++
            }
        }

        return score
    }

    // Sort task by priority and due date
    fun quickSort(tasks: MutableList<Task>, low: Int, high: Int) {
        if (low < high) {
            val pivotIndex = partition(tasks, low, high)
            quickSort(tasks, low, pivotIndex - 1)
            quickSort(tasks, pivotIndex + 1, high)
        }
    }

    private fun partition(
        tasks: MutableList<Task>,
        low: Int,
        high: Int
    ): Int {
        val pivot = tasks[high]
        var i = low - 1

        for (j in low until high) {
            val current = tasks[j]
            val earlierDeadline = (current.dueDate ?: Long.MAX_VALUE) < (pivot.dueDate ?: Long.MAX_VALUE)

            if (earlierDeadline) {
                i++
                val temp = tasks[i]
                tasks[i] = tasks[j]
                tasks[j] = temp
            }
        }

        val temp = tasks[i + 1]
        tasks[i + 1] = tasks[high]
        tasks[high] = temp

        return i + 1
    }

    // Assign the best user to each task
    // Update: assign task to the one with lowest workload when multiple person have the same skill score match
    fun assign(
        tasks: MutableList<Task>,
        users: List<User>
    ): List<Task> {
        quickSort(tasks, 0, tasks.size - 1)

        // Track workload per user
        val workload = users.associate { user ->
            user.uid to tasks.count { it.assignedMemberId == user.uid }
        }.toMutableMap()

        return tasks.map { task ->
            if (task.assignedMemberId != null) {
                return@map task
            }

            var bestUser: User? = null
            var bestScore = -1
            var lowestWorkload = Int.MAX_VALUE

            for (user in users) {
                val score = skillMatchScore(task, user)
                val currentLoad = workload[user.uid] ?: 0

                if (
                    score > bestScore ||
                    (score == bestScore && currentLoad < lowestWorkload)
                ) {
                    bestScore = score
                    bestUser = user
                    lowestWorkload = currentLoad
                }
            }

            if (bestUser != null) {

                workload[bestUser.uid] =
                    (workload[bestUser.uid] ?: 0) + 1

                task.copy(
                    assignedMemberId = bestUser.uid
                )

            } else {
                task
            }
        }
    }

    /* sample firestore update
    val updatedTasks = GreedyScheduler.assign(tasks, users)

for (task in updatedTasks) {

    firestore.collection("tasks")
        .document(task.id)
        .set(task)
}

TO BE WORKFLOW
tasks
→ auto assign
→ updatedTasks
→ save to Firestore
→ UI listens to Firestore
→ UI updates automatically
     */

}