package com.storyspots.notificationFeed

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.storyspots.ui.theme.DarkText
import com.storyspots.ui.theme.LightText
import com.storyspots.ui.theme.Pink
import com.storyspots.ui.theme.White
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import com.storyspots.model.NotificationWithUser

@Composable
fun NotificationRow(
    item: NotificationWithUser,
    onViewClick: (NotificationWithUser) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            if (!item.profileImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${item.username}'s profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = item.username.firstOrNull()?.toString() ?: "?"
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = item.username,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.notification.message ?: "No message",
                fontSize = 14.sp,
                color = LightText
            )
        }

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