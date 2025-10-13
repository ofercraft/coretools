package com.feldman.coretools.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.MainActivity.Dest
import com.feldman.coretools.R
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.ui.components.ActionRow
import com.feldman.coretools.ui.components.SegmentedOption
import com.feldman.coretools.ui.components.Title
import com.kyant.backdrop.backdrops.rememberLayerBackdrop


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = null)
    val isGlass = appStyle == AppStyle.Glass

    if (appStyle == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    val backdrop = rememberLayerBackdrop()

    Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
        Title("Settings")

        ActionRow {
            addVerticalActionList(
                options = listOf(
                    SegmentedOption("app", text = "App", desc = "Set default page and layout", iconRes = R.drawable.ic_apps),
                    SegmentedOption("customization", text = "Customization", desc = "Customize the app style and feel", iconRes = R.drawable.ic_palette),
                    SegmentedOption("flashlight", text = "Flashlight", desc = "Instant flashlight, default brightness...", iconRes = R.drawable.ic_flashlight),
                    SegmentedOption("compass", text = "Compass", desc = "Adjust Compass shape, show inter-cardinals, use true north...", iconRes = R.drawable.ic_compass),
                    SegmentedOption("usage", text = "Usage", desc = "Adjust usage settings", iconRes = R.drawable.ic_usage),
                    SegmentedOption("level", text = "Level", desc = "Adjust level settings", iconRes = R.drawable.ic_level),
                    SegmentedOption("tile", text = "Tile", desc = "Adjust tiles settings", iconRes = R.drawable.ic_tile),
                ),
                onClick = { option ->
                    when (option) {
                        "app" -> navController.navigate(Dest.SettingsApp.name)
                        "customization" -> navController.navigate(Dest.SettingsCustomization.name)
                        "compass" -> navController.navigate(Dest.SettingsCompass.name)
                        "flashlight" -> navController.navigate(Dest.SettingsFlash.name)
                        "usage" -> navController.navigate(Dest.SettingsUsage.name)
                        "level" -> navController.navigate(Dest.SettingsLevel.name)
                        "tile" -> navController.navigate(Dest.SettingsTile.name)
                    }
                },
                isGlass = isGlass,
                backdrop = backdrop
            )

        }
    }

}
