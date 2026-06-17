package com.collabwise.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.collabwise.data.model.Task
import com.collabwise.data.model.TaskStatus
import com.collabwise.data.model.User
import com.collabwise.ui.components.EmptyState
import com.collabwise.ui.components.LoadingOverlay
import com.collabwise.ui.components.StatusChip
import com.collabwise.ui.components.UserAvatar
import com.collabwise.viewmodel.ProjectViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    onBack: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    var showProjectInfo by remember {
        mutableStateOf(false)
    }

    val tabs = listOf("Tasks", "Members")
    val snackbarHost = remember { SnackbarHostState() }

    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = colorScheme.background,

        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {

                    Text(
                        text = uiState.project?.name ?: "",
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    uiState.group?.name?.let { groupName ->
                        Text(
                            text = groupName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = {
                        showProjectInfo = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Project details",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },

        floatingActionButton = {
            if (uiState.isLeader && selectedTab == 0) {
                FloatingActionButton(
                    onClick = { viewModel.showCreateTask() },
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add task"
                    )
                }
            }
        },

        snackbarHost = {
            SnackbarHost(snackbarHost)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colorScheme.surface
            ) {

                tabs.forEachIndexed { index, title ->

                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },

                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {

                                Text(title)

                                val count = when (index) {
                                    0 -> uiState.tasks.size
                                    1 -> uiState.members.size
                                    else -> 0
                                }

                                if (count > 0) {

                                    Surface(
                                        color =
                                            if (selectedTab == index)
                                                colorScheme.primaryContainer
                                            else
                                                colorScheme.surfaceVariant,

                                        shape = RoundedCornerShape(20.dp)
                                    ) {

                                        Text(
                                            text = count.toString(),
                                            fontSize = 11.sp,

                                            color =
                                                if (selectedTab == index)
                                                    colorScheme.onPrimaryContainer
                                                else
                                                    colorScheme.onSurfaceVariant,

                                            modifier = Modifier.padding(
                                                horizontal = 7.dp,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> LoadingOverlay()

                selectedTab == 0 -> {
                    TasksTab(
                        tasks = uiState.tasks,
                        currentUid = uiState.currentUser?.uid ?: "",
                        isLeader = uiState.isLeader,
                        onTaskClick = { viewModel.selectTask(it) },
                        onStatusChange = { id, status ->
                            viewModel.updateTaskStatus(id, status)
                        }
                    )
                }

                else -> {
                    MembersTab(
                        tasksByMember = viewModel.tasksByMember()
                    )
                }
            }
        }
    }

    if (uiState.showCreateTask) {
        CreateTaskSheet(
            skillsGrouped = uiState.skillsGrouped,
            eligibleDependencies = viewModel.getEligibleDependencies(""),
            isLoading = uiState.isCreatingTask,
            createTaskError = uiState.createTaskError,
            cycleError = uiState.cycleError,
            onDismiss = { viewModel.dismissCreateTask() },

            onCreate = { title, desc, skillIds, dueDate, dependsOn ->
                viewModel.createTask(
                    title,
                    desc,
                    skillIds,
                    LocalDate.parse(dueDate),
                    dependsOn
                )
            }
        )
    }

    if (uiState.showTaskDetail && uiState.selectedTask != null) {
        TaskDetailSheet(
            task = uiState.selectedTask!!,
            prerequisites = viewModel.getPrerequisites(uiState.selectedTask!!),
            allTasks = uiState.tasks,
            isLeader = uiState.isLeader,
            currentUid = uiState.currentUser?.uid ?: "",
            onDismiss = { viewModel.dismissTaskDetail() },
            onStatusChange = { id, status ->
                viewModel.updateTaskStatus(id, status)
            },
            onAddDep = { taskId, depId ->
                viewModel.addDependency(taskId, depId)
            },
            onRemoveDep = { taskId, depId ->
                viewModel.removeDependency(taskId, depId)
            },
            onDelete = { viewModel.deleteTask(it) }
        )
    }

    if (showProjectInfo) {

        AlertDialog(
            onDismissRequest = {
                showProjectInfo = false
            },

            title = {
                Text("Project Details")
            },

            text = {
                Text(
                    text = uiState.project?.description
                        ?.takeIf { it.isNotBlank() }
                        ?: "No description provided."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showProjectInfo = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun TasksTab(
    tasks: List<Task>,
    currentUid: String,
    isLeader: Boolean,
    onTaskClick: (Task) -> Unit,
    onStatusChange: (String, TaskStatus) -> Unit
) {

    if (tasks.isEmpty()) {

        EmptyState(
            emoji = "\uD83D\uDCCB",
            title = "No tasks yet",

            subtitle =
                if (isLeader)
                    "Tap + to add the first task. It will be auto-assigned immediately."
                else
                    "No tasks have been created yet.",

            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
        )

        return
    }

    val available = tasks.count {
        it.status == TaskStatus.AVAILABLE.name
    }

    val inProgress = tasks.count {
        it.status == TaskStatus.IN_PROGRESS.name
    }

    val blocked = tasks.count {
        it.status == TaskStatus.LOCKED.name
    }

    val done = tasks.count {
        it.status == TaskStatus.DONE.name
    }

    val overdue = tasks.count {
        it.dueDate != null &&
                LocalDate.parse(it.dueDate) < LocalDate.now() &&
                it.status != TaskStatus.DONE.name
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {

        item {
            TaskSummaryRow(
                available = available,
                inProgress = inProgress,
                blocked = blocked,
                done = done,
                overdue = overdue
            )
        }

        items(tasks, key = { it.id }) { task ->

            TaskCard(
                task = task,
                isAssignedToMe = task.assignedMemberId == currentUid,
                onClick = { onTaskClick(task) },
                onStatusChange = { status ->
                    onStatusChange(task.id, status)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun TaskSummaryRow(
    available: Int,
    inProgress: Int,
    blocked: Int,
    done: Int,
    overdue: Int
) {

    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),

        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(
            count = inProgress,
            label = "Active",
            color = Color(0xFF84CC16) ,
            bg = Color(0x3384CC16),
            modifier = Modifier.weight(1f)
        )

        SummaryChip(
            count = blocked,
            label = "Blocked",
            color = Color(0xFF84CC16) ,
            bg = Color(0x3384CC16),
            modifier = Modifier.weight(1f)
        )

        SummaryChip(
            count = done,
            label = "Done",
            color = Color(0xFF84CC16) ,
            bg = Color(0x3384CC16),
            modifier = Modifier.weight(1f)
        )

        if (overdue > 0) {

            SummaryChip(
                count = overdue,
                label = "Overdue",
                color = colorScheme.error,
                bg = colorScheme.errorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryChip(
    count: Int,
    label: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(
                vertical = 8.dp,
                horizontal = 4.dp
            )
        ) {

            Text(
                text = count.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )

            Text(
                text = label,
                fontSize = 10.sp,
                color = color.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun MembersTab(
    tasksByMember: Map<User, List<Task>>
) {

    if (tasksByMember.isEmpty()) {

        EmptyState(
            emoji = "\uD83D\uDC65",
            title = "No members",
            subtitle = "Members are managed from the Group screen.",
            modifier = Modifier.fillMaxWidth()
        )

        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {

        items(
            tasksByMember.entries.toList(),
            key = { it.key.uid }
        ) { (member, tasks) ->

            MemberWorkloadCard(
                member = member,
                tasks = tasks
            )
        }
    }
}

@Composable
private fun MemberWorkloadCard(
    member: User,
    tasks: List<Task>
) {

    val colorScheme = MaterialTheme.colorScheme

    val done = tasks.count {
        it.status == TaskStatus.DONE.name
    }

    val total = tasks.size

    val progress =
        if (total > 0)
            done.toFloat() / total
        else
            0f

    val hasOverdue = tasks.any {
        it.dueDate != null &&
                LocalDate.parse(it.dueDate) < LocalDate.now() &&
                it.status != TaskStatus.DONE.name
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),

        shape = RoundedCornerShape(14.dp)
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                UserAvatar(
                    name = member.name,
                    size = 40
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        Text(
                            text = member.name,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )

                        if (hasOverdue) {

                            Surface(
                                color = colorScheme.errorContainer,
                                shape = RoundedCornerShape(20.dp)
                            ) {

                                Text(
                                    text = "Overdue",
                                    fontSize = 10.sp,
                                    color = colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium,

                                    modifier = Modifier.padding(
                                        horizontal = 7.dp,
                                        vertical = 2.dp
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = "$done / $total task${if (total != 1) "s" else ""} done",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {

                    Text(
                        text = total.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onPrimaryContainer,

                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        )
                    )
                }
            }

            if (total > 0) {

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { progress },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),

                    color =
                        if (hasOverdue)
                            colorScheme.error
                        else
                            colorScheme.primary,

                    trackColor = colorScheme.surfaceVariant,

                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            if (tasks.isNotEmpty()) {

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(
                    color = colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                val displayTasks =
                    tasks.filter {
                        it.status != TaskStatus.DONE.name
                    }.ifEmpty {
                        tasks.take(2)
                    }

                displayTasks.forEach { task ->
                    MemberTaskRow(task = task)
                }

                if (tasks.size > displayTasks.size) {

                    val remaining = tasks.size - displayTasks.size

                    Text(
                        text = "+$remaining more completed",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant,

                        modifier = Modifier.padding(
                            top = 4.dp,
                            start = 4.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberTaskRow(task: Task) {

    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        StatusChip(status = task.status)

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = task.title,
            fontSize = 12.sp,

            color =
                if (task.status == TaskStatus.DONE.name)
                    colorScheme.onSurfaceVariant
                else
                    colorScheme.onSurface,

            maxLines = 1,
            overflow = TextOverflow.Ellipsis,

            modifier = Modifier.weight(1f)
        )

        task.dueDate?.let {

            val urgency = task.dueDateUrgency()

            if (
                urgency == DueDateUrgency.OVERDUE ||
                urgency == DueDateUrgency.TODAY
            ) {

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = urgency.bgColor(),
                    shape = RoundedCornerShape(20.dp)
                ) {

                    Text(
                        text = task.dueDateLabel(),
                        fontSize = 10.sp,
                        color = urgency.color(),

                        modifier = Modifier.padding(
                            horizontal = 6.dp,
                            vertical = 2.dp
                        )
                    )
                }
            }
        }
    }
}