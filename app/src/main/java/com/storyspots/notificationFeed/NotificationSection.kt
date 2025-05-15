package com.storyspots.notificationFeed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyspots.model.NotificationItem
import com.storyspots.ui.components.NotificationRow
import com.storyspots.ui.theme.DarkText

@Composable
fun NotificationSection(
    title: String,
    items: List<NotificationItem>,
    onViewClick: (NotificationItem) -> Unit = {}
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Text(
                text = "No notifications",
                fontSize = 14.sp,
                color = DarkText.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp)
            )
        } else {
            items.forEach { notificationItem ->
                NotificationRow(
                    item = notificationItem,
                    onViewClick = onViewClick
                )
            }
        }
    }
}