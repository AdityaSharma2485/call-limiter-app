package com.example.calllimiter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext // Import LocalContext

// Import your custom colors from Color.kt
import com.example.calllimiter.ui.theme.Blue80
import com.example.calllimiter.ui.theme.BlueGrey80
import com.example.calllimiter.ui.theme.Blue40
import com.example.calllimiter.ui.theme.BlueGrey40

// Import your Typography from Type.kt
import com.example.calllimiter.ui.theme.Typography


// Updated Dark Color Scheme using our new fallback colors
private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = BlueGrey80
)

// Updated Light Color Scheme using our new fallback colors
private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = BlueGrey40
)

@Composable
fun CallLimiterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Obtain the context within the Composable scope
    val context = LocalContext.current

    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context) // 'context' is now defined
        dynamicColor && !darkTheme -> dynamicLightColorScheme(context) // 'context' is now defined
        darkTheme -> DarkColorScheme // Your predefined dark theme
        else -> LightColorScheme    // Your predefined light theme
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Typography is now imported
        content = content
    )
}
