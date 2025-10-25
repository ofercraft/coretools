package com.feldman.coretools.ui.pages.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.autoFlashlightOffFlow
import com.feldman.coretools.storage.defaultFlashlightLevelFlow
import com.feldman.coretools.storage.instantFlashlightFlow
import com.feldman.coretools.storage.setAutoFlashlightOff
import com.feldman.coretools.storage.setDefaultFlashlightLevel
import com.feldman.coretools.storage.setInstantFlashlight
import com.feldman.coretools.ui.components.SettingCard
import com.feldman.coretools.ui.components.adaptive.AdaptiveSlider
import com.feldman.coretools.ui.components.adaptive.AdaptiveSwitch
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightSettingsPage(navController: NavController = rememberNavController(), padding: Dp = 16.dp, onBack: () -> Unit = { navController.navigateUp() }, showTopBar: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backdrop = rememberLayerBackdrop()
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    val instantFlashlight by context.instantFlashlightFlow().collectAsState(initial = false)
    val defaultFlashlightLevel by context.defaultFlashlightLevelFlow().collectAsState(initial = 100)
    val autoFlashlightOff by context.autoFlashlightOffFlow().collectAsState(initial = true)

    val dark = isDarkTheme()
    Column(Modifier.padding(padding)) {
        if (showTopBar){
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Flashlight setting",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(12.dp))

        }

        SettingCard(
            index = 0,
            isGlass = isGlass,
            backdrop = backdrop,
            dark = dark,
            totalCount = 3
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Instant Flashlight", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                AdaptiveSwitch(
                    selected = { instantFlashlight },
                    onSelect = { scope.launch { context.setInstantFlashlight(it) } },
                    backdrop = rememberLayerBackdrop()
                )
            }
        }
        Spacer(Modifier.height(if(isGlass) 10.dp else 2.dp))

        SettingCard(
            index = 1,
            isGlass = isGlass,
            backdrop = backdrop,
            dark = dark,
            totalCount = 3
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Default brightness: ${defaultFlashlightLevel}%", color = MaterialTheme.colorScheme.onSurface)
                    AdaptiveSlider(
                        level = defaultFlashlightLevel,
                        maxLevel = 100,
                        applyLevel = { newPercent ->
                            scope.launch { context.setDefaultFlashlightLevel(newPercent) }
                        },
                        valueRange = 0f..100.toFloat(),
                        visibilityThreshold = 0.01f,
                        backdrop = backdrop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isGlass) 60.dp else 48.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(if(isGlass) 10.dp else 2.dp))

        SettingCard(
            index = 2,
            isGlass = isGlass,
            backdrop = backdrop,
            dark = dark,
            totalCount = 3
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Auto turn off when leaving",
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                AdaptiveSwitch(
                    selected = { autoFlashlightOff },
                    onSelect = { scope.launch { context.setAutoFlashlightOff(it) } },
                    backdrop = rememberLayerBackdrop()
                )
            }
        }


        Spacer(Modifier.height(24.dp))
    }
}