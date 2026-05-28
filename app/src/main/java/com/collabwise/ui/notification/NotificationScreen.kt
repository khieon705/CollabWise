package com.collabwise.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.collabwise.data.model.Notification
import com.collabwise.ui.components.EmptyState
import com.collabwise.ui.components.LoadingOverlay
import com.collabwise.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
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

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Notifications",
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (uiState.unreadCount > 0) {

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {

                            Text(
                                text =
                                    if (uiState.unreadCount > 99)
                                        "99+"
                                    else
                                        uiState.unreadCount.toString(),

                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Medium,

                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 3.dp
                                )
                            )
                        }
                    }
                }

                if (uiState.unreadCount > 0) {

                    TextButton(
                        onClick = {
                            viewModel.markAllAsRead()
                        }
                    ) {

                        Text(
                            text = "Mark all",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },

        snackbarHost = {
            SnackbarHost(snackbarHost)
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when {

                uiState.isLoading -> {
                    LoadingOverlay()
                }

                uiState.notifications.isEmpty() -> {
                    EmptyState(
                        emoji = "\uD83D\uDD14",
                        title = "No notifications yet",
                        subtitle = "You'll be notified here when tasks are assigned or become available.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {

                        val unread = uiState.notifications.filter { !it.isRead }
                        val read = uiState.notifications.filter { it.isRead }

                        if (unread.isNotEmpty()) {

                            item {
                                SectionHeader(
                                    text = "Unread",
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                )
                            }

                            items(
                                items = unread,
                                key = { notification ->
                                    notification.id.ifBlank {
                                        "${notification.createdAt}_${notification.taskTitle}"
                                    }
                                }
                            ) { notification ->

                                NotificationCard(
                                    notification = notification,
                                    onRead = {
                                        viewModel.markAsRead(notification.id)
                                    }
                                )
                            }
                        }

                        if (read.isNotEmpty()) {

                            item {
                                SectionHeader(
                                    text = "Earlier",
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = if (unread.isNotEmpty()) 16.dp else 8.dp,
                                        bottom = 8.dp
                                    )
                                )
                            }

                            items(
                                items = read,
                                key = { notification ->
                                    notification.id.ifBlank {
                                        "${notification.createdAt}_${notification.taskTitle}"
                                    }
                                }
                            ) { notification ->

                                NotificationCard(
                                    notification = notification,
                                    onRead = {}
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onRead: () -> Unit
) {

    val isUnread = !notification.isRead

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isUnread) onRead()
            }
            .background(
                if (isUnread)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else
                    MaterialTheme.colorScheme.surface
            )
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),

        verticalAlignment = Alignment.Top
    ) {

        Surface(
            color =
                if (isUnread)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,

            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {

            Box(contentAlignment = Alignment.Center) {

                Icon(
                    imageVector = notifIcon(notification),
                    contentDescription = null,

                    tint =
                        if (isUnread)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,

                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            if (notification.projectName.isNotBlank()) {

                Text(
                    text = notification.projectName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Text(
                text = notification.taskTitle,
                fontWeight =
                    if (isUnread)
                        FontWeight.SemiBold
                    else
                        FontWeight.Medium,

                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = notification.message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = relativeTime(notification.createdAt),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (isUnread) {

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 68.dp)
    )
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

private fun notifIcon(notification: Notification) = when {

    notification.message.contains("overdue", true) ||
            notification.message.contains("past", true) -> {
        Icons.Default.Warning
    }

    notification.message.contains("available", true) ||
            notification.message.contains("start", true) -> {
        Icons.Default.PlayCircle
    }

    notification.message.contains("assigned", true) -> {
        Icons.Default.PersonAdd
    }

    else -> {
        Icons.Default.Notifications
    }
}

private fun relativeTime(epochSeconds: Long): String {

    val now = System.currentTimeMillis()

    val diffMs = now - (epochSeconds * 1000)

    val diffMin = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val diffHrs = TimeUnit.MILLISECONDS.toHours(diffMs)
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)

    return when {

        diffMin < 1 -> {
            "Just now"
        }

        diffMin < 60 -> {
            "$diffMin min ago"
        }

        diffHrs < 24 -> {
            "$diffHrs hour${if (diffHrs > 1) "s" else ""} ago"
        }

        diffDays == 1L -> {
            "Yesterday"
        }

        diffDays < 7 -> {
            "$diffDays days ago"
        }

        else -> {
            val sdf = SimpleDateFormat(
                "MMM d",
                Locale.getDefault()
            )

            sdf.format(Date(epochSeconds * 1000))
        }
    }
}