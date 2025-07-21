package com.example.calllimiter.ui.screens // Changed from .ui to .ui.screens as per your structure

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.calllimiter.data.ContactRule
import com.example.calllimiter.ui.viewmodels.ContactsViewModel
import com.example.calllimiter.ui.navigation.Screen // Import the Screen object

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    navController: NavHostController, // Added navController to navigate
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val contactRules by viewModel.filteredContacts.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState() // Keep searchQuery here

    // State for the universal toggle. It's on if the list is not empty and all contacts are managed.
    val allContactsManaged by remember(contactRules) {
        derivedStateOf { contactRules.isNotEmpty() && contactRules.all { it.isManaged } }
    }

    // REMOVED: var showDialog by remember { mutableStateOf(false) }
    // REMOVED: if (showDialog) { ... AlertDialog ... } block

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                actions = {
                    // Only keep the settings icon
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Intent to open system contact app to create a new contact
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                }
                context.startActivity(intent)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Create new contact")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search contacts") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (contactRules.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("No contacts found or permissions not granted.") }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(contactRules, key = { it.phoneNumber }) { rule ->
                        ContactRow(
                            rule = rule,
                            onRuleChanged = viewModel::updateContactRule,
                            onContactClick = { phoneNumber ->
                                // Open system contact for the given phone number
                                val contactUri = Uri.withAppendedPath(
                                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                                    Uri.encode(phoneNumber)
                                )
                                val cursor = context.contentResolver.query(
                                    contactUri,
                                    arrayOf(ContactsContract.PhoneLookup._ID),
                                    null,
                                    null,
                                    null
                                )
                                cursor?.use {
                                    if (it.moveToFirst()) {
                                        val contactId = it.getLong(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                                        val viewContactUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                                            .appendPath(contactId.toString())
                                            .build()
                                        val intent = Intent(Intent.ACTION_VIEW, viewContactUri)
                                        context.startActivity(intent)
                                    } else {
                                        // If contact not found by number, try to open a generic contact view
                                        val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
                                        context.startActivity(intent)
                                    }
                                } ?: run {
                                    // Fallback if cursor is null
                                    val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// No changes needed below this line for the dialer role request
@RequiresApi(Build.VERSION_CODES.Q)
private fun requestDefaultDialerRole(context: Context) {
    val roleManager = context.getSystemService(RoleManager::class.java)
    if (roleManager != null &&
        roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
        !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    ) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        context.startActivity(intent)
    } else {
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        context.startActivity(intent)
    }
}


@Composable
private fun ContactRow(
    rule: ContactRule,
    onRuleChanged: (ContactRule) -> Unit,
    onContactClick: (String) -> Unit // Added callback for contact click
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { onContactClick(rule.phoneNumber) }, // Make the card clickable
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                Text(rule.phoneNumber, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(
                checked = rule.isManaged,
                onCheckedChange = { onRuleChanged(rule.copy(isManaged = it)) }
            )
        }
    }
}