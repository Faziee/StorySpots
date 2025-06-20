package com.storyspots.navigation

import androidx.annotation.DrawableRes
import com.storyspots.R

sealed class NavItem(
    val title: String,
    @DrawableRes val icon: Int,
    val route: String
) {
    object Home : NavItem("Home", R.drawable.ic_home, "home")
    object YourFeed : NavItem("Your Feed", R.drawable.ic_favourites, "your_feed")
    object Notifications : NavItem("Notifications", R.drawable.ic_notifications, "notifications")
    object Settings : NavItem("Settings", R.drawable.ic_settings, "settings")
    object CreatePost : NavItem("Post", R.drawable.ic_post, "create_post")
}