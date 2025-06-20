import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.DocumentReference
import com.storyspots.caption.ui.FullscreenStoryOverlay
import com.storyspots.caption.model.StoryData
import com.storyspots.model.NotificationWithUser
import com.storyspots.notificationFeed.NotificationSection
import com.storyspots.notificationFeed.NotificationsViewModel
import com.storyspots.ui.theme.Background
import com.storyspots.ui.theme.DarkText
import com.storyspots.ui.theme.LightPink
import com.storyspots.ui.theme.Pink80
import com.storyspots.ui.theme.White
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationFeedScreen(
    onBackClick: () -> Unit = {}
) {
    val viewModel: NotificationsViewModel = viewModel()
    val newNotifications by viewModel.newNotifications.collectAsState()
    val lastWeekNotifications by viewModel.lastWeekNotifications.collectAsState()
    val lastMonthNotifications by viewModel.lastMonthNotifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current

    var selectedStoryRef by remember { mutableStateOf<DocumentReference?>(null) }
    var selectedStory by remember { mutableStateOf<StoryData?>(null) }
    var isStoryLoading by remember { mutableStateOf(false) }
    var selectedNotification by remember { mutableStateOf<NotificationWithUser?>(null) }

    LaunchedEffect(selectedStoryRef) {
        selectedStoryRef?.let { ref ->
            isStoryLoading = true
            val storyData = try {
                val doc = ref.get().await()
                if (doc.exists()) {
                    StoryData(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        createdAt = doc.getTimestamp("created_at"),
                        location = doc.getGeoPoint("location"),
                        caption = doc.getString("caption"),
                        imageUrl = doc.getString("image_url"),
                        mapRef = doc.getDocumentReference("mapRef"),
                        authorRef = doc.getDocumentReference("authorRef"),
                        userPath = doc.getString("userPath")
                    )
                } else null
            } catch (e: Exception) {
                Log.e("FetchStoryData", "Error loading story: ${e.message}", e)
                null
            }

            selectedStory = storyData
            isStoryLoading = false
            selectedStoryRef = null
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            viewModel.refreshNotifications()
                            Toast.makeText(context, "Refreshing...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Pink80,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        containerColor = Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Background),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = LightPink
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(White)
                            .padding(16.dp)
                    ) {
                        Column {
                            val onViewClick: (NotificationWithUser) -> Unit = { item ->
                                selectedStoryRef = item.notification.story
                                selectedNotification = item
                            }

                            NotificationSection(
                                title = "New",
                                items = newNotifications,
                                onViewClick = onViewClick
                            )

                            if (newNotifications.isNotEmpty() &&
                                (lastWeekNotifications.isNotEmpty() || lastMonthNotifications.isNotEmpty())
                            ) {
                                Divider(
                                    color = Background,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            if (lastWeekNotifications.isNotEmpty()) {
                                NotificationSection(
                                    title = "Last 7 days",
                                    items = lastWeekNotifications,
                                    onViewClick = onViewClick
                                )

                                if (lastMonthNotifications.isNotEmpty()) {
                                    Divider(
                                        color = Background,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }

                            if (lastMonthNotifications.isNotEmpty()) {
                                NotificationSection(
                                    title = "Last 30 days",
                                    items = lastMonthNotifications,
                                    onViewClick = onViewClick
                                )
                            }

                            if (newNotifications.isEmpty() &&
                                lastWeekNotifications.isEmpty() &&
                                lastMonthNotifications.isEmpty()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No notifications yet",
                                        fontSize = 16.sp,
                                        color = DarkText.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (isStoryLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LightPink)
                }
            }

            selectedStory?.let { story ->
                FullscreenStoryOverlay(
                    stories = listOf(story),
                    onClose = { selectedStory = null }
                )
            }
        }
    }
}