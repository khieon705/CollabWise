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
import androidx.compose.ui.graphics.Color
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
    val uiState        by viewModel.uiState.collectAsState()
    var selectedTab    by remember { mutableIntStateOf(0) }
    val tabs           = listOf("Projects", "Members")
    val snackbarHost   = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text       = uiState.group?.name ?: "",
                            fontWeight = FontWeight.Medium,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        if (uiState.group?.description?.isNotBlank() == true) {
                            Text(
                                text     = uiState.group!!.description,
                                fontSize = 12.sp,
                                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isLeader) {
                        IconButton(onClick = { viewModel.showInvite() }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Invite member")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            if (uiState.isLeader && selectedTab == 0) {
                FloatingActionButton(
                    onClick        = { viewModel.showCreateProject() },
                    containerColor = Color.Red,
                    contentColor   = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New project")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Group color banner strip
            uiState.group?.let { group ->
                Surface(
                    color    = bannerColorFor(group.id),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                ) { }
            }

            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Color.White
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment    = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(title)
                                // Badge counts
                                val count = when (index) {
                                    0 -> uiState.projects.size
                                    1 -> uiState.members.size
                                    else -> 0
                                }
                                if (count > 0) {
                                    Surface(
                                        color = if (selectedTab == index)
                                            Color.Red.copy(alpha = 0.1f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text     = count.toString(),
                                            fontSize = 11.sp,
                                            color    = if (selectedTab == index) Color.Red
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Tab content
            when {
                uiState.isLoading -> LoadingOverlay()
                selectedTab == 0  -> ProjectsTab(
                    projects        = uiState.projects,
                    groupId         = uiState.group?.id ?: "",
                    isLeader        = uiState.isLeader,
                    onProjectClick  = onProjectClick
                )
                else -> MembersTab(
                    members     = uiState.members,
                    leaderId    = uiState.group?.leaderId ?: "",
                    currentUid  = uiState.currentUser?.uid ?: "",
                    isLeader    = uiState.isLeader,
                    onRemove    = { viewModel.removeMember(it) }
                )
            }
        }
    }

    // Invite dialog
    if (uiState.showInvite) {
        InviteDialog(
            isLoading    = uiState.isInviting,
            errorMessage = uiState.inviteError,
            onDismiss    = { viewModel.dismissInvite() },
            onInvite     = { viewModel.inviteMember(it) }
        )
    }

    // Create project dialog
    if (uiState.showCreateProject) {
        CreateProjectDialog(
            isLoading    = uiState.isCreatingProject,
            errorMessage = uiState.createProjectError,
            onDismiss    = { viewModel.dismissCreateProject() },
            onCreate     = { name, desc -> viewModel.createProject(name, desc) }
        )
    }
}

// ── Projects tab ──────────────────────────────────────────────────────────────

@Composable
private fun ProjectsTab(
    projects: List<Project>,
    groupId: String,
    isLeader: Boolean,
    onProjectClick: (String, String) -> Unit
) {
    if (projects.isEmpty()) {
        EmptyState(
            emoji    = "\uD83D\uDCCB",
            title    = "No projects yet",
            subtitle = if (isLeader)
                "Tap + to create the first project."
            else
                "No projects have been created yet.",
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        items(projects, key = { it.id }) { project ->
            ProjectCard(
                project  = project,
                onClick  = { onProjectClick(groupId, project.id) }
            )
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
fun ProjectCard(project: Project, onClick: () -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape     = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder icon
            val isActive = project.status == ProjectStatus.ACTIVE.name
            Surface(
                color    = if (isActive) Color(0xFFE8F0FE) else Color(0xFFF1F3F4),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = Icons.Default.Folder,
                        contentDescription = null,
                        tint               = if (isActive) Color(0xFF1967D2) else Color(0xFF5F6368),
                        modifier           = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = project.name,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (project.description.isNotBlank()) {
                    Text(
                        text     = project.description,
                        fontSize = 12.sp,
                        color    = Color(0xFF5F6368),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status badge
            Surface(
                color = if (isActive) Color(0xFFE6F4EA) else Color(0xFFF1F3F4),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text       = project.status,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color      = if (isActive) Color(0xFF137333) else Color(0xFF5F6368),
                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = Color(0xFFDADCE0),
                modifier           = Modifier
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
            emoji    = "\uD83D\uDC65",
            title    = "No members",
            subtitle = "Invite members using the person icon above.",
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        items(members, key = { it.uid }) { member ->
            MemberCard(
                member     = member,
                isLeader   = member.uid == leaderId,
                isCurrentUser = member.uid == currentUid,
                canRemove  = isLeader && member.uid != leaderId,
                onRemove   = { removingUid = member.uid }
            )
        }
    }

    // Confirm remove dialog
    removingUid?.let { uid ->
        val member = members.find { it.uid == uid }
        AlertDialog(
            onDismissRequest = { removingUid = null },
            title = { Text("Remove member") },
            text  = {
                Text(
                    "Remove ${member?.name ?: "this member"} from the group? " +
                            "They will lose access to all projects and tasks."
                )
            },
            confirmButton = {
                Button(
                    onClick = { onRemove(uid); removingUid = null },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { removingUid = null }) { Text("Cancel") }
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
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        shape    = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(name = member.name, size = 40)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text       = member.name + if (isCurrentUser) " (you)" else "",
                            fontWeight = FontWeight.Medium,
                            fontSize   = 14.sp
                        )
                        if (isLeader) {
                            Surface(
                                color = Color(0xFFE8F0FE),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text     = "Leader",
                                    fontSize = 10.sp,
                                    color    = Color.Red,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text     = member.email,
                        fontSize = 12.sp,
                        color    = Color(0xFF5F6368)
                    )
                }
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector        = Icons.Default.PersonRemove,
                            contentDescription = "Remove member",
                            tint               = Color(0xFF5F6368)
                        )
                    }
                }
            }

            // Skills row
            if (member.skillIds.isNotEmpty()) {
                // We show skill IDs here — in a real app you'd resolve
                // IDs to names via SkillRepository. For now show count.
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(start = 52.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Star,
                        contentDescription = null,
                        tint               = Color(0xFF5F6368),
                        modifier           = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text     = "${member.skillIds.size} skill${if (member.skillIds.size != 1) "s" else ""} declared",
                        fontSize = 12.sp,
                        color    = Color(0xFF5F6368)
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
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Invite member", fontWeight = FontWeight.Medium) },
        text = {
            Column {
                if (errorMessage != null) {
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFCE8E8)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text     = errorMessage,
                            color    = Color(0xFFC5221F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value         = email,
                    onValueChange = { email = it },
                    label         = { Text("Email address") },
                    placeholder   = { Text("e.g. maria@pup.edu.ph") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Text(
                    text     = "The user must already have a CollabWise account.",
                    fontSize = 11.sp,
                    color    = Color(0xFF5F6368),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { if (email.isNotBlank()) onInvite(email.trim()) },
                enabled  = email.isNotBlank() && !isLoading,
                colors   = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Invite", fontWeight = FontWeight.Medium)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
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
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Create project", fontWeight = FontWeight.Medium) },
        text = {
            Column {
                if (errorMessage != null) {
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFCE8E8)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text     = errorMessage,
                            color    = Color(0xFFC5221F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Project name") },
                    placeholder   = { Text("e.g. Accounting Week 2026") },
                    singleLine    = true,
                    isError       = errorMessage != null && name.isBlank(),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value         = desc,
                    onValueChange = { desc = it },
                    label         = { Text("Description (optional)") },
                    placeholder   = { Text("e.g. Annual accounting org event") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { if (name.isNotBlank()) onCreate(name, desc) },
                enabled  = name.isNotBlank() && !isLoading,
                colors   = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create", fontWeight = FontWeight.Medium)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}