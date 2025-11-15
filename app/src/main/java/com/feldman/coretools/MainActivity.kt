@file:Suppress("PROPERTY_WONT_BE_SERIALIZED")

package com.feldman.coretools

import android.app.Application
import android.content.pm.ActivityInfo
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.os.Parcelable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.feldman.coretools.Dest.Compass
import com.feldman.coretools.Dest.Flashlight
import com.feldman.coretools.Dest.Level
import com.feldman.coretools.Dest.Settings
import com.feldman.coretools.Dest.SettingsApp
import com.feldman.coretools.Dest.SettingsCompass
import com.feldman.coretools.Dest.SettingsCustomization
import com.feldman.coretools.Dest.SettingsFlash
import com.feldman.coretools.Dest.SettingsLevel
import com.feldman.coretools.Dest.SettingsSpeedometer
import com.feldman.coretools.Dest.SettingsTile
import com.feldman.coretools.Dest.SettingsUsage
import com.feldman.coretools.Dest.Speedometer
import com.feldman.coretools.Dest.Usage
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.OrientationMode
import com.feldman.coretools.storage.PrefKeys
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.dataStore
import com.feldman.coretools.storage.defaultPageFlow
import com.feldman.coretools.storage.orientationModeFlow
import com.feldman.coretools.ui.components.AnimatedSpeedOutlineIcon
import com.feldman.coretools.ui.components.SideAppBar
import com.feldman.coretools.ui.components.liquid.CustomLiquidBottomBar
import com.feldman.coretools.ui.components.liquid.LiquidBottomTab
import com.feldman.coretools.ui.components.liquid.LiquidBottomTabs
import com.feldman.coretools.ui.components.material.CustomFloatingBottomBar
import com.feldman.coretools.ui.navigation.AppNavHost
import com.feldman.coretools.ui.pages.CompassPage
import com.feldman.coretools.ui.pages.FlashlightPage
import com.feldman.coretools.ui.pages.LevelPage
import com.feldman.coretools.ui.pages.SettingsPage
import com.feldman.coretools.ui.pages.SpeedometerPage
import com.feldman.coretools.ui.pages.UsageScreen
import com.feldman.coretools.ui.pages.scaledWidth
import com.feldman.coretools.ui.pages.settings.AppSettingsPage
import com.feldman.coretools.ui.pages.settings.CompassSettingsPage
import com.feldman.coretools.ui.pages.settings.CustomizationSettingsPage
import com.feldman.coretools.ui.pages.settings.FlashlightSettingsPage
import com.feldman.coretools.ui.pages.settings.LevelSettingsPage
import com.feldman.coretools.ui.pages.settings.SpeedometerSettingsPage
import com.feldman.coretools.ui.pages.settings.TileSettingsPage
import com.feldman.coretools.ui.pages.settings.UsageSettingsPage
import com.feldman.coretools.ui.theme.AppTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer


val LocalAppOrientation = compositionLocalOf { OrientationMode.AUTO }
val BottomSpacer = 100.dp

@Serializable
sealed interface Dest : NavKey, Parcelable {
    val label: String
    val filledIcon: Int
    val outlineIcon: Int
    val parent: Dest? get() = null

    @Composable
    fun Content(
        onNavigate: (Dest) -> Unit,
        onBack: () -> Unit,
        backdrop: LayerBackdrop
    )

    // MAIN
    @Parcelize
    @Serializable
    data object Flashlight : Dest {
        override val label = "Flash"
        override val filledIcon = R.drawable.ic_flashlight
        override val outlineIcon = R.drawable.ic_flashlight_outline

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            FlashlightPage()
        }
    }

    @Parcelize
    @Serializable
    data object Compass : Dest {
        override val label = "Compass"
        override val filledIcon = R.drawable.ic_compass
        override val outlineIcon = R.drawable.ic_compass_outline

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            CompassPage()
        }
    }

    @Parcelize
    @Serializable
    data object Speedometer : Dest {
        override val label = "Speed"
        override val filledIcon = R.drawable.ic_speed
        override val outlineIcon = R.drawable.ic_speed_outline

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            SpeedometerPage()
        }
    }

    @Parcelize
    @Serializable
    data object Usage : Dest {
        override val label = "Usage"
        override val filledIcon = R.drawable.ic_usage
        override val outlineIcon = R.drawable.ic_usage_outline

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            UsageScreen()
        }
    }

    @Parcelize
    @Serializable
    data object Level : Dest {
        override val label = "Level"
        override val filledIcon = R.drawable.ic_level
        override val outlineIcon = R.drawable.ic_level_outline

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            val backdrop = rememberLayerBackdrop()
            LevelPage(modifier = Modifier.layerBackdrop(backdrop))
        }
    }

    @Parcelize
    @Serializable
    data object Settings : Dest {
        override val label = "Settings"
        override val filledIcon = R.drawable.ic_settings
        override val outlineIcon = R.drawable.ic_settings_outline

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            SettingsPage(onNavigate)
        }
    }

    // SETTINGS SUBPAGES
    @Parcelize @Serializable
    data object SettingsApp : Dest {
        override val label = "App Settings"
        override val filledIcon = R.drawable.ic_apps
        override val outlineIcon = R.drawable.ic_apps
        override val parent = Settings

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            AppSettingsPage(onBack = onBack)
        }
    }

    @Parcelize @Serializable
    data object SettingsCustomization : Dest {
        override val label = "Customization"
        override val filledIcon = R.drawable.ic_palette
        override val outlineIcon = R.drawable.ic_palette_outline
        override val parent = Settings

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            CustomizationSettingsPage(onBack = onBack)
        }
    }

    @Parcelize @Serializable
    data object SettingsFlash : Dest {
        override val label = "Flash Settings"
        override val filledIcon = R.drawable.ic_flashlight
        override val outlineIcon = R.drawable.ic_flashlight_outline
        override val parent = Settings

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            FlashlightSettingsPage(onBack = onBack)
        }
    }

    @Parcelize @Serializable
    data object SettingsCompass : Dest {
        override val label = "Compass Settings"
        override val filledIcon = R.drawable.ic_compass
        override val outlineIcon = R.drawable.ic_compass_outline
        override val parent = Settings

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            CompassSettingsPage(onBack = onBack)
        }
    }

    @Parcelize @Serializable
    data object SettingsSpeedometer : Dest {
        override val label = "Speedometer Settings"
        override val filledIcon = R.drawable.ic_speed
        override val outlineIcon = R.drawable.ic_speed_outline
        override val parent = Settings

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            SpeedometerSettingsPage(onBack = onBack)
        }
    }

    @Parcelize @Serializable
    data object SettingsUsage : Dest {
        override val label = "Usage Settings"
        override val filledIcon = R.drawable.ic_usage
        override val outlineIcon = R.drawable.ic_usage_outline
        override val parent = Settings

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            UsageSettingsPage(onBack = onBack)
        }
    }

    @Parcelize @Serializable
    data object SettingsLevel : Dest {
        override val label = "Level Settings"
        override val filledIcon = R.drawable.ic_level
        override val outlineIcon = R.drawable.ic_level_outline
        override val parent = Settings

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            LevelSettingsPage(onBack = onBack)
        }
    }
    @Parcelize @Serializable
    data object SettingsTile : Dest {
        override val label = "Tile Settings"
        override val filledIcon = R.drawable.ic_tile
        override val outlineIcon = R.drawable.ic_tile
        override val parent = Settings

        @Composable
        override fun Content(
            onNavigate: (Dest) -> Unit,
            onBack: () -> Unit,
            backdrop: LayerBackdrop
        ) {
            TileSettingsPage(onBack = onBack)
        }
    }

}

public val ordered = listOf(
    Flashlight,
    Compass,
    Speedometer,
    Usage,
    Level,
    Settings,
    SettingsApp,
    SettingsCustomization,
    SettingsFlash,
    SettingsCompass,
    SettingsSpeedometer,
    SettingsUsage,
    SettingsLevel,
    SettingsTile
)

public val topLevel = listOf(
    Flashlight,
    Compass,
    Speedometer,
    Usage,
    Level,
    Settings
)


class DestBackStack(start: Dest) {

    private var stacks = linkedMapOf(
        start to mutableStateListOf(start)
    )

    var currentTop by mutableStateOf(start)
        private set

    val backStack = mutableStateListOf(start)

    private fun sync() {
        backStack.apply {
            clear()
            addAll(stacks.flatMap { it.value })
        }
    }

    fun navigateTop(dest: Dest) {
        if (stacks[dest] == null) {
            stacks[dest] = mutableStateListOf(dest)
        } else {
            val existing = stacks.remove(dest)!!
            stacks[dest] = existing
        }
        currentTop = dest
        sync()
    }

    fun navigate(dest: Dest) {
        stacks[currentTop]?.add(dest)
        sync()
    }

    fun pop() {
        val removed = stacks[currentTop]?.removeLastOrNull()
        stacks.remove(removed)
        currentTop = stacks.keys.last()
        sync()
    }
}

//
//@Serializable
//enum class DestEntries(
//    val label: String,
//    val filledIcon: Int,
//    val outlineIcon: Int,
//    val parent: DestEntries? = null
//) : NavKey {
//    Flashlight("Flash", R.drawable.ic_flashlight, R.drawable.ic_flashlight_outline),
//    Compass("Compass", R.drawable.ic_compass, R.drawable.ic_compass_outline),
//    Speedometer("Speed", R.drawable.ic_speed, R.drawable.ic_speed_outline),
//    Usage("Usage", R.drawable.ic_usage, R.drawable.ic_usage_outline),
//    Level("Level", R.drawable.ic_level, R.drawable.ic_level_outline),
//    Settings("Settings", R.drawable.ic_settings, R.drawable.ic_settings_outline),
//
//    SettingsApp("App Settings", R.drawable.ic_apps, R.drawable.ic_apps, parent = Settings),
//    SettingsCustomization("Customization Settings", R.drawable.ic_palette, R.drawable.ic_palette_outline, parent = Settings),
//    SettingsFlash("Flash Settings", R.drawable.ic_flashlight, R.drawable.ic_flashlight_outline, parent = Settings),
//    SettingsCompass("Compass Settings", R.drawable.ic_compass, R.drawable.ic_compass_outline, parent = Settings),
//    SettingsSpeedometer("Speedometer Settings", R.drawable.ic_speed, R.drawable.ic_speed_outline, parent = Settings),
//    SettingsUsage("Usage Settings", R.drawable.ic_usage, R.drawable.ic_usage_outline, parent = Settings),
//    SettingsLevel("Level Settings", R.drawable.ic_level, R.drawable.ic_level_outline, parent = Settings),
//    SettingsTile("Tile Settings", R.drawable.ic_tile, R.drawable.ic_tile, parent = Settings)
//}
//
//
////
////@Serializable
////sealed class Dest(val old: DestEntries) : NavKey, Parcelable {
////    val label get() = old.label
////    val filledIcon get() = old.filledIcon
////    val outlineIcon get() = old.outlineIcon
////
////    val parent: Dest?
////        get() = old.parent?.let { map[it] }
////
////    @Parcelize
////    @Serializable
////    private class Impl(val entry: DestEntries) : Dest(entry), Parcelable
////
////    companion object {
////        // ✅ use Impl instead of anonymous object
////        val map: Map<DestEntries, Dest> by lazy {
////            DestEntries.entries.associateWith { entry -> Impl(entry) }
////        }
////
////        val entries: List<Dest> get() = map.values.toList()
////    }
////}
//
//@Serializable
//enum class Dest(
//    val label: String,
//    val filledIcon: Int,
//    val outlineIcon: Int,
//    val parent: Dest? = null
//) : NavKey {
//    // --- MAIN DESTINATIONS ---
//    Flashlight("Flash", R.drawable.ic_flashlight, R.drawable.ic_flashlight_outline),
//    Compass("Compass", R.drawable.ic_compass, R.drawable.ic_compass_outline),
//    Speedometer("Speed", R.drawable.ic_speed, R.drawable.ic_speed_outline),
//    Usage("Usage", R.drawable.ic_usage, R.drawable.ic_usage_outline),
//    Level("Level", R.drawable.ic_level, R.drawable.ic_level_outline),
//    Settings("Settings", R.drawable.ic_settings, R.drawable.ic_settings_outline),
//
//    // --- SETTINGS SUBPAGES ---
//    SettingsApp(
//        "App Settings",
//        R.drawable.ic_apps,
//        R.drawable.ic_apps,
//        parent = Settings
//    ),
//    SettingsCustomization(
//        "Customization Settings",
//        R.drawable.ic_palette,
//        R.drawable.ic_palette_outline,
//        parent = Settings
//    ),
//    SettingsFlash(
//        "Flash Settings",
//        R.drawable.ic_flashlight,
//        R.drawable.ic_flashlight_outline,
//        parent = Settings
//    ),
//    SettingsCompass(
//        "Compass Settings",
//        R.drawable.ic_compass,
//        R.drawable.ic_compass_outline,
//        parent = Settings
//    ),
//    SettingsSpeedometer(
//        "Speedometer Settings",
//        R.drawable.ic_speed,
//        R.drawable.ic_speed_outline,
//        parent = Settings
//    ),
//    SettingsUsage(
//        "Usage Settings",
//        R.drawable.ic_usage,
//        R.drawable.ic_usage_outline,
//        parent = Settings
//    ),
//    SettingsLevel(
//        "Level Settings",
//        R.drawable.ic_level,
//        R.drawable.ic_level_outline,
//        parent = Settings
//    ),
//    SettingsTile(
//        "Tile Settings",
//        R.drawable.ic_tile,
//        R.drawable.ic_tile,
//        parent = Settings
//    );
//}


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {



    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {

            dataStore.data.map { prefs ->
                OrientationMode.fromKey(prefs[PrefKeys.ORIENTATION_MODE])
            }.collect { mode ->

                requestedOrientation = when (mode) {
                    OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }

                setContent {
                    AppTheme {
                        MainApp()
                    }
                }
            }
        }
    }

}
//@Serializable
//sealed class DestKey : NavKey {
//    abstract val dest: Dest
//
//    @Serializable
//    data class DestNav(val d: Dest) : DestKey() {
//        override val dest: Dest get() = d
//    }
//}



@Composable
fun MainApp(startDestination: Dest? = null) {
    val context = LocalContext.current
    val backdrop = rememberLayerBackdrop()

    // App style (material vs glass)
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    // Default page from preferences
    val defaultPage by context.defaultPageFlow().collectAsState(initial = Dest.Flashlight)
    val startKey = startDestination ?: defaultPage

    val backStack = remember(startKey) { DestBackStack(startKey) }

    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

    // Unified navigation entry point
    val onNavigate: (Dest) -> Unit = { dest ->
        val topParent = dest.parent ?: dest
        val currentTop = backStack.currentTop

        if (topParent != currentTop) {
            // Jump to another top-level root
            backStack.navigateTop(topParent)
            if (dest != topParent) {
                backStack.navigate(dest)
            }
        } else {
            // Navigate inside same root stack
            backStack.navigate(dest)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (!isLandscape) {
                if (!isGlass) {
                    CustomFloatingBottomBar(
                        backStack = backStack,
                        onNavigate = { dest -> backStack.navigateTop(dest) }
                    )
                } else {
                    CustomLiquidBottomBar(
                        backStack = backStack,
                        onNavigate = { dest -> backStack.navigateTop(dest) },
                        backdrop = backdrop
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        val layoutDir = LocalLayoutDirection.current
        val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
        val railWidth = scaledWidth(92.dp)
        val leftInset = safeInsets.calculateLeftPadding(layoutDir)

        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            AppNavHost(
                backStack = backStack,
                onNavigate = onNavigate,
                modifier = Modifier
//                    .padding(padding)
//                    .padding(4.dp)
                    .layerBackdrop(backdrop)
//                    .windowInsetsPadding(
//                        WindowInsets.systemBars.only(WindowInsetsSides.Top)
//                    )
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .then(
                        if (isLandscape)
                            Modifier.padding(start = railWidth + leftInset)
                        else
                            Modifier
                    ),
                backdrop = backdrop
            )

            if (isLandscape) {
                SideAppBar(
                    backStack = backStack,
                    onNavigate = { dest -> backStack.navigateTop(dest) },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
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

@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = LocalContentColor.current,
    maxLines: Int = 1,
    minTextSize: TextUnit = 8.sp,
    maxTextSize: TextUnit = style.fontSize,
) {
    var textStyle by remember { mutableStateOf(style.copy(fontSize = maxTextSize, textAlign = TextAlign.Center)) }
    var readyToDraw by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            style = textStyle,
            maxLines = maxLines,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            onTextLayout = { layoutResult ->
                if (layoutResult.didOverflowWidth && textStyle.fontSize > minTextSize) {
                    textStyle = textStyle.copy(fontSize = textStyle.fontSize * 0.9)
                } else {
                    readyToDraw = true
                }
            },
            modifier = Modifier.alpha(if (readyToDraw) 1f else 0f)
        )
    }
}

