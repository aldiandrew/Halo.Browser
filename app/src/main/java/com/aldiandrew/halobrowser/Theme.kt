package com.aldiandrew.halobrowser

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun HaloExpressiveTheme(
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current

    val scheme: ColorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark ->
            dynamicDarkColorScheme(context)

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !dark ->
            dynamicLightColorScheme(context)

        dark ->
            darkColorScheme(
                primary = Color(0xFF9FC2FF),
                secondary = Color(0xFFBFC6FF),
                tertiary = Color(0xFF9FE5D2)
            )

        else ->
            expressiveLightColorScheme()
    }

    val shapes = Shapes(
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(32.dp),
        extraLarge = RoundedCornerShape(40.dp)
    )

    MaterialExpressiveTheme(
        colorScheme = scheme,
        shapes = shapes,
        content = content
    )
}
