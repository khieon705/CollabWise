package com.collabwise.algorithm

import com.collabwise.data.model.Task
import com.collabwise.data.model.TaskStatus

data class UnlockNotification(
    val memberId: String,
    val taskId: String,
    val taskTitle: String,
    val message: String
)

data class UnlockResult(
    val unlockedTaskIds: List<String>,
    val notifications: List<UnlockNotification>
)

object BfsUnlocker {

    /**
     * True BFS Unlock Propagation
     *
     * Traverses downstream dependency chains and unlocks tasks
     * whose prerequisites are fully completed.
     */
    fun propagate(
        completedTaskId: String,
        allTasks: List<Task>
    ): UnlockResult {

        val taskMap = allTasks.associateBy { it.id }

        /**
         * Build adjacency list:
         *
         * prerequisiteTaskId -> dependent tasks
         */
        val adjacency = mutableMapOf<String, MutableList<Task>>()

        for (task in allTasks) {
            for (dependencyId in task.dependsOn) {
                adjacency
                    .getOrPut(dependencyId) { mutableListOf() }
                    .add(task)
            }
        }

        val queue = ArrayDeque<String>()
        val visited = mutableSetOf<String>()

        val unlockedTaskIds = mutableListOf<String>()
        val notifications = mutableListOf<UnlockNotification>()

        queue.add(completedTaskId)

        while (queue.isNotEmpty()) {
            val currentCompletedId = queue.removeFirst()

            if (!visited.add(currentCompletedId)) {
                continue
            }

            val downstreamTasks = adjacency[currentCompletedId].orEmpty()

            for (task in downstreamTasks) {
                // Ignore already active/completed tasks
                if (task.status != TaskStatus.LOCKED.name) {
                    continue
                }

                val allPrerequisitesDone =
                    task.dependsOn.all { prereqId ->

                        val prereqTask = taskMap[prereqId]

                        prereqTask?.status == TaskStatus.DONE.name ||
                                prereqId == completedTaskId
                    }

                if (allPrerequisitesDone) {
                    unlockedTaskIds.add(task.id)

                    if (!task.assignedMemberId.isNullOrBlank()) {
                        notifications.add(
                            UnlockNotification(
                                memberId = task.assignedMemberId,
                                taskId = task.id,
                                taskTitle = task.title,
                                message =
                                    "Task \"${task.title}\" is now ready to start."
                            )
                        )
                    }

                    /**
                     * Continue BFS propagation.
                     *
                     * This supports chains like:
                     * A -> B -> C
                     */
                    queue.add(task.id)
                }
            }
        }

        return UnlockResult(
            unlockedTaskIds = unlockedTaskIds.distinct(),
            notifications = notifications
        )
    }
}