package com.collabwise.ui.project

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.collabwise.ui.components.StatusChip
import com.collabwise.ui.components.UserAvatar

private data class TaskSheetColors(
    val primary: Color,
    val primaryContainer: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textSecondary: Color,
    val handle: Color
)

@Composable
private fun taskSheetColors(): TaskSheetColors {
    val dark = isSystemInDarkTheme()

    return if (dark) {
        TaskSheetColors(
            primary = MaterialTheme.colorScheme.primary,
            primaryContainer = MaterialTheme.colorScheme.primaryContainer,
            success = Color(0xFF4ADE80),
            successContainer = Color(0xFF052E16),
            warning = Color(0xFFFBBF24),
            warningContainer = Color(0xFF451A03),
            error = MaterialTheme.colorScheme.error,
            errorContainer = MaterialTheme.colorScheme.errorContainer,
            surfaceVariant = MaterialTheme.colorScheme.surfaceVariant,
            border = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            textSecondary = MaterialTheme.colorScheme.onSurfaceVariant,
            handle = Color(0xFF52525B)
        )
    } else {
        TaskSheetColors(
            primary = MaterialTheme.colorScheme.primary,
            primaryContainer = Color(0xFFEFF6FF),
            success = Color(0xFF15803D),
            successContainer = Color(0xFFDCFCE7),
            warning = Color(0xFFB45309),
            warningContainer = Color(0xFFFEF3C7),
            error = MaterialTheme.colorScheme.error,
            errorContainer = Color(0xFFFEE2E2),
            surfaceVariant = Color(0xFFF8FAFC),
            border = Color(0xFFE2E8F0),
            textSecondary = Color(0xFF64748B),
            handle = Color(0xFFCBD5E1)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    prerequisites: List<Task>,
    allTasks: List<Task>,
    isLeader: Boolean,
    currentUid: String,
    onDismiss: () -> Unit,
    onStatusChange: (taskId: String, newStatus: TaskStatus) -> Unit,
    onAddDep: (taskId: String, dependsOnId: String) -> Unit,
    onRemoveDep: (taskId: String, dependsOnId: String) -> Unit,
    onDelete: (taskId: String) -> Unit
) {
    val colors = taskSheetColors()

    val isAssignedToMe = task.assignedMemberId == currentUid
    val urgency = task.dueDateUrgency()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddDepPicker by remember { mutableStateOf(false) }

    val eligibleToAdd = allTasks.filter {
        it.id != task.id && it.id !in task.dependsOn
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Surface(
                color = colors.handle,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
            ) {}
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {

                        StatusChip(status = task.status)

                        if (task.dueDate != null) {

                            Surface(
                                color = urgency.bgColor(),
                                shape = RoundedCornerShape(20.dp)
                            ) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 3.dp
                                    )
                                ) {

                                    Icon(
                                        imageVector =
                                            if (urgency == DueDateUrgency.OVERDUE)
                                                Icons.Default.Warning
                                            else
                                                Icons.Default.CalendarToday,

                                        contentDescription = null,
                                        tint = urgency.color(),
                                        modifier = Modifier.size(11.dp)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = task.dueDateLabel(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = urgency.color()
                                    )
                                }
                            }
                        }
                    }
                }

                if (isLeader) {

                    IconButton(
                        onClick = { showDeleteConfirm = true }
                    ) {

                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete task",
                            tint = colors.textSecondary
                        )
                    }
                }
            }

            if (task.description.isNotBlank()) {

                Text(
                    text = task.description,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(
                        top = 4.dp,
                        bottom = 16.dp
                    )
                )

            } else {

                Spacer(modifier = Modifier.height(16.dp))
            }

            HorizontalDivider(color = colors.border)

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(
                icon = Icons.Default.Person,
                label = "Assigned to",
                textColor = colors.textSecondary
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    UserAvatar(
                        name = task.assignedMemberName,
                        size = 24
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = task.assignedMemberName +
                                if (isAssignedToMe) " (you)" else "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (task.requiredSkillIds.isNotEmpty()) {

                DetailRow(
                    icon = Icons.Default.Star,
                    label = "Required skills",
                    textColor = colors.textSecondary
                ) {

                    Surface(
                        color = colors.surfaceVariant,
                        shape = RoundedCornerShape(20.dp)
                    ) {

                        Text(
                            text = "${task.requiredSkillIds.size} skill${if (task.requiredSkillIds.size != 1) "s" else ""} required",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            DetailRow(
                icon = Icons.Default.Link,
                label = "Prerequisites",
                textColor = colors.textSecondary
            ) {

                if (prerequisites.isEmpty()) {

                    Text(
                        text = "None",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }

            if (prerequisites.isNotEmpty()) {

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 28.dp)
                ) {

                    prerequisites.forEach { prereq ->

                        PrerequisiteRow(
                            task = prereq,
                            canRemove = isLeader,
                            textColor = colors.textSecondary,
                            onRemove = {
                                onRemoveDep(task.id, prereq.id)
                            }
                        )
                    }
                }
            }

            if (isLeader && eligibleToAdd.isNotEmpty()) {

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAddDepPicker = !showAddDepPicker
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                Icons.Default.AddLink,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Add prerequisite",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                if (showAddDepPicker)
                                    Icons.Default.ExpandLess
                                else
                                    Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                        }

                        AnimatedVisibility(visible = showAddDepPicker) {

                            Column {

                                HorizontalDivider(color = colors.border)

                                Column(
                                    modifier = Modifier.padding(
                                        horizontal = 14.dp,
                                        vertical = 10.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {

                                    Text(
                                        text = "Select tasks that must be completed before this one.",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )

                                    eligibleToAdd.forEach { dependency ->

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onAddDep(task.id, dependency.id)
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            StatusChip(status = dependency.status)

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = dependency.title,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add dependency",
                                                tint = colors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = colors.border,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            AnimatedVisibility(
                visible = task.status == TaskStatus.LOCKED.name
            ) {

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colors.warningContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {

                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {

                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = colors.warning,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {

                            Text(
                                text = "Task is blocked",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = colors.warning
                            )

                            val pendingCount = prerequisites.count {
                                it.status != TaskStatus.DONE.name
                            }

                            Text(
                                text = "$pendingCount prerequisite${if (pendingCount != 1) "s" else ""} must be completed before you can start this task.",
                                fontSize = 12.sp,
                                color = colors.warning
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = urgency == DueDateUrgency.OVERDUE &&
                        task.status != TaskStatus.DONE.name
            ) {

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colors.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {

                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {

                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "This task is past its due date. Please complete it as soon as possible.",
                            fontSize = 12.sp,
                            color = colors.error
                        )
                    }
                }
            }

            if (isAssignedToMe) {

                when (task.status) {

                    TaskStatus.AVAILABLE.name -> {

                        Button(
                            onClick = {
                                onStatusChange(task.id, TaskStatus.IN_PROGRESS)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {

                            Icon(
                                Icons.Default.PlayArrow,
                                null,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Start this task",
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }

                    TaskStatus.IN_PROGRESS.name -> {

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            Button(
                                onClick = {
                                    onStatusChange(task.id, TaskStatus.DONE)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.success
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {

                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Mark as done",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = colors.successContainer
                                )
                            ) {

                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {

                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        null,
                                        tint = colors.success,
                                        modifier = Modifier.size(14.dp)
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = "Marking done will automatically notify members whose tasks were waiting on this one.",
                                        fontSize = 12.sp,
                                        color = colors.success
                                    )
                                }
                            }
                        }
                    }

                    TaskStatus.DONE.name -> {

                        Surface(
                            color = colors.successContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(14.dp)
                            ) {

                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = colors.success,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Task completed",
                                    color = colors.success,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    TaskStatus.LOCKED.name -> Unit
                }
            }

            if (!isAssignedToMe && task.status != TaskStatus.DONE.name) {

                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Only ${task.assignedMemberName} can update this task's status.",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrerequisiteRow(
    task: Task,
    canRemove: Boolean,
    textColor: Color,
    onRemove: () -> Unit
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {

        StatusChip(status = task.status)

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = task.title,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (canRemove) {

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {

                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove dependency",
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    textColor: Color,
    content: @Composable () -> Unit
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$label: ",
            fontSize = 13.sp,
            color = textColor
        )

        content()
    }
}