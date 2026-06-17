package com.collabwise.data.model

import java.time.LocalDate

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val skillIds: List<String> = emptyList()
)

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val leaderId: String = ""
)

data class Member(
    val groupId: String = "",
    val userId: String = "",
    val role: String = MemberRole.MEMBER.name,
    val joinedAt: Long = 0L
)

data class Project(
    val id: String = "",
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val status: String = "",
    val leaderId: String = "",
    val createdAt: Long = 0L
)

data class Task(
    val id: String = "",
    val projectId: String = "",
    val groupId: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = TaskStatus.LOCKED.name,
    val assignedMemberId: String = "",
    val assignedMemberName: String = "",
    val requiredSkillIds: List<String> = emptyList(),
    val dependsOn: List<String> = emptyList(),
    val dueDate: String = LocalDate.now().plusDays(1).toString(),
    val createdAt: Long = 0L
)

data class Notification(
    val id: String = "",
    val userId: String = "",
    val taskId: String = "",
    val projectId: String = "",
    val groupId: String = "",
    val taskTitle: String = "",
    val projectName: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)

data class Skill(
    val id: String = "",
    val name: String = "",
    val categoryId: String = ""
)

data class SkillCategory(
    val id: String = "",
    val name: String = "",
    val displayOrder: Int = 0
)

enum class MemberRole{
    LEADER,
    MEMBER
}

enum class TaskStatus{
    LOCKED,
    IN_PROGRESS,
    DONE,

    AVAILABLE
}

enum class ProjectStatus{
    ACTIVE,
    COMPLETED
}