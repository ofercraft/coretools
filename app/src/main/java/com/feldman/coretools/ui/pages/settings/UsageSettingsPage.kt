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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.hideHomeFlow
import com.feldman.coretools.storage.hideSaverFlow
import com.feldman.coretools.storage.setHideHome
import com.feldman.coretools.storage.setHideSaver
import com.feldman.coretools.storage.setThemedIcons
import com.feldman.coretools.storage.themedIconsFlow
import com.feldman.coretools.ui.components.SettingCard
import com.feldman.coretools.ui.components.adaptive.AdaptiveSwitch
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageSettingsPage(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backdrop = rememberLayerBackdrop()
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass
    val dark = isDarkTheme()

    val hideHome by context.hideHomeFlow().collectAsState(initial = true)
    val hideSaver by context.hideSaverFlow().collectAsState(initial = true)
    val themedIcons by context.themedIconsFlow().collectAsState(initial = false)


    Column(Modifier.padding(16.dp)) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Usage Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(12.dp))

        SettingCard(
            index = 0,
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
                    "Hide Home apps (Launchers)",
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                AdaptiveSwitch(
                    selected = { hideHome },
                    onSelect = { scope.launch { context.setHideHome(it) } },
                    backdrop = rememberLayerBackdrop()
                )
            }
        }

        Spacer(Modifier.height(if (isGlass) 10.dp else 2.dp))

        SettingCard(
            index = 1,
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
                    "Hide Screensaver apps",
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                AdaptiveSwitch(
                    selected = { hideSaver },
                    onSelect = { scope.launch { context.setHideSaver(it) } },
                    backdrop = rememberLayerBackdrop()
                )
            }
        }

        Spacer(Modifier.height(if (isGlass) 10.dp else 2.dp))

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
                    "Enable Themed Icons",
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                AdaptiveSwitch(
                    selected = { themedIcons },
                    onSelect = { scope.launch { context.setThemedIcons(it) } },
                    backdrop = rememberLayerBackdrop()
                )
            }
        }
    }
}