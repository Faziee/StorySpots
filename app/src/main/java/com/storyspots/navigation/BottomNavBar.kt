import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun BottomNavBar(
    currentRoute: String?,
    onItemClick: (NavItem) -> Unit
) {
    val sideItems = listOf(NavItem.Home, NavItem.Favourites, null, NavItem.Notifications, NavItem.Settings)

    Box(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.White)
                .align(Alignment.BottomCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            sideItems.forEach { item ->
                if (item == null) {
                    Spacer(modifier = Modifier.width(72.dp))
                }
                else {
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = { onItemClick(item) },
                        icon = {
                            Icon(
                                painter = painterResource(item.icon),
                                contentDescription = null,
                                tint = if (currentRoute == item.route) Color(0xFFFC6C85) else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = { Spacer(modifier = Modifier.height(0.dp)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-36).dp)
                .size(72.dp)
                .clickable { onItemClick(NavItem.CreatePost) }
        ) {
            Image(
                painter = painterResource(NavItem.CreatePost.icon),
                contentDescription = "Create Post",
                modifier = Modifier.fillMaxSize()
            )
        }

    }
}