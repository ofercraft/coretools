package com.feldman.coretools.ui.components.adaptive

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.ui.components.liquid.LiquidButton
import com.feldman.coretools.ui.pages.scaled
import com.feldman.coretools.ui.pages.scaledHeight
import com.kyant.backdrop.Backdrop

@Composable
fun AdaptiveButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = null)
    val isGlass = appStyle == AppStyle.Glass

    if (isGlass) {
        LiquidButton(
            onClick = onClick,
            backdrop = backdrop,
            modifier = modifier,
            isInteractive = isInteractive,
            tint = tint,
            surfaceColor = surfaceColor,
            content = content
        )
    } else {
        Button(
            onClick = onClick,
            modifier = modifier
                .height(scaledHeight(48.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (surfaceColor.isSpecified) surfaceColor else MaterialTheme.colorScheme.primary,
                contentColor = if (tint.isSpecified) tint else MaterialTheme.colorScheme.onPrimary
            ),
            content = content
        )
    }
}
