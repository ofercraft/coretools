package com.feldman.coretools.ui.navigation

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import com.feldman.coretools.MainActivity.Dest
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.feldman.coretools.storage.defaultPageFlow
import com.feldman.coretools.ui.pages.*
import com.feldman.coretools.ui.pages.settings.AppSettingsPage
import com.feldman.coretools.ui.pages.settings.CompassSettingsPage
import com.feldman.coretools.ui.pages.settings.CustomizationSettingsPage
import com.feldman.coretools.ui.pages.settings.FlashlightSettingsPage
import com.feldman.coretools.ui.pages.settings.LevelSettingsPage
import com.feldman.coretools.ui.pages.settings.TileSettingsPage
import com.feldman.coretools.ui.pages.settings.UsageSettingsPage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

fun isSettingsRouteTransition(from: String?, to: String?): Boolean {
    if (from == null || to == null) return false

    val fromDest = Dest.valueOf(from)
    val toDest = Dest.valueOf(to)
    val fromIsSettings = fromDest.parent != null
    val toIsSettings = toDest.parent != null
    return fromIsSettings || toIsSettings
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop = rememberLayerBackdrop(),
    startDestination: Dest? = null
) {
    val items = Dest.entries
    var prevRoute by remember { mutableStateOf<String?>(null) }
    var currentRoute by remember { mutableStateOf<String?>(null) }
    var prevIndex by remember { mutableIntStateOf(0) }
    var forward by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val defaultPage by context.defaultPageFlow().collectAsState(initial = Dest.Flashlight)

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val newRoute = destination.route ?: return@OnDestinationChangedListener

            val oldRoute = currentRoute
            if (oldRoute != null && newRoute != oldRoute) {
                val oldIndex = items.indexOfFirst {
                    oldRoute.startsWith(it.name, ignoreCase = true)
                }.takeIf { it != -1 } ?: prevIndex
                val newIndex = items.indexOfFirst {
                    newRoute.startsWith(it.name, ignoreCase = true)
                }.takeIf { it != -1 } ?: oldIndex
                forward = newIndex > oldIndex

                println("new: $newRoute $newIndex, old: $oldRoute $oldIndex forward $forward")
                prevIndex = newIndex
            }

            prevRoute = oldRoute
            currentRoute = newRoute
        }

        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    val settingsTransition = remember(currentRoute, prevRoute) {
        isSettingsRouteTransition(prevRoute, currentRoute)
    }
    val duration = 400
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val easing = LinearOutSlowInEasing

    NavHost(
        navController = navController,
        startDestination = startDestination?.name ?: defaultPage.name,
        enterTransition = {
            when {
                settingsTransition -> fadeIn(animationSpec = tween(duration, easing = easing))
                forward -> {
                    // Landscape → slide up, Portrait → slide right
                    val slideIn = if (isLandscape)
                        slideInVertically(initialOffsetY = { it / 6 }, animationSpec = tween(duration, easing = easing))
                    else
                        slideInHorizontally(initialOffsetX = { it / 6 }, animationSpec = tween(duration, easing = easing))

                    scaleIn(initialScale = 0.92f, animationSpec = tween(duration, easing = easing)) +
                            fadeIn(animationSpec = tween(duration, easing = easing)) + slideIn
                }
                else -> {
                    // Landscape → slide down, Portrait → slide left
                    val slideIn = if (isLandscape)
                        slideInVertically(initialOffsetY = { -it / 6 }, animationSpec = tween(duration, easing = easing))
                    else
                        slideInHorizontally(initialOffsetX = { -it / 6 }, animationSpec = tween(duration, easing = easing))

                    scaleIn(initialScale = 0.92f, animationSpec = tween(duration, easing = easing)) +
                            fadeIn(animationSpec = tween(duration, easing = easing)) + slideIn
                }
            }
        },
        exitTransition = {
            when {
                settingsTransition -> fadeOut(animationSpec = tween(duration, easing = easing))
                forward -> {
                    // Landscape → slide down, Portrait → slide left
                    val slideOut = if (isLandscape)
                        slideOutVertically(targetOffsetY = { -it / 8 }, animationSpec = tween(duration, easing = easing))
                    else
                        slideOutHorizontally(targetOffsetX = { -it / 8 }, animationSpec = tween(duration, easing = easing))

                    scaleOut(targetScale = 0.95f, animationSpec = tween(duration, easing = easing)) +
                            fadeOut(animationSpec = tween(duration, easing = easing)) + slideOut
                }
                else -> {
                    // Landscape → slide up, Portrait → slide right
                    val slideOut = if (isLandscape)
                        slideOutVertically(targetOffsetY = { it / 8 }, animationSpec = tween(duration, easing = easing))
                    else
                        slideOutHorizontally(targetOffsetX = { it / 8 }, animationSpec = tween(duration, easing = easing))

                    scaleOut(targetScale = 0.95f, animationSpec = tween(duration, easing = easing)) +
                            fadeOut(animationSpec = tween(duration, easing = easing)) + slideOut
                }
            }
        },
        popEnterTransition = {
            scaleIn(initialScale = 0.92f, animationSpec = tween(duration, easing = easing)) +
                    fadeIn(animationSpec = tween(duration, easing = easing))
        },
        popExitTransition = {
            scaleOut(targetScale = 0.95f, animationSpec = tween(duration, easing = easing)) +
                    fadeOut(animationSpec = tween(duration, easing = easing))
        },
        modifier = modifier
    ) {
        composable(Dest.Flashlight.name) { FlashlightPage() }
        composable(Dest.Usage.name) { UsageScreen() }
        composable(Dest.Compass.name) { CompassPage() }
        composable(Dest.Level.name) { LevelPage(modifier = Modifier.layerBackdrop(backdrop)) }
        composable(Dest.Settings.name) { SettingsPage(navController = navController) }

        //Settings page
        composable(Dest.SettingsApp.name) { AppSettingsPage(navController) }
        composable(Dest.SettingsCustomization.name) { CustomizationSettingsPage(navController) }
        composable(Dest.SettingsFlash.name) { FlashlightSettingsPage(navController) }
        composable(Dest.SettingsCompass.name) { CompassSettingsPage(navController) }
        composable(Dest.SettingsUsage.name) { UsageSettingsPage(navController) }
        composable(Dest.SettingsLevel.name) { LevelSettingsPage(navController) }
        composable(Dest.SettingsTile.name) { TileSettingsPage(navController) }
    }

}
