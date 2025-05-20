package com.storyspots.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.storyspots.model.NotificationItem
import com.storyspots.ui.theme.DarkText
import com.storyspots.ui.theme.LightText
import com.storyspots.ui.theme.Pink
import com.storyspots.ui.theme.White

@Composable
fun NotificationRow(
    item: NotificationItem,
    onViewClick: (NotificationItem) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image
        Image(
            painter = rememberAsyncImagePainter(item.imageUrl),
            contentDescription = "${item.from}'s profile picture",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        // Notification Text
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = item.from,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.from,
                fontSize = 14.sp,
                color = LightText
            )
        }

        // View Button
        Button(
            onClick = { onViewClick(item) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Pink
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = "View",
                color = White,
                fontSize = 14.sp
            )
        }
    }
}