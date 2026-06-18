package com.collabwise.algorithm

import android.util.Log
import com.collabwise.data.model.Task

data class ValidationResult(
    val isValid: Boolean,
    val message: String? = null
)

data class TopologicalSortResult(
    val sortedTasks: List<Task>,
    val hasCycle: Boolean,
    val cycleNodes: List<String> = emptyList()
)

object TopologicalSort {

    /**
     * Kahn's Algorithm
     */
    fun sort(tasks: List<Task>): TopologicalSortResult {
        if (tasks.isEmpty()) {
            return TopologicalSortResult(
                sortedTasks = emptyList(),
                hasCycle = false
            )
        }

        val taskMap = tasks.associateBy { it.id }

        val inDegree = mutableMapOf<String, Int>()
        val adjacency = mutableMapOf<String, MutableList<String>>()

        for (task in tasks) {
            inDegree[task.id] = 0
            adjacency[task.id] = mutableListOf()
        }

        for (task in tasks) {
            for (dependencyId in task.dependsOn) {
                if (dependencyId !in taskMap) {
                    continue
                }

                adjacency[dependencyId]?.add(task.id)

                inDegree[task.id] = (inDegree[task.id] ?: 0) + 1
            }
        }

        val queue = ArrayDeque<String>()

        for ((taskId, degree) in inDegree) {
            if (degree == 0) {
                queue.add(taskId)
            }
        }

        val result = mutableListOf<Task>()

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val currentTask = taskMap[currentId]

            if (currentTask != null) {
                result.add(currentTask)
            }

            for (neighborId in adjacency[currentId].orEmpty()) {
                inDegree[neighborId] = (inDegree[neighborId] ?: 0) - 1

                if (inDegree[neighborId] == 0) {
                    queue.add(neighborId)
                }
            }
        }

        val hasCycle = result.size != tasks.size

        val cycleNodes =
            if (hasCycle) {
                inDegree
                    .filterValues { it > 0 }
                    .keys
                    .toList()
            } else {
                emptyList()
            }

        return TopologicalSortResult(
            sortedTasks = result,
            hasCycle = hasCycle,
            cycleNodes = cycleNodes
        )
    }

    /**
     * Used by ViewModel before adding dependencies.
     */
    fun validate(
        tasks: List<Task>,
        newEdge: Pair<String, String>? = null
    ): ValidationResult {

        val updatedTasks =
            if (newEdge != null) {
                val (taskId, dependsOnId) = newEdge

                tasks.map { task ->
                    if (task.id == taskId) {
                        task.copy(
                            dependsOn =
                                task.dependsOn + dependsOnId
                        )
                    } else {
                        task
                    }
                }
            } else {
                tasks
            }

        val result = sort(updatedTasks)

        return if (result.hasCycle) {
            Log.e(
                "TopologicalSort",
                "Invalid dependency detected. Adding this dependency would create a cycle: ${
                    result.cycleNodes.joinToString(" -> ")
                }"
            )

            ValidationResult(
                isValid = false,
                message =
                    "Dependency cycle detected: ${
                        result.cycleNodes.joinToString(" → ")
                    }"
            )
        } else {
            ValidationResult(
                isValid = true
            )
        }
    }
}