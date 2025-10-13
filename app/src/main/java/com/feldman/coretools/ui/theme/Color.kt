package com.feldman.coretools.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.toColorInt


@Composable
fun buttonColor2(): Color {
    return if (isSystemInDarkTheme()) {
        Color("#8f8f8f".toColorInt())
    } else {
        Color("#8f8f8f".toColorInt())
    }
}
@Composable
fun textColor(): Color {
    return if (isSystemInDarkTheme()) {
        Color("#ffffff".toColorInt())
    } else {
        Color("#000000".toColorInt())
    }
}

@Composable
fun primary(): Color {
    return fadePrimary(0f)
}


@Composable
fun primary1(): Color {
    return fadePrimary(0.1f)
}

@Composable
fun primary2(): Color {
    return fadePrimary(0.2f)
}

@Composable
fun primary3(): Color {
    return fadePrimary(0.3f)
}

@Composable
fun primary4(): Color {
    return fadePrimary(0.4f)
}

@Composable
fun primary5(): Color {
    return fadePrimary(0.5f)
}

@Composable
fun fadePrimary(fade: Float): Color {
    val fadedPrimary = lerp(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background, fade)
    return fadedPrimary
}
@Composable
fun getColor(light: Color, dark: Color): Color {
    val darkTheme = isSystemInDarkTheme()
    return if (darkTheme) dark else light
}
@Composable
fun getColor(light: String, dark: String): Color {
    val darkTheme = isSystemInDarkTheme()
    return if (darkTheme) Color(dark.toColorInt()) else Color(light.toColorInt())
}
@Composable
fun tickColor(): Color {
    return getColor("#767676", "#b4b4b4")
}
