package com.example.calllimiter.ui.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.calllimiter.ui.navigation.AppDestinations

// Added selected and unselected icons for a better visual cue
data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navItems = listOf(
        BottomNavItem(
            "Calls",
            Icons.Filled.Call,
            Icons.Outlined.Call,
            AppDestinations.CALL_LOG_ROUTE
        ),
        BottomNavItem(
            "Contacts",
            Icons.Filled.Contacts,
            Icons.Outlined.Contacts,
            AppDestinations.CONTACTS_ROUTE
        ),
        // Added Dialer as a main navigation destination for easy access
        BottomNavItem(
            "Dialpad",
            Icons.Filled.Dialpad,
            Icons.Outlined.Dialpad,
            AppDestinations.DIALER_ROUTE
        )
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        navItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    val icon = if (currentDestination?.hierarchy?.any { it.route == item.route } == true) {
                        item.selectedIcon
                    } else {
                        item.unselectedIcon
                    }
                    Icon(icon, contentDescription = item.label)
                },
                label = { Text(item.label, fontSize = 12.sp) }, // Slightly smaller font for labels
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
