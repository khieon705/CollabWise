package com.collabwise.ui.dashboard

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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.collabwise.R
import com.collabwise.ui.components.AppDrawer
import com.collabwise.ui.components.EmptyState
import com.collabwise.ui.components.GroupCard
import com.collabwise.ui.components.LoadingOverlay
import com.collabwise.ui.components.UserAvatar
import com.collabwise.ui.navigation.Screen
import com.collabwise.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val bannerColors = listOf(
    Color(0xFF1A73E8),
    Color(0xFFE53935),
    Color(0xFF2E7D32),
    Color(0xFFF9A825),
    Color(0xFF6A1B9A),
    Color(0xFF00838F),
    Color(0xFFC62828),
    Color(0xFF1565C0),
    Color(0xFF558B2F)
)

fun bannerColorFor(id: String): Color =
    bannerColors[Math.abs(id.hashCode()) % bannerColors.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onGroupClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigate: (Screen) -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val firstName = remember(uiState.currentUser?.name) {
        uiState.currentUser
            ?.name
            ?.split(" ")
            ?.firstOrNull()
            .orEmpty()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                onGroupsClick = {
                    scope.launch {
                        drawerState.close()
                        delay(50)
                        onNavigate(Screen.Dashboard)
                    }
                },
                onNotificationsClick = {
                    scope.launch {
                        drawerState.close()
                        delay(50)
                        onNavigate(Screen.Notifications)
                    }
                },
                onTodoClick = {
                    scope.launch {
                        drawerState.close()
                        delay(50)
                        onNavigate(Screen.Dashboard)
                    }
                },
                onLogoutClick = {
                    scope.launch {
                        drawerState.close()
                        delay(50)
                        onLogout()
                    }
                }
            )
        }
    ) {

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = "CollabWise",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily(
                            Font(R.font.protest_riot_regular)
                        ),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onNotificationsClick
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.unreadCount > 0) {
                                    Badge {
                                        Text(
                                            text =
                                                if (uiState.unreadCount > 9)
                                                    "9+"
                                                else
                                                    uiState.unreadCount.toString(),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    }

                    IconButton(
                        onClick = onProfileClick
                    ) {
                        if (uiState.currentUser != null) {
                            UserAvatar(
                                name = uiState.currentUser!!.name,
                                size = 28
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.showCreateGroup()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        Icon(Icons.Default.Add, null)
                    },
                    text = {
                        Text(
                            text = "New group",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            }
        ) { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingOverlay()
                    }
                    uiState.groups.isEmpty() -> {
                        EmptyState(
                            emoji = "🏫",
                            title = "No groups yet",
                            subtitle =
                                "Create your first group to get started.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = "Welcome, $firstName",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${uiState.groups.size} group(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            items(
                                uiState.groups,
                                key = { it.id }
                            ) { group ->
                                GroupCard(
                                    group = group,
                                    onClick = {
                                        onGroupClick(group.id)
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
                }
            }
        }
    }

    if (uiState.showCreateGroup) {
        CreateGroupDialog(
            isLoading = uiState.isCreatingGroup,
            errorMessage = uiState.createGroupError,
            onDismiss = {
                viewModel.dismissCreateGroup()
            },
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
        onDismissRequest = {
            if (!isLoading) {
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Create group",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
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
                        Text("Group name")
                    },
                    placeholder = {
                        Text("e.g. JPIA Finance Committee")
                    },
                    singleLine = true,
                    isError = errorMessage != null && name.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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
                            MaterialTheme.colorScheme.primary,
                        focusedContainerColor =
                            MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor =
                            MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                    },
                    label = {
                        Text("Description (optional)")
                    },
                    placeholder = {
                        Text(
                            "e.g. Handles financial reports and budgets"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                            MaterialTheme.colorScheme.primary,
                        focusedContainerColor =
                            MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor =
                            MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description)
                    }
                },
                enabled = name.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                    Text(
                        text = "Create",
                        fontWeight = FontWeight.SemiBold
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
