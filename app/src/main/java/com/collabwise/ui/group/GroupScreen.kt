package com.collabwise.ui.group

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.collabwise.data.model.Project
import com.collabwise.data.model.ProjectStatus
import com.collabwise.data.model.User
import com.collabwise.ui.components.EmptyState
import com.collabwise.ui.components.LoadingOverlay
import com.collabwise.ui.components.UserAvatar
import com.collabwise.ui.dashboard.bannerColorFor
import com.collabwise.viewmodel.GroupViewModel

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    onBack: () -> Unit,
    onProjectClick: (groupId: String, projectId: String) -> Unit,
    viewModel: GroupViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    val tabs = listOf("Projects", "Members")

    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                        text = uiState.group?.name ?: "",
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (uiState.group?.description?.isNotBlank() == true) {
                        Text(
                            text = uiState.group!!.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (uiState.isLeader) {
                    IconButton(
                        onClick = {
                            viewModel.showInvite()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Invite member",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        },

        floatingActionButton = {
            if (uiState.isLeader && selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        viewModel.showCreateProject()
                    },
                    containerColor =
                        MaterialTheme.colorScheme.primary,
                    contentColor =
                        MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New project"
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
            uiState.group?.let { group ->
                Surface(
                    color = bannerColorFor(group.id),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                ) {}
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor =
                    MaterialTheme.colorScheme.background,
                contentColor =
                    MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                        },
                        selectedContentColor =
                            MaterialTheme.colorScheme.primary,
                        unselectedContentColor =
                            MaterialTheme.colorScheme.secondary,
                        text = {
                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.spacedBy(6.dp)
                            ) {
                                Text(title)
                                val count = when (index) {
                                    0 -> uiState.projects.size
                                    1 -> uiState.members.size
                                    else -> 0
                                }

                                if (count > 0) {
                                    Surface(
                                        color =
                                            if (selectedTab == index)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else
                                                MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            fontSize = 11.sp,
                                            color =
                                                if (selectedTab == index)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.secondary,
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
                uiState.isLoading -> {
                    LoadingOverlay()
                }

                selectedTab == 0 -> {
                    ProjectsTab(
                        projects = uiState.projects,
                        groupId = uiState.group?.id ?: "",
                        isLeader = uiState.isLeader,
                        onProjectClick = onProjectClick,
                        onStatusClick = { projectId, status ->
                            viewModel.updateProjectStatus(projectId, status)
                        }
                    )
                }

                else -> {
                    MembersTab(
                        members = uiState.members,
                        leaderId = uiState.group?.leaderId ?: "",
                        currentUid = uiState.currentUser?.uid ?: "",
                        isLeader = uiState.isLeader,
                        onRemove = {
                            viewModel.removeMember(it)
                        }
                    )
                }
            }
        }
    }

    if (uiState.showInvite) {
        InviteDialog(
            isLoading = uiState.isInviting,
            errorMessage = uiState.inviteError,
            onDismiss = {
                viewModel.dismissInvite()
            },
            onInvite = {
                viewModel.inviteMember(it)
            }
        )
    }

    if (uiState.showCreateProject) {
        CreateProjectDialog(
            isLoading = uiState.isCreatingProject,
            errorMessage = uiState.createProjectError,
            onDismiss = {
                viewModel.dismissCreateProject()
            },
            onCreate = { name, desc ->
                viewModel.createProject(name, desc)
            }
        )
    }
}

// ── Projects tab ──────────────────────────────────────────────────────────────

@Composable
private fun ProjectsTab(
    projects: List<Project>,
    groupId: String,
    isLeader: Boolean,
    onProjectClick: (String, String) -> Unit,
    onStatusClick: (String, ProjectStatus) -> Unit
) {
    if (projects.isEmpty()) {
        EmptyState(
            emoji = "\uD83D\uDCCB",
            title = "No projects yet",
            subtitle =
                if (isLeader)
                    "Tap + to create the first project."
                else
                    "No projects have been created yet.",
            modifier = Modifier.fillMaxSize().padding(top = 64.dp)
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(projects, key = { it.id }) { project ->
            ProjectCard(
                project = project,
                isLeader = isLeader,
                onClick = {
                    onProjectClick(groupId, project.id)
                },
                onStatusClick = {
                    val newStatus =
                        if (project.status == ProjectStatus.ACTIVE.name)
                            ProjectStatus.COMPLETED
                        else
                            ProjectStatus.ACTIVE

                    onStatusClick(project.id, newStatus)
                }
            )
        }

        item {
            Spacer(
                modifier = Modifier.height(72.dp)
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    isLeader: Boolean,
    onClick: () -> Unit,
    onStatusClick: () -> Unit
) {
    val isActive = project.status == ProjectStatus.ACTIVE.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color =
                    if (isActive)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint =
                            if (isActive)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary,

                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (project.description.isNotBlank()) {
                    Text(
                        text = project.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color =
                    if (isActive)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable(enabled = isLeader) {
                    onStatusClick()
                }
            ) {
                Text(
                    text = project.status,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color =
                        if (isActive)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 3.dp
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(16.dp)
            )
        }
    }
}

// ── Members tab ───────────────────────────────────────────────────────────────

@Composable
private fun MembersTab(
    members: List<User>,
    leaderId: String,
    currentUid: String,
    isLeader: Boolean,
    onRemove: (String) -> Unit
) {

    var removingUid by remember { mutableStateOf<String?>(null) }

    if (members.isEmpty()) {
        EmptyState(
            emoji = "\uD83D\uDC65",
            title = "No members",
            subtitle = "Invite members using the person icon above.",
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(members, key = { it.uid }) { member ->
            MemberCard(
                member = member,
                isLeader = member.uid == leaderId,
                isCurrentUser = member.uid == currentUid,
                canRemove = isLeader && member.uid != leaderId,
                onRemove = {
                    removingUid = member.uid
                }
            )
        }
    }

    removingUid?.let { uid ->
        val member = members.find { it.uid == uid }

        AlertDialog(
            onDismissRequest = {
                removingUid = null
            },
            containerColor =
                MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Remove member",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text =
                        "Remove ${member?.name ?: "this member"} from the group? " +
                                "They will lose access to all projects and tasks.",
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemove(uid)
                        removingUid = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme.colorScheme.error,
                        contentColor =
                            MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        removingUid = null
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

@Composable
private fun MemberCard(
    member: User,
    isLeader: Boolean,
    isCurrentUser: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
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
                            text = member.name + if (isCurrentUser) " (you)" else "",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (isLeader) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "Leader",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 2.dp
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = member.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (canRemove) {
                    IconButton(
                        onClick = onRemove
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = "Remove member",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (member.skillIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(12.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "${member.skillIds.size} skill${if (member.skillIds.size != 1) "s" else ""} declared",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// ── InviteDialog ──────────────────────────────────────────────────────────────

@Composable
fun InviteDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit
) {

    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Invite member",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                    },
                    label = {
                        Text("Email address")
                    },
                    placeholder = {
                        Text("e.g. maria@pup.edu.ph")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor =
                            MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor =
                            MaterialTheme.colorScheme.secondary,
                        focusedTextColor =
                            MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor =
                            MaterialTheme.colorScheme.onSurface,
                        cursorColor =
                            MaterialTheme.colorScheme.primary
                    )
                )

                Text(
                    text = "The user must already have a CollabWise account.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        onInvite(email.trim())
                    }
                },
                enabled = email.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme.colorScheme.primary,
                    contentColor =
                        MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Invite", fontWeight = FontWeight.Medium)
                }
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

// ── CreateProjectDialog ───────────────────────────────────────────────────────

@Composable
fun CreateProjectDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {

    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        containerColor =
            MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Create project",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text(
                            "Project name"
                        )
                    },
                    placeholder = {
                        Text(
                            "e.g. Accounting Week 2026"
                        )
                    },
                    singleLine = true,
                    isError =
                        errorMessage != null && name.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor =
                            MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor =
                            MaterialTheme.colorScheme.secondary,
                        focusedTextColor =
                            MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor =
                            MaterialTheme.colorScheme.onSurface,
                        cursorColor =
                            MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = {
                        desc = it
                    },
                    label = {
                        Text(
                            "Description (optional)"
                        )
                    },
                    placeholder = {
                        Text(
                            "e.g. Annual accounting org event"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor =
                            MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor =
                            MaterialTheme.colorScheme.secondary,
                        focusedTextColor =
                            MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor =
                            MaterialTheme.colorScheme.onSurface,
                        cursorColor =
                            MaterialTheme.colorScheme.primary
                    )
                )
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, desc)
                    }
                },
                enabled =
                    name.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme.colorScheme.primary,
                    contentColor =
                        MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color =
                            MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}