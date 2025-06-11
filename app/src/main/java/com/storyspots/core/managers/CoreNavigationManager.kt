package com.storyspots.core.managers

import NavItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NavigationScreen {
    const val HOME = "home"
    const val YOUR_FEED = "your_feed"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val CREATE = "create"
}

class CoreNavigationManager {
    private val _currentScreen = MutableStateFlow(NavigationScreen.HOME)
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    fun navigate(item: NavItem) {
        _currentScreen.value = when (item) {
            NavItem.Home -> NavigationScreen.HOME
            NavItem.YourFeed -> NavigationScreen.YOUR_FEED
            NavItem.Notifications -> NavigationScreen.NOTIFICATIONS
            NavItem.Settings -> NavigationScreen.SETTINGS
            NavItem.CreatePost -> NavigationScreen.CREATE
        }
    }

    fun navigateToHome() {
        _currentScreen.value = NavigationScreen.HOME
    }

    fun navigateToScreen(screen: String) {
        _currentScreen.value = screen
    }
}