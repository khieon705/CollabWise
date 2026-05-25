package com.collabwise.algorithm

import com.collabwise.data.model.Task

object MergeSort {

    fun sort(tasks: List<Task>): List<Task> {

        if (tasks.size <= 1) return tasks

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
            result.add(left[i])
            i++
        }

        while (j < right.size) {
            result.add(right[j])
            j++
        }

        return result
    }

    /*
     * Comparison Rules:
     * 1. Earlier due date first
     * 2. TODO before IN_PROGRESS before DONE
     */
    private fun compare(a: Task, b: Task): Boolean {

        val aDue = a.dueDate ?: Long.MAX_VALUE
        val bDue = b.dueDate ?: Long.MAX_VALUE

        // earlier due date wins
        if (aDue != bDue) {
            return aDue < bDue
        }

        // compare status priority
        return statusRank(a.status) < statusRank(b.status)
    }

    private fun statusRank(status: String): Int {

        return when (status) {
            "TODO" -> 0
            "IN_PROGRESS" -> 1
            "DONE" -> 2
            else -> 3
        }
    }
}