package com.feldman.coretools.ui.pages.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.feldman.coretools.BottomSpacer
import com.feldman.coretools.R
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.OrientationMode
import com.feldman.coretools.storage.PrefKeys
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.dataStore
import com.feldman.coretools.storage.orientationModeFlow
import com.feldman.coretools.storage.setAppStyle
import com.feldman.coretools.storage.setOrientationMode
import com.feldman.coretools.storage.setThemeColor
import com.feldman.coretools.ui.components.SettingCard
import com.feldman.coretools.ui.components.adaptive.AdaptivePicker
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationSettingsPage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)

    val themeColorFlow = remember { context.dataStore.data.map { it[PrefKeys.THEME_COLOR] ?: 0 } }
    val themeColor by themeColorFlow.collectAsState(initial = 0)
    val orientationMode by context.orientationModeFlow().collectAsState(initial = OrientationMode.AUTO)

    val appStyleKeys: List<String> = AppStyle.entries.map {
        it.key.replaceFirstChar { c ->
            if (c.isLowerCase()) c.titlecase() else c.toString()
        }
    }

    val isGlass = appStyle == AppStyle.Glass

    val backdrop = rememberLayerBackdrop()
    val dark = isDarkTheme()



    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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
                text = "Customization",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))


        SettingCard(
            index = 0,
            isGlass = isGlass,
            backdrop = backdrop,
            dark = dark,
            totalCount = 3
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("App Style", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))


                AdaptivePicker(
                    options = appStyleKeys,
                    selectedIndex = { AppStyle.entries.indexOf(appStyle) },
                    onSelected = { index ->
                        val style = AppStyle.entries[index]
                        scope.launch { context.setAppStyle(style) }
                    },
                    backdrop = rememberLayerBackdrop(),
                    icons = listOf(
                        painterResource(R.drawable.ic_animation),
                        painterResource(R.drawable.ic_blur)
                    ),
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
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Theme Color", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))

                AdaptivePicker(
                    options = listOf("Auto", "Light", "Dark"),
                    selectedIndex = { themeColor },
                    onSelected = {
                            index -> scope.launch { context.setThemeColor(index) }
                    },
                    backdrop = rememberLayerBackdrop()
                )
            }
        }
        Spacer(Modifier.height(if (isGlass) 10.dp else 2.dp))

        // --- Orientation Mode ---
        SettingCard(
            index = 2,
            isGlass = isGlass,
            backdrop = backdrop,
            dark = dark,
            totalCount = 3
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Orientation",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))

                val orientationOptions = listOf("Auto", "Portrait", "Landscape")

                AdaptivePicker(
                    options = orientationOptions,
                    selectedIndex = { OrientationMode.entries.indexOf(orientationMode) },
                    onSelected = { index ->
                        val selected = OrientationMode.entries[index]
                        scope.launch { context.setOrientationMode(selected) }
                    },
                    backdrop = rememberLayerBackdrop(),
                    icons = listOf(
                        painterResource(R.drawable.ic_auto),
                        painterResource(R.drawable.ic_portrait),
                        painterResource(R.drawable.ic_landscape)
                    )
                )
            }
        }

        Spacer(Modifier.height(BottomSpacer))

    }
}