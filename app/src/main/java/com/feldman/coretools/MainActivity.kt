package com.feldman.coretools

import android.app.Application
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.feldman.coretools.MainActivity.Dest
import com.feldman.coretools.R
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.OrientationMode
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.orientationModeFlow
import com.feldman.coretools.ui.components.liquid.LiquidBottomTab
import com.feldman.coretools.ui.components.liquid.LiquidBottomTabs
import com.feldman.coretools.ui.navigation.AppNavHost
import com.feldman.coretools.ui.theme.AppTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch


val LocalAppOrientation = compositionLocalOf { OrientationMode.AUTO }


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    enum class Dest(
        val label: String,
        val filledIcon: Int,
        val outlineIcon: Int,
        val parent: Dest? = null
    ) {
        Flashlight("Flash", R.drawable.ic_flashlight, R.drawable.ic_flashlight_outline),
        Compass("Compass", R.drawable.ic_compass, R.drawable.ic_compass_outline),
        Usage("Usage", R.drawable.ic_usage, R.drawable.ic_usage_outline),
        Level("Level", R.drawable.ic_level, R.drawable.ic_level_outline),
        Settings("Settings", R.drawable.ic_settings, R.drawable.ic_settings_outline),

        SettingsApp("App Settings", R.drawable.ic_apps, R.drawable.ic_apps, parent = Settings),
        SettingsCustomization("Customization Settings", R.drawable.ic_palette, R.drawable.ic_palette_outline, parent = Settings),
        SettingsFlash("Flash Settings", R.drawable.ic_flashlight, R.drawable.ic_flashlight_outline, parent = Settings),
        SettingsCompass("Compass Settings", R.drawable.ic_compass, R.drawable.ic_compass_outline, parent = Settings),
        SettingsUsage("Usage Settings", R.drawable.ic_usage, R.drawable.ic_usage_outline, parent = Settings),
        SettingsLevel("Level Settings", R.drawable.ic_level, R.drawable.ic_level_outline, parent = Settings),
        SettingsTile("Tile Settings", R.drawable.ic_tile, R.drawable.ic_tile, parent = Settings)
    }




    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainApp()
        }
    }

}


@Composable
fun MainApp(startDestination: Dest? = null){
    AppRoot {
        AppTheme {
            val navController = rememberNavController()

            val backdrop = rememberLayerBackdrop()
            val context = LocalContext.current

            val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Playful)
            val isGlass = appStyle == AppStyle.Glass
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    if (!isGlass && !isLandscape) {
                        NavigationBar {
                            Dest.entries
                                .filter { it.parent == null }
                                .forEach { dest ->
                                    val isSelected =
                                        currentRoute?.startsWith(dest.name, ignoreCase = true) == true

                                    // per-item anim state
                                    val offsetY = remember { Animatable(0f) }
                                    val rotation = remember { Animatable(0f) }
                                    val usageScales = if (dest == Dest.Usage) {
                                        remember { listOf(Animatable(1f), Animatable(1f), Animatable(1f)) }
                                    } else null

                                    // animate on selection; reset on unselect
                                    LaunchedEffect(dest, isSelected) {
                                        if (isSelected) {
                                            when (dest) {
                                                Dest.Flashlight -> {
                                                    offsetY.snapTo(0f)
                                                    offsetY.animateTo(-8f, tween(200, easing = LinearEasing))
                                                    offsetY.animateTo(0f, tween(200, easing = LinearEasing))
                                                }
                                                Dest.Compass -> {
                                                    rotation.snapTo(0f)
                                                    rotation.animateTo(20f, tween(75, easing = LinearEasing))
                                                    rotation.animateTo(-20f, tween(150, easing = LinearEasing))
                                                    rotation.animateTo(0f, tween(75, easing = LinearEasing))
                                                }
                                                Dest.Usage -> {
                                                    usageScales!!.forEach { it.snapTo(1f) }
                                                    for (i in usageScales.indices) {
                                                        usageScales[i].animateTo(1.4f, tween(150, easing = LinearEasing))
                                                    }
                                                    usageScales.forEach { s ->
                                                        launch {
                                                            s.animateTo(0.8f, tween(200, easing = LinearEasing))
                                                            s.animateTo(1f, tween(150, easing = LinearEasing))
                                                        }
                                                    }
                                                }
                                                Dest.Level -> {
                                                    rotation.snapTo(0f)
                                                    rotation.animateTo(16f, tween(120, easing = LinearEasing))
                                                    rotation.animateTo(-16f, tween(240, easing = LinearEasing))
                                                    rotation.animateTo(0f, tween(120, easing = LinearEasing))
                                                }
                                                Dest.Settings -> {
                                                    rotation.snapTo(0f)
                                                    rotation.animateTo(360f, tween(300, easing = LinearEasing))
                                                    rotation.snapTo(0f)
                                                }
                                                else -> { }
                                            }
                                        } else {
                                            offsetY.snapTo(0f)
                                            rotation.snapTo(0f)
                                            usageScales?.forEach { it.snapTo(1f) }
                                        }
                                    }

                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != dest.name) {
                                                navController.navigate(dest.name) {
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            if (dest == Dest.Usage && isSelected) {
                                                val tintColor = MaterialTheme.colorScheme.primary
                                                val scales = usageScales ?: return@NavigationBarItem

                                                Box(Modifier.size(28.dp)) {
                                                    Image(
                                                        painter = painterResource(R.drawable.ic_usage_part1),
                                                        contentDescription = null,
                                                        colorFilter = ColorFilter.tint(tintColor),
                                                        modifier = Modifier
                                                            .matchParentSize()
                                                            .graphicsLayer {
                                                                scaleX = scales[0].value
                                                                scaleY = scales[0].value
                                                            }
                                                    )
                                                    Image(
                                                        painter = painterResource(R.drawable.ic_usage_part2),
                                                        contentDescription = null,
                                                        colorFilter = ColorFilter.tint(tintColor),
                                                        modifier = Modifier
                                                            .matchParentSize()
                                                            .graphicsLayer {
                                                                scaleX = scales[1].value
                                                                scaleY = scales[1].value
                                                            }
                                                    )
                                                    Image(
                                                        painter = painterResource(R.drawable.ic_usage_part3),
                                                        contentDescription = null,
                                                        colorFilter = ColorFilter.tint(tintColor),
                                                        modifier = Modifier
                                                            .matchParentSize()
                                                            .graphicsLayer {
                                                                scaleX = scales[2].value
                                                                scaleY = scales[2].value
                                                            }
                                                    )
                                                }
                                            }
                                            else {
                                                val tint = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                                Icon(
                                                    painter = painterResource(
                                                        if (isSelected) dest.filledIcon else dest.outlineIcon
                                                    ),
                                                    contentDescription = dest.label,
                                                    tint = tint,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .offset(y = offsetY.value.dp)
                                                        .graphicsLayer { rotationZ = rotation.value }
                                                )
                                            }
                                        },
                                        label = { Text(dest.label) }
                                    )
                                }
                        }

                    }
                },
                contentWindowInsets = WindowInsets(0)
            ) { padding ->
                val layoutDir = LocalLayoutDirection.current
                val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

                // Exact rail width
                val railWidth = 92.dp

                // Exact left system inset (handles LTR/RTL + cutouts)
                val leftInset = safeInsets.calculateLeftPadding(layoutDir)

                // Use the same system insets for the rail itself
                val railInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // ---- Content ----
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier
                            .padding(padding)
                            .padding(4.dp)
                            .layerBackdrop(backdrop)
                            .windowInsetsPadding(
                                WindowInsets.systemBars.only(WindowInsetsSides.Top)
                            )
                            .then(
                                if (isLandscape)
                                    Modifier.padding(start = railWidth + leftInset)
                                else
                                    Modifier
                            ),
                        backdrop = backdrop,
                        startDestination = startDestination
                    )

                    // ---- Rail ----
                    if (!isGlass && isLandscape) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .align(Alignment.CenterStart) // anchor to top
                                // apply bars + cutout so it's never under system UI
                                .windowInsetsPadding(railInsets)
                                .fillMaxHeight()
                                .width(railWidth)
                                .padding(vertical = 8.dp) // small internal breathing room
                        ) {
                            Dest.entries
                                .filter { it.parent == null }
                                .forEach { dest ->
                                    val isSelected = currentRoute?.startsWith(dest.name, true) == true
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != dest.name) {
                                                navController.navigate(dest.name) {
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                painter = painterResource(
                                                    if (isSelected) dest.filledIcon else dest.outlineIcon
                                                ),
                                                contentDescription = dest.label,
                                                tint = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = dest.label,
                                                fontSize = 13.sp,
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        alwaysShowLabel = true
                                    )
                                }
                        }
                    }



                    if (isGlass) {
                        val selectedTabIndex = remember(currentRoute) {
                            Dest.entries.indexOfFirst { dest ->
                                currentRoute?.startsWith(dest.name, ignoreCase = true) == true
                            }.takeIf { it >= 0 } ?: 0
                        }

                        LiquidBottomTabs(
                            selectedTabIndex = { selectedTabIndex },
                            onTabSelected = { index ->
                                val selected = Dest.entries.filter { it.parent == null }[index]
                                navController.navigate(selected.name) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            backdrop = backdrop,
                            tabsCount = Dest.entries.filter { it.parent == null }.size,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 36.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            Dest.entries.filter { it.parent == null }.forEachIndexed { index, dest ->
                                val isSelected = selectedTabIndex == index
                                val tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface

                                // per-tab anim state
                                val offsetY = remember { Animatable(0f) }
                                val rotation = remember { Animatable(0f) }
                                val usageScales = if (dest == Dest.Usage) {
                                    remember { listOf(Animatable(1f), Animatable(1f), Animatable(1f)) }
                                } else null

                                LaunchedEffect(dest, isSelected) {
                                    if (isSelected) {
                                        when (dest) {
                                            Dest.Flashlight -> {
                                                offsetY.snapTo(0f)
                                                offsetY.animateTo(-8f, tween(200, easing = LinearEasing))
                                                offsetY.animateTo(0f, tween(200, easing = LinearEasing))
                                            }
                                            Dest.Compass -> {
                                                rotation.snapTo(0f)
                                                rotation.animateTo(20f, tween(75, easing = LinearEasing))
                                                rotation.animateTo(-20f, tween(150, easing = LinearEasing))
                                                rotation.animateTo(0f, tween(75, easing = LinearEasing))
                                            }
                                            Dest.Usage -> {
                                                usageScales!!.forEach { it.snapTo(1f) }
                                                for (i in usageScales.indices) {
                                                    usageScales[i].animateTo(1.4f, tween(150, easing = LinearEasing))
                                                }
                                                usageScales.forEach { s ->
                                                    launch {
                                                        s.animateTo(0.8f, tween(200, easing = LinearEasing))
                                                        s.animateTo(1f, tween(150, easing = LinearEasing))
                                                    }
                                                }
                                            }
                                            Dest.Level -> {
                                                rotation.snapTo(0f)
                                                rotation.animateTo(16f, tween(120, easing = LinearEasing))
                                                rotation.animateTo(-16f, tween(240, easing = LinearEasing))
                                                rotation.animateTo(0f, tween(120, easing = LinearEasing))
                                            }
                                            Dest.Settings -> {
                                                rotation.snapTo(0f)
                                                rotation.animateTo(360f, tween(300, easing = LinearEasing))
                                                rotation.snapTo(0f)
                                            }
                                            else -> { }
                                        }
                                    } else {
                                        offsetY.snapTo(0f)
                                        rotation.snapTo(0f)
                                        usageScales?.forEach { it.snapTo(1f) }
                                    }
                                }

                                LiquidBottomTab(
                                    onClick = {
                                        val selected = Dest.entries.filter { it.parent == null }[index]
                                        navController.navigate(selected.name) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // icon
                                    if (dest == Dest.Usage && isSelected) {
                                        val scales = usageScales ?: return@LiquidBottomTab

                                        Box(Modifier.size(28.dp)) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_usage_part1),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(tint),
                                                modifier = Modifier.matchParentSize().graphicsLayer {
                                                    scaleX = scales[0].value
                                                    scaleY = scales[0].value
                                                }
                                            )
                                            Image(
                                                painter = painterResource(R.drawable.ic_usage_part2),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(tint),
                                                modifier = Modifier.matchParentSize().graphicsLayer {
                                                    scaleX = scales[1].value
                                                    scaleY = scales[1].value
                                                }
                                            )
                                            Image(
                                                painter = painterResource(R.drawable.ic_usage_part3),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(tint),
                                                modifier = Modifier.matchParentSize().graphicsLayer {
                                                    scaleX = scales[2].value
                                                    scaleY = scales[2].value
                                                }
                                            )
                                        }
                                    } else {
                                        Box(
                                            Modifier
                                                .size(28.dp)
                                                .graphicsLayer {
                                                    rotationZ = rotation.value
                                                    translationY = offsetY.value.dp.toPx()
                                                }
                                                .paint(
                                                    painterResource(
                                                        if (isSelected) dest.filledIcon else dest.outlineIcon
                                                    ),
                                                    colorFilter = ColorFilter.tint(tint)
                                                )
                                        )
                                    }

                                    // label
                                    BasicText(
                                        text = dest.label,
                                        style = TextStyle(
                                            color = tint,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }

                    }
                }

            }
        }
    }

}

@Composable
fun AppRoot(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val userOrientation by context.orientationModeFlow().collectAsState(initial = OrientationMode.AUTO)
    val deviceLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

    val effectiveOrientation = when (userOrientation) {
        OrientationMode.AUTO -> if (deviceLandscape) OrientationMode.LANDSCAPE else OrientationMode.PORTRAIT
        else -> userOrientation
    }

    CompositionLocalProvider(LocalAppOrientation provides effectiveOrientation) {
        content()
    }
}


class CoretoolsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        FlashlightRepo.init(this)
    }
}
//
//@Composable
//fun isLandscape(): Boolean {
//    val context = LocalContext.current
//    val userOrientation by context.orientationModeFlow().collectAsState(initial = OrientationMode.AUTO)
//    val deviceLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
//
//    val isLandscape = when (userOrientation) {
//        OrientationMode.AUTO -> deviceLandscape
//        OrientationMode.LANDSCAPE -> true
//        OrientationMode.PORTRAIT -> false
//    }
//    return isLandscape
//}
