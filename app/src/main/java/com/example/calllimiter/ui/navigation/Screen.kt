package com.example.calllimiter.ui.navigation

/**
 * Sealed class to define all navigation routes in the app.
 * This provides a type-safe way to define and access routes.
 */
sealed class Screen(val route: String) {
    object CallLog : Screen("call_log")
    object Contacts : Screen("contacts")
    object Dialer : Screen("dialer")
    object Settings : Screen("settings") // Route for the SettingsScreen
    // Add other screens here as needed
}
