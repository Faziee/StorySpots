import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.storyspots.R

sealed class NavItem(
    val title: String,
    @DrawableRes val icon: Int,
    val route: String
) {
    object Home : NavItem("Home", R.drawable.ic_home, "home")
    object Favourites : NavItem("Favourites", R.drawable.ic_favourites, "favourites")
    object Notifications : NavItem("Notifications", R.drawable.ic_notifications, "notifications")
    object Settings : NavItem("Settings", R.drawable.ic_settings, "settings")
    object CreatePost : NavItem("", R.drawable.ic_post, "create_post")
}