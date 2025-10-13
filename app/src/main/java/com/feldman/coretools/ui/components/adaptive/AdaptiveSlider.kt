package com.feldman.coretools.ui.components.adaptive

import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.ui.components.liquid.LiquidSlider
import com.feldman.coretools.ui.tiles.isCloseTo
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.FlowPreview
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSlider(
    level: Int,
    maxLevel: Int,
    applyLevel: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = null)
    val isGlass = appStyle == AppStyle.Glass

    if (isGlass) {
        var value by rememberSaveable { mutableFloatStateOf(level.toFloat()) }

        LaunchedEffect(level) {
            if (!value.isCloseTo(level.toFloat(), epsilon = 0.5f)) {
                value = level.toFloat()
            }
        }

        LiquidSlider(
            value = { value },
            onValueChange = { value = it },
            valueRange = valueRange,
            visibilityThreshold = visibilityThreshold,
            backdrop = backdrop,
            modifier = modifier
                .height(60.dp),
        )

        LaunchedEffect(value) {
            snapshotFlow { value }
                .debounce(250)
                .collectLatest {
                    val newLevel = it.toInt().coerceIn(0, maxLevel)
                    if (newLevel != level) applyLevel(newLevel)
                }
        }

    } else {
        var sliderValue by rememberSaveable { mutableFloatStateOf(level.toFloat()) }

        LaunchedEffect(level) {
            if (!sliderValue.isCloseTo(level.toFloat(), epsilon = 0.5f)) {
                sliderValue = level.toFloat()
            }
        }

        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                val newLevel = it.toInt().coerceIn(0, maxLevel)
                if (newLevel != level) applyLevel(newLevel)
            },
            valueRange = valueRange,
            modifier = modifier,
            track = { sliderState ->
                SliderDefaults.Track(
                    modifier = Modifier.height(28.dp),
                    sliderState = sliderState
                )
            },
        )
    }
}