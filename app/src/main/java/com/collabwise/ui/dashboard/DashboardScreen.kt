package com.collabwise.ui.dashboard

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
import com.collabwise.ui.components.EmptyState
import com.collabwise.ui.components.GroupCard
import com.collabwise.ui.components.LoadingOverlay
import com.collabwise.ui.components.Navy
import com.collabwise.ui.components.UserAvatar
import com.collabwise.viewmodel.DashboardViewModel

private val bannerColors = listOf(
    Color(0xFF1a73e8), Color(0xFFe53935), Color(0xFF2e7d32),
    Color(0xFFf9a825), Color(0xFF6a1b9a), Color(0xFF00838f),
    Color(0xFFc62828), Color(0xFF1565c0), Color(0xFF558b2f)
)

fun bannerColorFor(id: String): Color =
    bannerColors[Math.abs(id.hashCode()) % bannerColors.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onGroupClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val firstName = remember(uiState.currentUser?.name) {
        uiState.currentUser?.name?.split(" ")?.firstOrNull().orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text("Collab", fontSize = 22.sp, color = Navy)
                        Text("Wise", fontSize = 22.sp, color = Color.Red)
                    }
                },
                actions = {

                    BadgedBox(
                        badge = {
                            if (uiState.unreadCount > 0) {
                                Badge {
                                    Text(
                                        if (uiState.unreadCount > 9) "9+" else uiState.unreadCount.toString()
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNotificationsClick) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    }

                    IconButton(onClick = onProfileClick) {
                        if (uiState.currentUser != null) {
                            UserAvatar(
                                name = uiState.currentUser!!.name,
                                size = 32
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },

        // ✅ FIXED: no global role assumption anymore
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showCreateGroup() },
                containerColor = Navy,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New group") }
            )
        },

        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when {
                uiState.isLoading -> LoadingOverlay()

                uiState.groups.isEmpty() -> EmptyState(
                    emoji = "🏫",
                    title = "No groups yet",
                    subtitle = "Create your first group to get started.",
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    item {
                        Column(Modifier.padding(bottom = 8.dp)) {
                            Text(
                                text = "Welcome, $firstName",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "${uiState.groups.size} group(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    items(uiState.groups, key = { it.id }) { group ->
                        GroupCard(
                            group = group,
                            onClick = { onGroupClick(group.id) }
                        )
                    }

                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (uiState.showCreateGroup) {
        CreateGroupDialog(
            isLoading = uiState.isCreatingGroup,
            errorMessage = uiState.createGroupError,
            onDismiss = { viewModel.dismissCreateGroup() },
            onCreate = { name, desc ->
                viewModel.createGroup(name, desc)
            }
        )
    }
}

@Composable
fun CreateGroupDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text       = "Create group",
                fontWeight = FontWeight.Medium,
                fontSize   = 18.sp
            )
        },
        text = {
            Column {
                // Inline error
                if (errorMessage != null) {
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFCE8E8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text     = errorMessage,
                            color    = Color(0xFFC5221F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Group name field
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Group name") },
                    placeholder   = { Text("e.g. JPIA Finance Committee") },
                    singleLine    = true,
                    isError       = errorMessage != null && name.isBlank(),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Description field
                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    label         = { Text("Description (optional)") },
                    placeholder   = { Text("e.g. Handles financial reports and budgets") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { if (name.isNotBlank()) onCreate(name, description) },
                enabled  = name.isNotBlank() && !isLoading,
                colors   = ButtonDefaults.buttonColors(containerColor = Navy)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color    = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create", fontWeight = FontWeight.Medium)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick  = onDismiss,
                enabled  = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}