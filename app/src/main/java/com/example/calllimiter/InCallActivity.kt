package com.example.calllimiter

import android.os.Build
import android.os.Bundle
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calllimiter.service.CallManager
import com.example.calllimiter.ui.theme.CallLimiterTheme
// Make sure this import is present and correct
import androidx.compose.material.icons.filled.AddIcCall

@RequiresApi(Build.VERSION_CODES.M)
class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallLimiterTheme {
                val call by CallManager.call.collectAsState()
                val callState by CallManager.callState.collectAsState()

                if (callState == Call.STATE_DISCONNECTED) {
                    finish()
                }

                InCallScreen(
                    callState = callState,
                    callerNumber = call?.details?.handle?.schemeSpecificPart ?: "Unknown",
                    onHangup = { CallManager.hangup() },
                    onMute = { /* TODO: Implement Mute */ },
                    onSpeaker = { /* TODO: Implement Speaker */ },
                    onHold = { /* TODO: Implement Hold */ }
                )
            }
        }
    }
}

@Composable
fun InCallScreen(
    callState: Int?,
    callerNumber: String,
    onHangup: () -> Unit,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onHold: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF2C2C2C)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            Text(
                text = callerNumber,
                fontSize = 36.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = getCallStateString(callState),
                fontSize = 18.sp,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InCallButton(icon = Icons.Default.MicOff, text = "Mute", onClick = onMute)
                InCallButton(icon = Icons.Default.Dialpad, text = "Keypad", onClick = { /*TODO*/ })
                InCallButton(icon = Icons.Default.VolumeUp, text = "Speaker", onClick = onSpeaker)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // **** THIS IS THE CORRECTED LINE ****
                InCallButton(icon = Icons.Default.AddIcCall, text = "Add call", onClick = { /*TODO*/ })
                InCallButton(icon = Icons.Default.Videocam, text = "Video", onClick = { /*TODO*/ })
                InCallButton(icon = Icons.Default.Pause, text = "Hold", onClick = onHold)
            }

            Spacer(modifier = Modifier.height(48.dp))

            FloatingActionButton(
                onClick = onHangup,
                shape = CircleShape,
                modifier = Modifier.size(72.dp),
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "Hang Up", modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InCallButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            modifier = Modifier.size(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Gray.copy(alpha = 0.3f),
                contentColor = Color.White
            )
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = text, color = Color.White, fontSize = 14.sp)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private fun getCallStateString(state: Int?): String {
    return when (state) {
        Call.STATE_DIALING -> "Dialing..."
        Call.STATE_RINGING -> "Ringing..."
        Call.STATE_ACTIVE -> "Active"
        Call.STATE_CONNECTING -> "Connecting..."
        Call.STATE_DISCONNECTED -> "Disconnected"
        Call.STATE_HOLDING -> "On Hold"
        else -> " "
    }
}
