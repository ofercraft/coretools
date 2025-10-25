package com.feldman.coretools.ui.pages.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.feldman.coretools.MainActivity.Dest
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.defaultPageFlow
import com.feldman.coretools.storage.setDefaultPage
import com.feldman.coretools.ui.components.SettingCard
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsPage(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backdrop = rememberLayerBackdrop()
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    val defaultDest by context.defaultPageFlow().collectAsState(initial = Dest.Flashlight)

    val dark = isDarkTheme()
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
                text = "App Settings",
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
            totalCount = 1
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

                DefaultPagePicker(currentDest = defaultDest) { selected ->
                    scope.launch {
                        context.setDefaultPage(selected)
                    }
                }
            }
        }

    }

}


@Composable
fun DefaultPagePicker(
    currentDest: Dest,
    onPick: (Dest) -> Unit
) {
    Column {
        Text("Default Page", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Dest.entries.filter { it.parent == null }.forEach { dest ->
            val isSelected = currentDest == dest
            val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(dest) }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(dest.outlineIcon),
                    contentDescription = dest.label,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))
                Text(dest.label, color = color)
                Spacer(modifier = Modifier.weight(1f))
                if (currentDest == dest) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = color)
                }
            }
        }
    }
}