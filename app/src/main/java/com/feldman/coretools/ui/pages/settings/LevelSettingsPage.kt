package com.feldman.coretools.ui.pages.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.feldman.coretools.BottomSpacer
import com.feldman.coretools.R
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.levelModeFlow
import com.feldman.coretools.storage.levelVibrationFeedbackFlow
import com.feldman.coretools.storage.setLevelMode
import com.feldman.coretools.storage.setLevelVibrationFeedback
import com.feldman.coretools.ui.components.SettingCard
import com.feldman.coretools.ui.components.adaptive.AdaptivePicker
import com.feldman.coretools.ui.components.adaptive.AdaptiveSwitch
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSettingsPage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backdrop = rememberLayerBackdrop()
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass
    val dark = isDarkTheme()

    val vibrationFeedback by context.levelVibrationFeedbackFlow().collectAsState(initial = true)
    val levelMode by context.levelModeFlow().collectAsState(initial = "normal")

    Column(Modifier.padding(16.dp)) {
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
                text = "Level Settings",
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
                Icon(
                    painterResource(R.drawable.ic_vibration),
                    contentDescription = "Vibration Feedback",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Vibration Feedback",
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                AdaptiveSwitch(
                    selected = { vibrationFeedback },
                    onSelect = { scope.launch { context.setLevelVibrationFeedback(it) } },
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
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Level Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                AdaptivePicker(
                    options = listOf("Normal", "Reversed"),
                    selectedIndex = {
                        if (levelMode == "reversed") 1 else 0
                    },
                    onSelected = { index ->
                        scope.launch {
                            context.setLevelMode(if (index == 1) "reversed" else "normal")
                        }
                    },
                    backdrop = rememberLayerBackdrop(),

                    )
            }
        }

        Spacer(Modifier.height(BottomSpacer))
    }
}

