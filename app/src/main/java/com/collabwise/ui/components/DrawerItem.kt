package com.collabwise.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun DrawerItem(
    title: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        text = title,
        color =
            if (isDestructive)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                onClick()
            }
            .padding(vertical = 12.dp)
    )
}