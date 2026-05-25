package com.collabwise.algorithm

import com.collabwise.data.model.Notification
import com.collabwise.data.model.Task

data class UnlockResult(
    val unlockedTasks: List<Task>,
    val notifications: List<Notification>
)

object BfsUnlocker {

    /**
     * Performs unlock propagation after a task is completed.
     *
     * Workflow:
     * - marks the completed task as completed
     * - checks tasks directly depending on it
     * - unlocks tasks whose prerequisites are now fully satisfied
     * - generates notifications for assigned users
     *
     * Time Complexity:
     * O(n * d)
     * where:
     * - n = number of tasks
     * - d = average dependency count
     */
    fun propagate(
        completedTaskId: String,
        allTasks: List<Task>,
        completedTaskIds: Set<String> = emptySet()
    ): UnlockResult {

        // Include the newly completed task
        val completed = completedTaskIds + completedTaskId

        val unlockedTasks = mutableListOf<Task>()
        val notifications = mutableListOf<Notification>()

        for (task in allTasks) {

            // Skip already completed tasks
            if (task.id in completed) {
                continue
            }

            // Only check tasks affected by the completed task
            if (completedTaskId !in task.dependsOn) {
                continue
            }

            // Unlock only if ALL prerequisites are completed
            if (checkAllPrerequisitesMet(task, completed)) {

                unlockedTasks.add(task)

                if (task.assignedMemberId != null) {
                    notifications.add(
                        buildTaskUnlockedNotification(task)
                    )
                }
            }
        }

        return UnlockResult(
            unlockedTasks = unlockedTasks,
            notifications = notifications
        )
    }

    /**
     * Checks whether all prerequisite task IDs
     * exist inside the completed task set.
     *
     * Returns:
     * - true if every dependency is completed
     * - false otherwise
     *
     * Time Complexity:
     * O(d)
     * where d = dependency count
     */
    fun checkAllPrerequisitesMet(
        task: Task,
        completedTaskIds: Set<String>
    ): Boolean {

        if (task.dependsOn.isEmpty()) {
            return true
        }

        return task.dependsOn.all { depId ->
            depId in completedTaskIds
        }
    }

    /**
     * Builds a detailed prerequisite completion report
     * for UI display, analytics, or debugging.
     *
     * Includes:
     * - prerequisite task titles
     * - completion status
     * - completed count summary
     *
     * Missing dependencies are labeled as "Unknown".
     *
     * Time Complexity:
     * O(d)
     */
    fun checkPrerequisitesStatus(
        task: Task,
        completedTaskIds: Set<String>,
        taskMap: Map<String, Task>
    ): PrerequisiteStatus {

        val prerequisites = mutableListOf<PrerequisiteInfo>()

        for (depId in task.dependsOn) {

            val depTask = taskMap[depId]

            prerequisites.add(
                PrerequisiteInfo(
                    taskId = depId,
                    taskTitle = depTask?.title ?: "Unknown",
                    isMet = depId in completedTaskIds
                )
            )
        }

        return PrerequisiteStatus(
            taskId = task.id,
            allPrerequisitesMet = prerequisites.all { it.isMet },
            prerequisites = prerequisites,
            completedCount = prerequisites.count { it.isMet },
            totalCount = prerequisites.size
        )
    }

    /**
     * Finds all currently unlockable tasks.
     *
     * A task is unlockable if:
     * - it is not completed
     * - all prerequisites are completed
     *
     * Useful for:
     * - dashboard displays
     * - backlog filtering
     * - progress tracking
     *
     * Time Complexity:
     * O(n * d)
     */
    fun findUnlockableTasks(
        allTasks: List<Task>,
        completedTaskIds: Set<String>
    ): List<Task> {

        return allTasks.filter { task ->

            task.id !in completedTaskIds &&
                    checkAllPrerequisitesMet(task, completedTaskIds)
        }
    }

    /**
     * Creates a notification object for an unlocked task.
     *
     * Notification is intended for the assigned member.
     *
     * Time Complexity:
     * O(1)
     */
    private fun buildTaskUnlockedNotification(task: Task): Notification {

        return Notification(
            userId = task.assignedMemberId ?: "",
            taskId = task.id,
            message = buildNotificationMessage(task),
            isRead = false
        )
    }

    /**
     * Builds the default unlock notification message.
     *
     * Example:
     * "Task 'Setup Backend' is now unlocked and ready to work on!"
     *
     * Time Complexity:
     * O(1)
     */
    private fun buildNotificationMessage(task: Task): String {

        return "Task \"${task.title}\" is now unlocked and ready to work on!"
    }


    /**
     * Fluent builder for manually creating notifications.
     *
     * Supports:
     * - custom messages
     * - task-linked notifications
     * - user-targeted notifications
     *
     * Useful for:
     * - admin notifications
     * - system events
     * - workflow alerts
     */
    class NotificationBuilder {

        private var userId: String = ""
        private var taskId: String = ""
        private var message: String = ""
        private var customMessage: Boolean = false

        fun forUser(userId: String) = apply {
            this.userId = userId
        }

        fun forTask(taskId: String, taskTitle: String = "") = apply {

            this.taskId = taskId

            if (!customMessage && taskTitle.isNotEmpty()) {
                this.message = "Task \"$taskTitle\" is now unlocked!"
            }
        }

        fun withMessage(message: String) = apply {

            this.message = message
            this.customMessage = true
        }

        fun build(): Notification {

            return Notification(
                userId = userId,
                taskId = taskId,
                message = message,
                isRead = false
            )
        }
    }

    /**
     * Creates a new notification builder instance.
     *
     * Time Complexity:
     * O(1)
     */

    fun notificationBuilder(): NotificationBuilder {

        return NotificationBuilder()
    }

    /**
     * Generates a pseudo-unique notification ID.
     *
     * Format:
     * notif_<timestamp>_<randomNumber>
     *
     * NOTE:
     * Suitable for client-side generation,
     * but server-generated UUIDs are preferred
     * for distributed systems.
     *
     * Time Complexity:
     * O(1)
     */
    private fun generateNotificationId(): String {

        return "notif_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
    }
}

data class PrerequisiteInfo(
    val taskId: String,
    val taskTitle: String,
    val isMet: Boolean
)

data class PrerequisiteStatus(
    val taskId: String,
    val allPrerequisitesMet: Boolean,
    val prerequisites: List<PrerequisiteInfo>,
    val completedCount: Int,
    val totalCount: Int
)
