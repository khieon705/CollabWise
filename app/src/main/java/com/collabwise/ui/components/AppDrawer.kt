package com.collabwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.collabwise.R

@Composable
fun AppDrawer(
    onGroupsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onTodoClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color.White) // 👈 makes it solid
            .padding(16.dp)
    ) {

        Text(
            "CollabWise",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily(Font(R.font.protest_riot_regular))
        )

        Spacer(Modifier.height(24.dp))

        DrawerItem("Groups", onClick = onGroupsClick)
        DrawerItem("Notifications", onClick = onNotificationsClick)
        DrawerItem("Todo", onClick = onTodoClick)

        Spacer(Modifier.weight(1f))

        DrawerItem("Logout", isDestructive = true, onClick = onLogoutClick)
    }
}