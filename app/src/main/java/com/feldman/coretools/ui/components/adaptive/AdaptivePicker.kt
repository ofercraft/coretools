package com.feldman.coretools.ui.components.adaptive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.ui.components.liquid.LiquidSegmentedPicker
import com.kyant.backdrop.Backdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptivePicker(
    options: List<String>,
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    icons: List<Painter?>? = null,
) {
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = null)
    val isGlass = appStyle == AppStyle.Glass

    if (isGlass) {
        LiquidSegmentedPicker(
            options = options,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            backdrop = backdrop,
            modifier = modifier.fillMaxWidth(),
            icons = icons
        )
    } else {
        SingleChoiceSegmentedButtonRow(
            modifier = modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    selected = selectedIndex() == index,
                    onClick = { onSelected(index) },
                    icon = {
                        val painter = icons?.getOrNull(index)
                        if (painter != null) {
                            Icon(painter = painter, contentDescription = label)
                        }
                    },
                    label = { Text(label) },
                    modifier = Modifier.height(50.dp)
                )
            }
        }
    }
}