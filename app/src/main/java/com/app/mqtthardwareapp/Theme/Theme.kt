package com.app.mqtthardwareapp.Theme


import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Yellow,
    surface = DarkBlue,
    onPrimary = Color.White,
    background = Black,
    onBackground = Color.White,
    surfaceContainer = Grey
)

private val LightColorScheme = lightColorScheme(
    primary = Yellow,
    surface = DarkBlue,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Black,
    surfaceContainer = Grey


)

@Composable
fun MqttHardwareAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme


    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}