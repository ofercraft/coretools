package com.feldman.coretools.ui.navigation

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import com.feldman.coretools.Dest
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.feldman.coretools.DestBackStack
import com.feldman.coretools.ordered
import com.feldman.coretools.ui.pages.*
import com.feldman.coretools.ui.pages.settings.AppSettingsPage
import com.feldman.coretools.ui.pages.settings.CompassSettingsPage
import com.feldman.coretools.ui.pages.settings.CustomizationSettingsPage
import com.feldman.coretools.ui.pages.settings.FlashlightSettingsPage
import com.feldman.coretools.ui.pages.settings.LevelSettingsPage
import com.feldman.coretools.ui.pages.settings.SpeedometerSettingsPage
import com.feldman.coretools.ui.pages.settings.TileSettingsPage
import com.feldman.coretools.ui.pages.settings.UsageSettingsPage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.util.Map.entry
import kotlin.reflect.KClass

//
//fun isSettingsRouteTransition(from: String?, to: String?): Boolean {
//    if (from == null || to == null) return false
//
//    val fromDest = Dest.valueOf(from)
//    val toDest = Dest.valueOf(to)
//    val fromIsSettings = fromDest.parent != null
//    val toIsSettings = toDest.parent != null
//    return fromIsSettings || toIsSettings
//}

//@Composable
//fun AppNavHost(
//    navController: NavHostController,
//    modifier: Modifier = Modifier,
//    backdrop: LayerBackdrop = rememberLayerBackdrop(),
//    startDestination: Dest? = null
//) {
//    val items = Dest.entries
//    var prevRoute by remember { mutableStateOf<String?>(null) }
//    var currentRoute by remember { mutableStateOf<String?>(null) }
//    var prevIndex by remember { mutableIntStateOf(0) }
//    var forward by remember { mutableStateOf(true) }
//    val context = LocalContext.current
//    val defaultPage by context.defaultPageFlow().collectAsState(initial = Dest.Flashlight)
//
//    DisposableEffect(navController) {
//        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
//            val newRoute = destination.route ?: return@OnDestinationChangedListener
//
//            val oldRoute = currentRoute
//            if (oldRoute != null && newRoute != oldRoute) {
//                val oldIndex = items.indexOfFirst {
//                    oldRoute.startsWith(it.name, ignoreCase = true)
//                }.takeIf { it != -1 } ?: prevIndex
//                val newIndex = items.indexOfFirst {
//                    newRoute.startsWith(it.name, ignoreCase = true)
//                }.takeIf { it != -1 } ?: oldIndex
//                forward = newIndex > oldIndex
//
//                println("new: $newRoute $newIndex, old: $oldRoute $oldIndex forward $forward")
//                prevIndex = newIndex
//            }
//
//            prevRoute = oldRoute
//            currentRoute = newRoute
//        }
//
//        navController.addOnDestinationChangedListener(listener)
//        onDispose { navController.removeOnDestinationChangedListener(listener) }
//    }
//
//    val settingsTransition = remember(currentRoute, prevRoute) {
//        isSettingsRouteTransition(prevRoute, currentRoute)
//    }
//    val duration = 400
//    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
//    val easing = LinearOutSlowInEasing
//
//    NavHost(
//        navController = navController,
//        startDestination = startDestination?.name ?: defaultPage.name,
//        enterTransition = {
//            when {
//                settingsTransition -> fadeIn(animationSpec = tween(duration, easing = easing))
//                forward -> {
//                    // Landscape → slide up, Portrait → slide right
//                    val slideIn = if (isLandscape)
//                        slideInVertically(initialOffsetY = { it / 6 }, animationSpec = tween(duration, easing = easing))
//                    else
//                        slideInHorizontally(initialOffsetX = { it / 6 }, animationSpec = tween(duration, easing = easing))
//
//                    scaleIn(initialScale = 0.92f, animationSpec = tween(duration, easing = easing)) +
//                            fadeIn(animationSpec = tween(duration, easing = easing)) + slideIn
//                }
//                else -> {
//                    // Landscape → slide down, Portrait → slide left
//                    val slideIn = if (isLandscape)
//                        slideInVertically(initialOffsetY = { -it / 6 }, animationSpec = tween(duration, easing = easing))
//                    else
//                        slideInHorizontally(initialOffsetX = { -it / 6 }, animationSpec = tween(duration, easing = easing))
//
//                    scaleIn(initialScale = 0.92f, animationSpec = tween(duration, easing = easing)) +
//                            fadeIn(animationSpec = tween(duration, easing = easing)) + slideIn
//                }
//            }
//        },
//        exitTransition = {
//            when {
//                settingsTransition -> fadeOut(animationSpec = tween(duration, easing = easing))
//                forward -> {
//                    // Landscape → slide down, Portrait → slide left
//                    val slideOut = if (isLandscape)
//                        slideOutVertically(targetOffsetY = { -it / 8 }, animationSpec = tween(duration, easing = easing))
//                    else
//                        slideOutHorizontally(targetOffsetX = { -it / 8 }, animationSpec = tween(duration, easing = easing))
//
//                    scaleOut(targetScale = 0.95f, animationSpec = tween(duration, easing = easing)) +
//                            fadeOut(animationSpec = tween(duration, easing = easing)) + slideOut
//                }
//                else -> {
//                    // Landscape → slide up, Portrait → slide right
//                    val slideOut = if (isLandscape)
//                        slideOutVertically(targetOffsetY = { it / 8 }, animationSpec = tween(duration, easing = easing))
//                    else
//                        slideOutHorizontally(targetOffsetX = { it / 8 }, animationSpec = tween(duration, easing = easing))
//
//                    scaleOut(targetScale = 0.95f, animationSpec = tween(duration, easing = easing)) +
//                            fadeOut(animationSpec = tween(duration, easing = easing)) + slideOut
//                }
//            }
//        },
//        popEnterTransition = {
//            scaleIn(initialScale = 0.92f, animationSpec = tween(duration, easing = easing)) +
//                    fadeIn(animationSpec = tween(duration, easing = easing))
//        },
//        popExitTransition = {
//            scaleOut(targetScale = 0.95f, animationSpec = tween(duration, easing = easing)) +
//                    fadeOut(animationSpec = tween(duration, easing = easing))
//        },
//        modifier = modifier
//    ) {
//        composable(Dest.Flashlight.name) { FlashlightPage() }
//        composable(Dest.Compass.name) { CompassPage() }
//        composable(Dest.Speedometer.name) { SpeedometerPage() }
//        composable(Dest.Usage.name) { UsageScreen() }
//        composable(Dest.Level.name) { LevelPage(modifier = Modifier.layerBackdrop(backdrop)) }
//        composable(Dest.Settings.name) { SettingsPage(navController = navController) }
//
//        //Settings page
//        composable(Dest.SettingsApp.name) { AppSettingsPage(navController) }
//        composable(Dest.SettingsCustomization.name) { CustomizationSettingsPage(navController) }
//        composable(Dest.SettingsFlash.name) { FlashlightSettingsPage(navController) }
//        composable(Dest.SettingsCompass.name) { CompassSettingsPage(navController) }
//        composable(Dest.SettingsSpeedometer.name) {
//            println(123)
//            SpeedometerSettingsPage(navController)
//        }
//        composable(Dest.SettingsUsage.name) { UsageSettingsPage(navController) }
//        composable(Dest.SettingsLevel.name) { LevelSettingsPage(navController) }
//        composable(Dest.SettingsTile.name) { TileSettingsPage(navController) }
//    }
//
//}

/**
 * Custom App Navigation Host using Jetpack Navigation 3 (NavDisplay) for
 * declarative, state-based navigation and slide animations.
 *
 * NOTE: For this to compile, the following assumptions must be met:
 * 1. Your `Dest` type is a @Serializable sealed interface/class implementing NavKey.
 * 2. All Page composables (e.g., FlashlightPage, SettingsPage) are updated to accept an
 * `onNavigate: (Dest) -> Unit` lambda instead of a NavController.
 */
fun resolveDest(key: Any?): Dest {
    val name = key.toString().substringAfterLast(".") // "Flashlight", "Compass", etc.
    return ordered.firstOrNull { it::class.simpleName == name }
        ?: error("Unknown screen $name")
}

@Composable
fun AppNavHost(
    backStack: DestBackStack,
    onNavigate: (Dest) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop = rememberLayerBackdrop()
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val duration = 400
    val easing = LinearOutSlowInEasing

    fun isForward(old: Dest, new: Dest): Boolean {
        val o = ordered.indexOf(old)
        val n = ordered.indexOf(new)
        return n > o
    }

    fun isSameGroup(old: Dest, new: Dest): Boolean =
        old.parent == new.parent && old.parent != null

    fun isParentToChild(old: Dest, new: Dest): Boolean =
        new.parent == old

    fun isChildToParent(old: Dest, new: Dest): Boolean =
        old.parent == new

    NavDisplay(
        backStack = backStack.backStack,   // <- this MUST be SnapshotStateList<Dest>
        modifier = modifier,
        onBack = { backStack.pop() },

// MAIN TRANSITION
        transitionSpec = {
            val old = resolveDest(initialState.key)
            val new = resolveDest(targetState.key)

            println("TRANSITION: $initialState -> $targetState")

            // fallback for unknown keys
            when {

                (isParentToChild(old, new) || isChildToParent(old, new)) -> {
                    ContentTransform(
                        targetContentEnter = fadeIn(tween(duration, easing = easing)),
                        initialContentExit = fadeOut(tween(duration, easing = easing)),
                        sizeTransform = null
                    )
                }


                // Settings subpages: same parent group
                isSameGroup(old, new) -> {
                    val forward = isForward(old, new)
                    slideInHorizontally(
                        initialOffsetX = { w -> if (forward) w else -w },
                        animationSpec = tween(duration, easing = easing)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { w -> if (forward) -w else w },
                        animationSpec = tween(duration, easing = easing)
                    )
                }

                // Main tabs
                else -> {
                    val forward = isForward(old, new)

                    if (isLandscape) {
                        // LANDSCAPE: vertical slide, same speed both directions
                        slideInVertically(
                            initialOffsetY = { h -> if (forward) h else -h },
                            animationSpec = tween(duration, easing = easing)
                        ) togetherWith slideOutVertically(
                            targetOffsetY = { h -> if (forward) -h else h },
                            animationSpec = tween(duration, easing = easing)
                        )
                    } else {
                        // PORTRAIT: horizontal slide, same speed both directions
                        slideInHorizontally(
                            initialOffsetX = { w -> if (forward) w else -w },
                            animationSpec = tween(duration, easing = easing)
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { w -> if (forward) -w else w },
                            animationSpec = tween(duration, easing = easing)
                        )
                    }
                }
            }
        },
        // POP TRANSITION (back gesture / back press)
        popTransitionSpec = {
            val old = resolveDest(initialState.key)   // from (top) screen
            val new = resolveDest(targetState.key)    // to   (below) screen

            println("POP TRANSITION: $old -> $new")

            when {
                // Parent ⟷ child: fade only
                isParentToChild(old, new) || isChildToParent(old, new) -> {
                    ContentTransform(
                        targetContentEnter = fadeIn(tween(duration, easing = easing)),
                        initialContentExit = fadeOut(tween(duration, easing = easing)),
                        sizeTransform = null
                    )
                }

                // Same settings group, still want slide? Keep / adjust as you like:
                isSameGroup(old, new) -> {
                    val forward = isForward(old, new)
                    slideInHorizontally(
                        initialOffsetX = { w -> if (forward) w else -w },
                        animationSpec = tween(duration, easing = easing)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { w -> if (forward) -w else w },
                        animationSpec = tween(duration, easing = easing)
                    )
                }

                // Default pop slide
                else -> {
                    slideInHorizontally(
                        initialOffsetX = { full -> -full / 4 },
                        animationSpec = tween(duration, easing = easing)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { full -> full / 4 },
                        animationSpec = tween(duration, easing = easing)
                    )
                }
            }
        },
        predictivePopTransitionSpec = {
            val scaleIn = scaleIn(
                initialScale = 0.90f, // new page starts at 90% of size
                animationSpec = tween(duration, easing = easing)
            )

            val scaleOut = scaleOut(
                targetScale = 1.05f, // outgoing page grows slightly (parallax)
                animationSpec = tween(duration, easing = easing)
            )

            val fadeIn = fadeIn(
                animationSpec = tween(duration, easing = easing),
                initialAlpha = 0.8f
            )

            val fadeOut = fadeOut(
                animationSpec = tween(duration, easing = easing),
                targetAlpha = 0.0f
            )


            // Combine scale + fade + slide
            (scaleIn + fadeIn) togetherWith
                    (scaleOut + fadeOut)
        },
        entryProvider = entryProvider {

            entry<Dest.Flashlight> {
                FlashlightPage()
            }

            entry<Dest.Compass> {
                CompassPage()
            }

            entry<Dest.Speedometer> {
                SpeedometerPage()
            }

            entry<Dest.Usage> {
                UsageScreen()
            }

            entry<Dest.Level> {
                LevelPage(modifier = Modifier.layerBackdrop(backdrop))
            }

            entry<Dest.Settings> {
                SettingsPage(onNavigate)
            }

            // SETTINGS SUBPAGES
            entry<Dest.SettingsApp> {
                AppSettingsPage(onBack = { backStack.pop() })
            }

            entry<Dest.SettingsCustomization> {
                CustomizationSettingsPage(onBack = { backStack.pop() })
            }

            entry<Dest.SettingsFlash> {
                FlashlightSettingsPage(onBack = { backStack.pop() })
            }

            entry<Dest.SettingsCompass> {
                CompassSettingsPage(onBack = { backStack.pop() })
            }

            entry<Dest.SettingsSpeedometer> {
                SpeedometerSettingsPage(onBack = { backStack.pop() })
            }

            entry<Dest.SettingsUsage> {
                UsageSettingsPage(onBack = { backStack.pop() })
            }

            entry<Dest.SettingsLevel> {
                LevelSettingsPage(onBack = { backStack.pop() })
            }

            entry<Dest.SettingsTile> {
                TileSettingsPage(onBack = { backStack.pop() })
            }

//            for (dest in ordered) {
//                val clazz = dest::class
//                entry(clazz as KClass<Any>) {
//                    dest.Content(
//                        onNavigate = onNavigate,
//                        onBack = { backStack.pop() },
//                        backdrop = backdrop
//                    )
//                }
//            }
        }

    )
}