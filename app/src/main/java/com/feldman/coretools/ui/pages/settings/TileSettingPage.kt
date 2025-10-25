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
import com.feldman.coretools.storage.openFullPagesFlow
import com.feldman.coretools.storage.setHideHome
import com.feldman.coretools.storage.setHideSaver
import com.feldman.coretools.storage.setOpenFullPages
import com.feldman.coretools.storage.setShowTileInfo
import com.feldman.coretools.storage.setThemedIcons
import com.feldman.coretools.storage.showTileInfoFlow
import com.feldman.coretools.storage.themedIconsFlow
import com.feldman.coretools.ui.components.SettingCard
import com.feldman.coretools.ui.components.adaptive.AdaptiveSwitch
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileSettingsPage(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backdrop = rememberLayerBackdrop()
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass
    val dark = isDarkTheme()

    val showTileInfo by context.showTileInfoFlow().collectAsState(initial = true)
    val openFullPages by context.openFullPagesFlow().collectAsState(initial = true)


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
                text = "Tile Settings",
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
            totalCount = 2
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Show expanded info in tiles",
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                AdaptiveSwitch(
                    selected = { showTileInfo },
                    onSelect = { scope.launch { context.setShowTileInfo(it) } },
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
            totalCount = 2
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Open full pages",
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                AdaptiveSwitch(
                    selected = { openFullPages },
                    onSelect = { scope.launch { context.setOpenFullPages(it) } },
                    backdrop = rememberLayerBackdrop()
                )
            }
        }

    }
}