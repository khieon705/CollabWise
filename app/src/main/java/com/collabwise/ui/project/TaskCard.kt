package com.collabwise.ui.project

import com.collabwise.ui.components.StatusChip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collabwise.data.model.Task
import com.collabwise.data.model.TaskStatus
import com.collabwise.ui.components.UserAvatar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ── Due date urgency helpers ──────────────────────────────────────────────────

enum class DueDateUrgency { OVERDUE, TODAY, SOON, UPCOMING, NONE }

fun Task.dueDateUrgency(): DueDateUrgency {
    val due = dueDate
    val daysLeft = ChronoUnit.DAYS.between(
        LocalDate.now(),
        LocalDate.parse(due)
    ).toInt()
    return when {
        daysLeft < 0  -> DueDateUrgency.OVERDUE
        daysLeft == 0 -> DueDateUrgency.TODAY
        daysLeft <= 3 -> DueDateUrgency.SOON
        else          -> DueDateUrgency.UPCOMING
    }
}

fun Task.dueDateLabel(): String {
    val due = dueDate
    val daysLeft = ChronoUnit.DAYS.between(
        LocalDate.now(),
        LocalDate.parse(due)
    ).toInt()
    return when {
        daysLeft < -1 -> "Overdue by ${-daysLeft}d"
        daysLeft == -1 -> "Overdue by 1d"
        daysLeft == 0  -> "Due today"
        daysLeft == 1  -> "Due tomorrow"
        daysLeft <= 7  -> "Due in ${daysLeft}d"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
            "Due ${due.format(formatter)}"
        }
    }
}

fun DueDateUrgency.color(): Color = when (this) {
    DueDateUrgency.OVERDUE  -> Color(0xFFC5221F)
    DueDateUrgency.TODAY    -> Color(0xFFB06000)
    DueDateUrgency.SOON     -> Color(0xFFB06000)
    DueDateUrgency.UPCOMING -> Color(0xFF137333)
    DueDateUrgency.NONE     -> Color(0xFF5F6368)
}

fun DueDateUrgency.bgColor(): Color = when (this) {
    DueDateUrgency.OVERDUE  -> Color(0xFFFCE8E8)
    DueDateUrgency.TODAY    -> Color(0xFFFEF3DC)
    DueDateUrgency.SOON     -> Color(0xFFFEF3DC)
    DueDateUrgency.UPCOMING -> Color(0xFFE6F4EA)
    DueDateUrgency.NONE     -> Color(0xFFF1F3F4)
}

// ── TaskCard ──────────────────────────────────────────────────────────────────

@Composable
fun TaskCard(
    task: Task,
    isAssignedToMe: Boolean,
    onClick: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit
) {
    val urgency = task.dueDateUrgency()

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape     = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isAssignedToMe && task.status == TaskStatus.IN_PROGRESS.name)
                Color(0xFFF8FBFF) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Top row: icon + title + status chip ───────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                // Task icon circle
                Surface(
                    color  = taskIconBg(task.status) as Color,
                    shape  = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector        = taskIcon(task.status) as ImageVector,
                            contentDescription = null,
                            tint               = taskIconTint(task.status) as Color,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = task.title,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 14.sp,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                    // Assigned member
                    if (task.assignedMemberName!!.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(top = 3.dp)
                        ) {
                            UserAvatar(name = task.assignedMemberName, size = 16)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text  = task.assignedMemberName +
                                        if (isAssignedToMe) " (you)" else "",
                                fontSize = 12.sp,
                                color    = Color(0xFF5F6368)
                            )
                        }
                    }
                }

                StatusChip(status = task.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Bottom row: due date + dep count + quick actions ──────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth()
            ) {
                // Due date chip
                Surface(
                    color = urgency.bgColor(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector        = if (urgency == DueDateUrgency.OVERDUE)
                                Icons.Default.Warning else Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint               = urgency.color(),
                            modifier           = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text       = task.dueDateLabel(),
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color      = urgency.color()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))

                // Dependency indicator
                if (task.dependsOn.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFF1F3F4),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Link,
                                contentDescription = null,
                                tint               = Color(0xFF5F6368),
                                modifier           = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text     = "${task.dependsOn.size} dep${if (task.dependsOn.size > 1) "s" else ""}",
                                fontSize = 11.sp,
                                color    = Color(0xFF5F6368)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Quick action buttons — only for the assigned member
                if (isAssignedToMe) {
                    when (task.status) {
                        TaskStatus.AVAILABLE.name -> {
                            FilledTonalButton(
                                onClick       = { onStatusChange(TaskStatus.IN_PROGRESS) },
                                colors        = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFE8F0FE),
                                    contentColor   = Color(0xFF1967D2)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier       = Modifier.height(32.dp)
                            ) {
                                Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        TaskStatus.IN_PROGRESS.name -> {
                            FilledTonalButton(
                                onClick       = { onStatusChange(TaskStatus.DONE) },
                                colors        = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFE6F4EA),
                                    contentColor   = Color(0xFF137333)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier       = Modifier.height(32.dp)
                            ) {
                                Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        TaskStatus.LOCKED.name -> {
                            Surface(
                                color = Color(0xFFFEF3DC),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text     = "Waiting",
                                    fontSize = 11.sp,
                                    color    = Color(0xFFB06000),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        else -> { /* DONE — no action */ }
                    }
                }
            }
        }
    }
}

// ── Icon helpers ──────────────────────────────────────────────────────────────

private fun taskIcon(status: String) = when (status) {
    TaskStatus.AVAILABLE.name   -> Icons.Default.TaskAlt
    TaskStatus.IN_PROGRESS.name -> Icons.Default.PlayCircle
    TaskStatus.LOCKED.name     -> Icons.Default.Lock
    TaskStatus.DONE.name        -> Icons.Default.CheckCircle
    else -> {}
}

private fun taskIconTint(status: String) = when (status) {
    TaskStatus.AVAILABLE.name   -> Color(0xFF1967D2)
    TaskStatus.IN_PROGRESS.name -> Color(0xFF137333)
    TaskStatus.LOCKED.name     -> Color(0xFFB06000)
    TaskStatus.DONE.name        -> Color(0xFF5F6368)
    else -> {}
}

private fun taskIconBg(status: String) = when (status) {
    TaskStatus.AVAILABLE.name   -> Color(0xFFE8F0FE)
    TaskStatus.IN_PROGRESS.name -> Color(0xFFE6F4EA)
    TaskStatus.LOCKED.name     -> Color(0xFFFEF3DC)
    TaskStatus.DONE.name        -> Color(0xFFF1F3F4)
    else -> {}
}