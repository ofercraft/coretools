package com.feldman.coretools

import android.app.Application
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.DynamicColors

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.feldman.coretools.MainActivity.Dest
import com.feldman.coretools.R
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.OrientationMode
import com.feldman.coretools.storage.PrefKeys
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.dataStore
import com.feldman.coretools.storage.orientationModeFlow
import com.feldman.coretools.ui.components.liquid.LiquidBottomTab
import com.feldman.coretools.ui.components.liquid.LiquidBottomTabs
import com.feldman.coretools.ui.navigation.AppNavHost
import com.feldman.coretools.ui.theme.AppTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
    val indicatorWidth by animateDpAsState(targetValue = if (selected) 64.dp else 0.dp)
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
