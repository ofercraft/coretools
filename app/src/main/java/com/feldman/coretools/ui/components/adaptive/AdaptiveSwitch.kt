package com.feldman.coretools.ui.components.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.ui.components.liquid.LiquidToggle
import com.feldman.coretools.ui.pages.MaterialSwitch
import com.kyant.backdrop.Backdrop

@Composable
fun AdaptiveSwitch(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = null)
    val isGlass = appStyle == AppStyle.Glass

    if (isGlass) {
        LiquidToggle(
            selected = selected,
            onSelect = onSelect,
            backdrop = backdrop,
            modifier = modifier
        )
    }
    else{
        MaterialSwitch(
            checked = selected(),
            onCheckedChange = onSelect,
            modifier = modifier
        )
    }

}