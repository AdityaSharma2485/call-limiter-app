package com.example.calllimiter.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.calllimiter.data.ContactRule
import com.example.calllimiter.ui.viewmodels.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val contactRules by viewModel.filteredContacts.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    // State for the universal toggle. It's on if the list is not empty and all contacts are managed.
    val allContactsManaged by remember(contactRules) {
        derivedStateOf { contactRules.isNotEmpty() && contactRules.all { it.isManaged } }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Controls") },
            text = {
                // This Column now contains the universal toggle and the buttons
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manage All Contacts")
                        Switch(
                            checked = allContactsManaged,
                            onCheckedChange = { isChecked ->
                                viewModel.setAllContactsManaged(isChecked)
                            }
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                requestDefaultDialerRole(context)
                            } else {
                                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                context.startActivity(intent)
                            }
                            showDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set as Default Dialer")
                    }

                    Button(
                        onClick = { viewModel.startService(); showDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start Service") }

                    Button(
                        onClick = { viewModel.stopService(); showDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Stop Service") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (contactRules.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("No contacts found or permissions not granted.") }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contactRules, key = { it.phoneNumber }) { rule ->
                        ContactRow(rule, viewModel::updateContactRule)
                    }
                }
            }
        }
    }
}

// No changes needed below this line
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
    onRuleChanged: (ContactRule) -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
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
                if (rule.isManaged) {
                    Text(
                        "Limit: ${rule.callLimit} calls / ${rule.timeWindowHours} hr(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Switch(
                checked = rule.isManaged,
                onCheckedChange = { onRuleChanged(rule.copy(isManaged = it)) }
            )
        }
    }
}