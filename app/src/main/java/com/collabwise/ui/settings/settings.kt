package com.collabwise.ui.settings


import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {

    var notificationsEnabled by remember {
        mutableStateOf(true)
    }

    var selectedTheme by remember {
        mutableStateOf("System")
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Settings")
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── NOTIFICATIONS ─────────────────────

            SettingsSectionTitle(
                title = "🔔 Notifications"
            )

            Card(
                shape = RoundedCornerShape(16.dp),

                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),

                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "Enable Notifications"
                    )

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = {
                            notificationsEnabled = it
                        }
                    )
                }
            }

            // ── THEME ─────────────────────────────

            SettingsSectionTitle(
                title = "🎨 Theme"
            )

            Card(
                shape = RoundedCornerShape(16.dp),

                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {

                    ThemeOption(
                        title = "System",
                        selected = selectedTheme == "System",
                        onClick = {
                            selectedTheme = "System"
                        }
                    )

                    HorizontalDivider()

                    ThemeOption(
                        title = "Light",
                        selected = selectedTheme == "Light",
                        onClick = {
                            selectedTheme = "Light"
                        }
                    )

                    HorizontalDivider()

                    ThemeOption(
                        title = "Dark",
                        selected = selectedTheme == "Dark",
                        onClick = {
                            selectedTheme = "Dark"
                        }
                    )
                }
            }

            // ── ACCOUNT ───────────────────────────

            SettingsSectionTitle(
                title = "👤 Account"
            )

            Card(
                shape = RoundedCornerShape(16.dp),

                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {

                    SettingsItem(
                        title = "Change Password",
                        icon = Icons.Default.Lock,
                        onClick = { }
                    )

                    HorizontalDivider()

                    SettingsItem(
                        title = "Logout",
                        icon = Icons.Default.Logout,
                        onClick = onLogout
                    )
                }
            }

            // ── ABOUT APP ─────────────────────────

            SettingsSectionTitle(
                title = "ℹ️ About App"
            )

            Card(
                shape = RoundedCornerShape(16.dp),

                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {

                    SettingsItem(
                        title = "App Version 1.0.0",
                        icon = Icons.Default.Info,
                        onClick = { }
                    )

                    HorizontalDivider()

                    SettingsItem(
                        title = "Check for Updates",
                        icon = Icons.Default.Settings,
                        onClick = { }
                    )

                    HorizontalDivider()

                    SettingsItem(
                        title = "Developers",
                        icon = Icons.Default.Person,
                        onClick = { }
                    )

                    HorizontalDivider()

                    SettingsItem(
                        title = "Terms & Conditions",
                        icon = Icons.Default.Description,
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = title
        )

        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),

        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title
            )

            Text(
                text = title
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}