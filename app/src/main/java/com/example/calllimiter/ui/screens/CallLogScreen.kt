package com.example.calllimiter.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.calllimiter.ui.viewmodels.CallHistoryViewModel
import com.example.calllimiter.data.CallLog as AppCallLog
import com.example.calllimiter.ui.viewmodels.CallLogViewModel
import java.text.SimpleDateFormat
import java.util.*

// Permission request code
private const val READ_CALL_LOG_PERMISSION_REQUEST = 101

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(title: String) {
    TopAppBar(
        title = { Text(title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
fun ContactAvatar(name: String) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CallLogItem(
    log: CallLogViewModel.CallLogDisplay,
    onCallClick: (String) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(name = log.callerName ?: log.phoneNumber)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.callerName ?: log.phoneNumber,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getCallTypeIcon(log.type),
                    contentDescription = "Call type",
                    modifier = Modifier.size(16.dp),
                    tint = if (log.type == CallLog.Calls.MISSED_TYPE || log.type == AppCallLog.TYPE_BLOCKED)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                        .format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = {
                val cleanNumber = cleanPhoneNumber(log.originalNumber)
                if (cleanNumber.isNotBlank()) {
                    onCallClick(cleanNumber) // Use callback instead of direct call
                }
            }
        ) {
            Icon(
                Icons.Default.Phone,
                contentDescription = "Call",
                tint = MaterialTheme.colorScheme.primary
            )

            Icon(
                Icons.Default.Phone,
                contentDescription = "Call",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}


private fun cleanPhoneNumber(number: String): String {
    // Remove app-specific prefixes first
    val withoutPrefix = when {
        number.startsWith("blocked_") -> number.removePrefix("blocked_")
        number.startsWith("redirected_") -> {
            // Extract original number from "redirected_original_to_helper" format
            number.removePrefix("redirected_").substringBefore("_to_")
        }
        else -> number
    }

    // Now properly format the number for calling
    val digitsOnly = withoutPrefix.replace(Regex("[^0-9]"), "")

    // Return the number in a format suitable for calling
    return when {
        digitsOnly.length == 10 -> digitsOnly // Already 10 digits
        digitsOnly.length > 10 && digitsOnly.startsWith("91") -> digitsOnly.substring(2) // Remove country code
        digitsOnly.length > 10 -> digitsOnly.takeLast(10) // Take last 10 digits
        else -> digitsOnly
    }
}

@Composable
private fun getCallTypeIcon(type: Int): ImageVector = when (type) {
    CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
    CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
    CallLog.Calls.MISSED_TYPE -> Icons.Default.CallMissed
    AppCallLog.TYPE_BLOCKED -> Icons.Default.Block
    AppCallLog.TYPE_REDIRECTED -> Icons.Default.Redo
    else -> Icons.Default.Phone
}

@Composable
fun CallLogScreen(
    viewModel: CallLogViewModel = hiltViewModel(),
    callHistoryViewModel: CallHistoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val callLogs by viewModel.filteredCallLogs.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadCallLogs()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Call History")

        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search by name or number") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No call history found.\nMake sure permissions are granted.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(callLogs) { log ->
                    CallLogItem(
                        log = log,
                        onCallClick = { number ->
                            callHistoryViewModel.placeCallFromHistory(number)
                        }
                    )
                }
            }
        }
    }
}


