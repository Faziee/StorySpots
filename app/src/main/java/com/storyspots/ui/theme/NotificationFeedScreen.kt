package com.storyspots.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyspots.model.NotificationItem
import com.storyspots.ui.NotificationRow

@Composable
fun NotificationFeedScreen() {
    val newItems = emptyList<NotificationItem>()         // Will be dynamic later
    val pastWeekItems = emptyList<NotificationItem>()    // Will be dynamic later
    val pastMonthItems = emptyList<NotificationItem>()   // Will be dynamic later

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Section("New", newItems)
        Spacer(modifier = Modifier.height(16.dp))
        Section("Last 7 days", pastWeekItems)
        Spacer(modifier = Modifier.height(16.dp))
        Section("Last 30 days", pastMonthItems)
    }
}

@Composable
private fun Section(title: String, items: List<NotificationItem>) {
    Text(text = title, style = MaterialTheme.typography.subtitle1)
    Spacer(modifier = Modifier.height(8.dp))
    items.forEach { item ->
        NotificationRow(item)
    }
}
