package com.example.calllimiter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

// REMOVE THIS INCORRECT IMPORT: import com.example.calllimiter.ui.ContactsScreen

// Ensure this import is correct and points to your ContactsScreen.kt
import com.example.calllimiter.ui.screens.CallLogScreen
import com.example.calllimiter.ui.screens.ContactsScreen // THIS IS THE CORRECT IMPORT
import com.example.calllimiter.ui.screens.DialerPadScreen
import com.example.calllimiter.ui.screens.SettingsScreen

import com.example.calllimiter.ui.viewmodels.CallLogViewModel
import com.example.calllimiter.ui.viewmodels.ContactsViewModel
import com.example.calllimiter.ui.viewmodels.DialerViewModel
import com.example.calllimiter.ui.viewmodels.SettingsViewModel

object AppDestinations {
    const val CALL_LOG_ROUTE  = "call_log"
    const val CONTACTS_ROUTE  = "contacts"
    const val DIALER_ROUTE    = "dialer"
    const val SETTINGS_ROUTE  = "settings"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = AppDestinations.CALL_LOG_ROUTE
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        /* ---- Call Log ---- */
        composable(AppDestinations.CALL_LOG_ROUTE) {
            // CallLogScreen no longer takes navController
            CallLogScreen()
        }

        /* ---- Contacts ---- */
        composable(AppDestinations.CONTACTS_ROUTE) { // Corrected typo here: AppDestents -> AppDestinations
            // ContactsScreen now takes navController
            ContactsScreen(navController = navController)
        }

        /* ---- Dialer ---- */
        composable(AppDestinations.DIALER_ROUTE) {
            val vm: DialerViewModel = hiltViewModel()
            DialerPadScreen(
                navController = navController,
                onCallAction = { phoneNumber ->
                    vm.placeCall(phoneNumber)
                }
            )
        }

        /* ---- Settings ---- */
        composable(AppDestinations.SETTINGS_ROUTE) {
            // SettingsScreen still uses a navController.
            val vm: SettingsViewModel = hiltViewModel()
            SettingsScreen(navController = navController, viewModel = vm)
        }
    }
}
