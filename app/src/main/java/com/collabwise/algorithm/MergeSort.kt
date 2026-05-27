package com.collabwise.algorithm

import com.collabwise.data.model.Task
import com.collabwise.data.model.TaskStatus
import java.time.LocalDate

object MergeSort {

    fun sort(tasks: List<Task>): List<Task> {
        if (tasks.size <= 1) {
            return tasks
        }

        val mid = tasks.size / 2

        val left = sort(tasks.subList(0, mid))
        val right = sort(tasks.subList(mid, tasks.size))

        return merge(left, right)
    }

    private fun merge(
        left: List<Task>,
        right: List<Task>
    ): List<Task> {

        val result = mutableListOf<Task>()

        var i = 0
        var j = 0

        while (i < left.size && j < right.size) {
            if (compare(left[i], right[j])) {
                result.add(left[i])
                i++
            } else {
                result.add(right[j])
                j++
            }
        }

        while (i < left.size) {
            result.add(left[i++])
        }

        while (j < right.size) {
            result.add(right[j++])
        }

        return result
    }

    /**
     * Sorting Rules
     *
     * 1. IN_PROGRESS first
     * 2. LOCKED second
     * 3. DONE last
     * 4. Earlier due date first
     */
    private fun compare(
        a: Task,
        b: Task
    ): Boolean {

        val aRank = statusRank(a.status)
        val bRank = statusRank(b.status)

        if (aRank != bRank) {
            return aRank < bRank
        }

        val aDue = runCatching { LocalDate.parse(a.dueDate) }
            .getOrDefault(LocalDate.MAX)

        val bDue = runCatching { LocalDate.parse(b.dueDate) }
            .getOrDefault(LocalDate.MAX)

        return aDue < bDue
    }

    private fun statusRank(status: String): Int {
        return when (status) {
            TaskStatus.IN_PROGRESS.name -> 0
            TaskStatus.LOCKED.name -> 1
            TaskStatus.DONE.name -> 2
            else -> 3
        }
    }
}