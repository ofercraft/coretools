package com.feldman.coretools.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.feldman.coretools.storage.themeColorFlow

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val useDarkTheme = isDarkTheme()

    val colorScheme =
        if (useDarkTheme) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}




@Composable
fun isDarkTheme(): Boolean {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    // collect preference as state
    val themePreference by produceState(initialValue = 0, context) {
        context.themeColorFlow().collect { value = it }
    }

    val useDarkTheme = when (themePreference) {
        1 -> false // light
        2 -> true  // dark
        else -> systemDark // auto
    }
    return useDarkTheme

}