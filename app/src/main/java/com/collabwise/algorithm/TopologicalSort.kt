package com.collabwise.algorithm

import com.collabwise.data.model.Task

data class TopologicalSortResult(
    val sortedTasks: List<Task>,
    val hasCycle: Boolean,
    val cycleNodes: List<String> = emptyList(),
    val missingDependencies: List<String> = emptyList()
)

object TopologicalSort {

    /**
     * Performs topological sort using Kahn's Algorithm with:
     * - cycle detection
     * - missing dependency detection
     * - deadline propagation
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
        val adjacencyList = mutableMapOf<String, MutableList<String>>()

        val missingDependencies = mutableListOf<String>()

        // Initialize graph structures
        for (task in tasks) {
            adjacencyList[task.id] = mutableListOf()
        }

        // Build graph
        for (task in tasks) {

            var validDependencyCount = 0

            for (depId in task.dependsOn) {

                if (depId in taskMap) {

                    // dependency -> dependent
                    adjacencyList[depId]?.add(task.id)
                    validDependencyCount++

                } else {

                    missingDependencies.add(
                        "Task '${task.id}' depends on missing task '$depId'"
                    )
                }
            }

            inDegree[task.id] = validDependencyCount
        }

        // Kahn's Algorithm
        val queue = ArrayDeque<String>()
        val result = mutableListOf<Task>()

        // Add all nodes with in-degree 0
        for ((taskId, degree) in inDegree) {
            if (degree == 0) {
                queue.add(taskId)
            }
        }

        while (queue.isNotEmpty()) {

            val currentId = queue.removeFirst()
            val currentTask = taskMap[currentId]

            if (currentTask != null) {
                result.add(currentTask)
            }

            for (neighborId in adjacencyList[currentId].orEmpty()) {

                val newDegree = (inDegree[neighborId] ?: 0) - 1
                inDegree[neighborId] = newDegree

                if (newDegree == 0) {
                    queue.add(neighborId)
                }
            }
        }

        // Detect cycle
        val hasCycle = result.size != tasks.size

        val cycleNodes = if (hasCycle) {
            inDegree
                .filterValues { it > 0 }
                .keys
                .toList()
        } else {
            emptyList()
        }

        // Propagate deadlines only if graph is valid
        val propagatedTasks = if (!hasCycle) {
            propagateDeadlines(result)
        } else {
            result
        }

        return TopologicalSortResult(
            sortedTasks = propagatedTasks,
            hasCycle = hasCycle,
            cycleNodes = cycleNodes,
            missingDependencies = missingDependencies.distinct()
        )
    }

    /**
     * DFS-based cycle detection.
     * Returns:
     * Pair(hasCycle, cycleNodes)
     */
    fun detectCycle(tasks: List<Task>): Pair<Boolean, List<String>> {

        val taskMap = tasks.associateBy { it.id }

        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()

        val path = mutableListOf<String>()

        for (task in tasks) {

            if (task.id !in visited) {

                val cycle = dfsHasCycle(
                    currentId = task.id,
                    taskMap = taskMap,
                    visited = visited,
                    recursionStack = recursionStack,
                    path = path
                )

                if (cycle != null) {
                    return Pair(true, cycle)
                }
            }
        }

        return Pair(false, emptyList())
    }

    /**
     * Returns the cycle path if cycle exists, otherwise null.
     */
    private fun dfsHasCycle(
        currentId: String,
        taskMap: Map<String, Task>,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>,
        path: MutableList<String>
    ): List<String>? {

        visited.add(currentId)
        recursionStack.add(currentId)
        path.add(currentId)

        val currentTask = taskMap[currentId]

        if (currentTask != null) {

            for (depId in currentTask.dependsOn) {

                // Ignore missing dependency references
                if (depId !in taskMap) {
                    continue
                }

                if (depId !in visited) {

                    val cycle = dfsHasCycle(
                        currentId = depId,
                        taskMap = taskMap,
                        visited = visited,
                        recursionStack = recursionStack,
                        path = path
                    )

                    if (cycle != null) {
                        return cycle
                    }

                } else if (depId in recursionStack) {

                    // Extract actual cycle
                    val cycleStartIndex = path.indexOf(depId)

                    return if (cycleStartIndex != -1) {
                        path.subList(cycleStartIndex, path.size).toList()
                    } else {
                        listOf(depId)
                    }
                }
            }
        }

        recursionStack.remove(currentId)
        path.removeAt(path.lastIndex)

        return null
    }

    /**
     * Propagates deadlines backward through dependency chains.
     *
     * Example:
     * A depends on B
     * B due on Friday
     * -> A should ideally finish before Friday
     */
    private fun propagateDeadlines(
        sortedTasks: List<Task>
    ): List<Task> {

        val deadlineMap = mutableMapOf<String, Long>()

        // Initialize known deadlines
        for (task in sortedTasks) {
            if (task.dueDate != null) {
                deadlineMap[task.id] = task.dueDate
            }
        }

        // Traverse backwards
        for (task in sortedTasks.asReversed()) {

            if (task.dependsOn.isEmpty()) {
                continue
            }

            val earliestDependencyDeadline = task.dependsOn
                .mapNotNull { deadlineMap[it] }
                .minOrNull()

            if (earliestDependencyDeadline != null) {

                if (task.dueDate == null) {
                    deadlineMap[task.id] = earliestDependencyDeadline
                }
            }
        }

        return sortedTasks.map { task ->

            val propagatedDeadline = deadlineMap[task.id]

            if (propagatedDeadline != null) {
                task.copy(dueDate = propagatedDeadline)
            } else {
                task
            }
        }
    }

    /**
     * Computes the critical path (longest dependency chain).
     */
    fun getCriticalPath(tasks: List<Task>): List<Task> {

        if (tasks.isEmpty()) {
            return emptyList()
        }

        val sortResult = sort(tasks)

        if (sortResult.hasCycle) {
            return emptyList()
        }

        val taskMap = tasks.associateBy { it.id }

        val distance = mutableMapOf<String, Int>()
        val predecessor = mutableMapOf<String, String?>()

        // Initialize
        for (task in sortResult.sortedTasks) {
            distance[task.id] = 0
            predecessor[task.id] = null
        }

        // Longest path in DAG
        for (task in sortResult.sortedTasks) {

            for (depId in task.dependsOn) {

                val candidateDistance = (distance[depId] ?: 0) + 1

                if (candidateDistance > (distance[task.id] ?: 0)) {

                    distance[task.id] = candidateDistance
                    predecessor[task.id] = depId
                }
            }
        }

        // Find furthest node
        val endTaskId = distance.maxByOrNull { it.value }?.key
            ?: return emptyList()

        // Reconstruct path
        val criticalPath = mutableListOf<Task>()

        var currentId: String? = endTaskId

        while (currentId != null) {

            val task = taskMap[currentId]

            if (task != null) {
                criticalPath.add(0, task)
            }

            currentId = predecessor[currentId]
        }

        return criticalPath
    }
}