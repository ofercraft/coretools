package com.feldman.coretools

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun RowScope.CompactNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    enabled: Boolean = true,
    colors: NavigationBarItemColors = NavigationBarItemDefaults.colors()
) {
    val interactionSource = remember { MutableInteractionSource() }

    // ✅ Public-safe colors
    val targetIconColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val targetTextColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

    val iconColor by animateColorAsState(targetIconColor)
    val textColor by animateColorAsState(targetTextColor)

    // 🎬 Animated background pill
    val indicatorWidth by animateDpAsState(targetValue = if (selected) 56.dp else 0.dp)
    val indicatorAlpha by animateFloatAsState(if (selected) 1f else 0f)

    Box(
        modifier = Modifier
            .weight(1f)
            .clickable(
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // 🟦 Background indicator — slightly up so it sits behind the icon, not the text
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(indicatorWidth)
                .offset(y = (-10).dp) // ⬅️ shift upward to sit just behind the icon
                .clip(MaterialTheme.shapes.large)
                .graphicsLayer { alpha = indicatorAlpha }
                .background(indicatorColor)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 🧭 Icon
            CompositionLocalProvider(LocalContentColor provides iconColor) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }

            if (label != null && (alwaysShowLabel || selected)) {
                Spacer(Modifier.height(4.dp))
                CompositionLocalProvider(LocalContentColor provides textColor) {
                    label()
                }
            }
        }
    }
}
