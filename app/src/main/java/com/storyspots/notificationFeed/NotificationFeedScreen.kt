
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyspots.model.NotificationItem
import com.storyspots.ui.theme.Background
import com.storyspots.ui.theme.DarkText
import com.storyspots.ui.theme.LightPink
import com.storyspots.ui.theme.White
import com.storyspots.notificationFeed.NotificationSection
import com.storyspots.notificationFeed.NotificationsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storyspots.model.NotificationWithUser

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun NotificationFeedScreen(
    onBackClick: () -> Unit = {},
    onViewClick: (NotificationWithUser) -> Unit = {}
) {
    val viewModel: NotificationsViewModel = viewModel()
    val newNotifications by viewModel.newNotifications.collectAsState()
    val lastWeekNotifications by viewModel.lastWeekNotifications.collectAsState()
    val lastMonthNotifications by viewModel.lastMonthNotifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = White)
            )
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
                            // New Notifications
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
        }
    }
}