//package com.feldman.coretools.ui.pages
//
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Check
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.feldman.coretools.MainActivity.Dest
//import com.feldman.coretools.storage.AppStyle
//import com.feldman.coretools.storage.PrefKeys
//import com.feldman.coretools.R
//import com.feldman.coretools.storage.Filters
//import com.feldman.coretools.storage.appStyleFlow
//import com.feldman.coretools.storage.autoFlashlightOffFlow
//import com.feldman.coretools.storage.compassShapeFlow
//import com.feldman.coretools.storage.compassVibrationFeedbackFlow
//import com.feldman.coretools.storage.dataStore
//import com.feldman.coretools.storage.defaultFlashlightLevelFlow
//import com.feldman.coretools.storage.defaultPageFlow
//import com.feldman.coretools.storage.filtersFlow
//import com.feldman.coretools.storage.instantFlashlightFlow
//import com.feldman.coretools.storage.levelModeFlow
//import com.feldman.coretools.storage.levelVibrationFeedbackFlow
//import com.feldman.coretools.storage.setAppStyle
//import com.feldman.coretools.storage.setAutoFlashlightOff
//import com.feldman.coretools.storage.setCompassShape
//import com.feldman.coretools.storage.setCompassVibrationFeedback
//import com.feldman.coretools.storage.setDefaultFlashlightLevel
//import com.feldman.coretools.storage.setDefaultPage
//import com.feldman.coretools.storage.setHideHome
//import com.feldman.coretools.storage.setHideSaver
//import com.feldman.coretools.storage.setInstantFlashlight
//import com.feldman.coretools.storage.setLevelMode
//import com.feldman.coretools.storage.setLevelVibrationFeedback
//import com.feldman.coretools.storage.setShowIntercardinals
//import com.feldman.coretools.storage.setShowWeatherRow
//import com.feldman.coretools.storage.setThemeColor
//import com.feldman.coretools.storage.setThemedIcons
//import com.feldman.coretools.storage.setTrueNorth
//import com.feldman.coretools.storage.showIntercardinalsFlow
//import com.feldman.coretools.storage.showWeatherRowFlow
//import com.feldman.coretools.storage.trueNorthFlow
//import com.feldman.coretools.ui.components.SettingCard
//import com.feldman.coretools.ui.components.adaptive.AdaptivePicker
//import com.feldman.coretools.ui.components.adaptive.AdaptiveSlider
//import com.feldman.coretools.ui.components.adaptive.AdaptiveSwitch
//import com.feldman.coretools.ui.theme.isDarkTheme
//import com.kyant.backdrop.backdrops.rememberLayerBackdrop
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.launch
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CompassSettingsPage(navController: NavController) {
//    val context = LocalContext.current
//    val scope = rememberCoroutineScope()
//
//    val compassShape by context.compassShapeFlow().collectAsState(initial = 0)
//    val showIntercardinals by context.showIntercardinalsFlow().collectAsState(initial = true)
//    val showWeatherRow by context.showWeatherRowFlow().collectAsState(initial = true)
//    val trueNorth by context.trueNorthFlow().collectAsState(initial = false)
//    val vibrationFeedback by context.compassVibrationFeedbackFlow().collectAsState(initial = true)
//
//
//    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Playful)
//    val isGlass = appStyle == AppStyle.Glass
//
//    val backdrop = rememberLayerBackdrop()
//    val dark = isDarkTheme()
//
//    Column(Modifier.padding(16.dp)) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(56.dp)
//        ) {
//            IconButton(onClick = { navController.navigateUp() }) {
//                Icon(
//                    Icons.AutoMirrored.Filled.ArrowBack,
//                    contentDescription = "Back",
//                    tint = MaterialTheme.colorScheme.onSurface
//                )
//            }
//            Text(
//                text = "Compass Settings",
//                style = MaterialTheme.typography.titleLarge,
//                color = MaterialTheme.colorScheme.onSurface
//            )
//        }
//        Spacer(Modifier.height(8.dp))
//
//        SettingCard(
//            index = 0,
//            isGlass = isGlass,
//            backdrop = backdrop,
//            dark = dark,
//            totalCount = 5
//        ) {
//            Column(Modifier.fillMaxWidth().padding(16.dp)) {
//                Text("Compass Shape", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
//                Spacer(Modifier.height(8.dp))
//
//                CompassShapePicker(
//                    currentShapeId = compassShape,
//                    onShapeChange = { id -> scope.launch { context.setCompassShape(id) } },
//                    isGlass,
//                    backdrop
//                )
//            }
//        }
//        Spacer(Modifier.height(if(isGlass) 10.dp else 2.dp))
//
//        SettingCard(
//            index = 1,
//            isGlass = isGlass,
//            backdrop = backdrop,
//            dark = dark,
//            totalCount = 5
//        ) {
//            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
//                Text("Show inter cardinals", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
//                AdaptiveSwitch(
//                    selected = { showIntercardinals },
//                    onSelect = { scope.launch { context.setShowIntercardinals(it) } },
//                    backdrop = rememberLayerBackdrop()
//                )
//
//            }
//        }
//        Spacer(Modifier.height(if(isGlass) 10.dp else 2.dp))
//
//        SettingCard(
//            index = 2,
//            isGlass = isGlass,
//            backdrop = backdrop,
//            dark = dark,
//            totalCount = 5
//        ) {
//            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
//                Text("Show weather row", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
//                AdaptiveSwitch(
//                    selected = { showWeatherRow },
//                    onSelect = { scope.launch { context.setShowWeatherRow(it) } },
//                    backdrop = rememberLayerBackdrop()
//                )
//            }
//        }
//        Spacer(Modifier.height(if(isGlass) 10.dp else 2.dp))
//
//        SettingCard(
//            index = 3,
//            isGlass = isGlass,
//            backdrop = backdrop,
//            dark = dark,
//            totalCount = 5
//        ) {
//            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
//                Text("Use True North", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
//                AdaptiveSwitch(
//                    selected = { trueNorth },
//                    onSelect = { scope.launch { context.setTrueNorth(it) } },
//                    backdrop = rememberLayerBackdrop()
//                )
//            }
//        }
//
//        Spacer(Modifier.height(if(isGlass) 10.dp else 2.dp))
//
//        SettingCard(
//            index = 4,
//            isGlass = isGlass,
//            backdrop = backdrop,
//            dark = dark,
//            totalCount = 5
//        ) {
//
//            Row(
//                Modifier.fillMaxWidth().padding(16.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    "Vibration Feedback",
//                    Modifier.weight(1f),
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//                AdaptiveSwitch(
//                    selected = { vibrationFeedback },
//                    onSelect = { scope.launch { context.setCompassVibrationFeedback(it) } },
//                    backdrop = rememberLayerBackdrop()
//                )
//            }
//        }
//    }
//}
//
//
//
//
