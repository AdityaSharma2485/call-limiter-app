package com.example.calllimiter.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerPadScreen(
    navController: NavHostController,
    onCallAction: (String) -> Unit
) {
    var dialedNumber by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            // A prominent FloatingActionButton for the call action, centered at the bottom.
            FloatingActionButton(
                onClick = {
                    if (dialedNumber.isNotBlank()) {
                        onCallAction(dialedNumber)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Call",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from the Scaffold
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Vertically center the display and keypad within the available space.
            Spacer(modifier = Modifier.weight(1f))

            // Number Display Row
            NumberDisplayWithBackspace(
                number = dialedNumber,
                onBackspace = {
                    if (dialedNumber.isNotEmpty()) {
                        dialedNumber = dialedNumber.dropLast(1)
                    }
                },
                onClear = { dialedNumber = "" }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Keypad Grid
            GoogleStyleKeypadGrid(
                modifier = Modifier.padding(horizontal = 24.dp), // Add padding to make buttons circular
                onDigitClick = { digit -> dialedNumber += digit }
            )

            // Vertically center the display and keypad within the available space.
            Spacer(modifier = Modifier.weight(1f))

            // Spacer to ensure the FAB does not overlap content excessively.
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberDisplayWithBackspace(
    number: String,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = number.ifEmpty { "Enter number" },
            fontSize = 36.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            color = if (number.isNotEmpty())
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
            maxLines = 2
        )

        // Backspace button is only visible when there's a number.
        if (number.isNotEmpty()) {
            IconButton(
                modifier = Modifier
                    .size(48.dp)
                    .combinedClickable(
                        onClick = onBackspace,
                        onLongClick = onClear
                    ),
                onClick = onBackspace // Required for accessibility
            ) {
                Icon(
                    Icons.Default.Backspace,
                    contentDescription = "Delete (Long press to clear)",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun GoogleStyleKeypadGrid(
    modifier: Modifier = Modifier,
    onDigitClick: (String) -> Unit
) {
    val buttons = listOf(
        KeypadData("1", ""), KeypadData("2", "ABC"), KeypadData("3", "DEF"),
        KeypadData("4", "GHI"), KeypadData("5", "JKL"), KeypadData("6", "MNO"),
        KeypadData("7", "PQRS"), KeypadData("8", "TUV"), KeypadData("9", "WXYZ"),
        KeypadData("*", ""), KeypadData("0", "+"), KeypadData("#", "")
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        buttons.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                row.forEach { keypadData ->
                    GoogleStyleKeypadButton(
                        keypadData = keypadData,
                        onClick = { onDigitClick(keypadData.digit) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f) // Makes the button a square
                    )
                }
            }
        }
    }
}

data class KeypadData(
    val digit: String,
    val letters: String
)

@Composable
private fun GoogleStyleKeypadButton(
    modifier: Modifier = Modifier,
    keypadData: KeypadData,
    onClick: () -> Unit
) {
    // A flat, circular, clickable button with a ripple effect.
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.CircleShape) // Ensures ripple is circular
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true) // Material-style ripple effect
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = keypadData.digit,
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (keypadData.letters.isNotEmpty()) {
            Text(
                text = keypadData.letters,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}