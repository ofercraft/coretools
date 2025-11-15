package com.feldman.coretools.ui.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feldman.coretools.Dest
import com.feldman.coretools.DestBackStack
import com.feldman.coretools.R
import com.feldman.coretools.topLevel
import com.feldman.coretools.ui.components.AnimatedSpeedOutlineIcon
import com.kyant.backdrop.Backdrop

@Composable
fun CustomLiquidBottomBar(
    backStack: DestBackStack,
    onNavigate: (Dest) -> Unit,
    backdrop: Backdrop
) {
    val rootDestinations = topLevel

    // Current selection based on top-level destination
    val current = backStack.currentTop
    val selectedTabIndex = rootDestinations.indexOfFirst {
        it == current || current.parent == it
    }.coerceAtLeast(0)

    LiquidBottomTabs(
        selectedTabIndex = { selectedTabIndex },
        onTabSelected = { index ->
            onNavigate(rootDestinations[index])
        },
        backdrop = backdrop,
        tabsCount = rootDestinations.size,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 32.dp)
    ) {
        rootDestinations.forEachIndexed { index, dest ->
            val isSelected = selectedTabIndex == index
            val tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            // Animations
            val offsetY = remember { Animatable(0f) }
            val rotation = remember { Animatable(0f) }
            val usageScales = if (dest == Dest.Usage) List(3) { Animatable(1f) } else null

            LaunchedEffect(isSelected) {
                if (isSelected) {
                    when (dest) {
                        Dest.Flashlight -> offsetY.animateTo(
                            -8f,
                            tween(200) { LinearEasing.transform(it) }
                        )

                        Dest.Compass -> {
                            rotation.animateTo(20f, tween(75) { LinearEasing.transform(it) })
                            rotation.animateTo(-20f, tween(150) { LinearEasing.transform(it) })
                            rotation.animateTo(0f, tween(75) { LinearEasing.transform(it) })
                        }

                        Dest.Usage -> {
                            usageScales!!.forEach { it.snapTo(1f) }
                            usageScales.forEach {
                                it.animateTo(1.4f, tween(150) { LinearEasing.transform(it) })
                            }
                        }

                        Dest.Level -> {
                            rotation.animateTo(16f, tween(120) { LinearEasing.transform(it) })
                            rotation.animateTo(-16f, tween(240) { LinearEasing.transform(it) })
                            rotation.animateTo(0f, tween(120) { LinearEasing.transform(it) })
                        }

                        Dest.Settings -> {
                            rotation.animateTo(360f, tween(300) { LinearEasing.transform(it) })
                            rotation.snapTo(0f)
                        }

                        else -> Unit
                    }
                } else {
                    offsetY.snapTo(0f)
                    rotation.snapTo(0f)
                    usageScales?.forEach { it.snapTo(1f) }
                }
            }

            // Tab
            LiquidBottomTab(
                onClick = { onNavigate(dest) },
                modifier = Modifier.weight(1f)
            ) {

                // Icon
                when {
                    dest == Dest.Usage && isSelected -> {
                        val scales = usageScales!!
                        Box(Modifier.size(28.dp)) {
                            Image(
                                painter = painterResource(R.drawable.ic_usage_part1),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(tint),
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        scaleX = scales[0].value
                                        scaleY = scales[0].value
                                    }
                            )
                            Image(
                                painter = painterResource(R.drawable.ic_usage_part2),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(tint),
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        scaleX = scales[1].value
                                        scaleY = scales[1].value
                                    }
                            )
                            Image(
                                painter = painterResource(R.drawable.ic_usage_part3),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(tint),
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        scaleX = scales[2].value
                                        scaleY = scales[2].value
                                    }
                            )
                        }
                    }

                    dest == Dest.Speedometer && isSelected -> {
                        AnimatedSpeedOutlineIcon(
                            tint = tint,
                            isSelected = true
                        )
                    }

                    else -> {
                        Box(
                            Modifier
                                .size(28.dp)
                                .graphicsLayer {
                                    rotationZ = rotation.value
                                    translationY = offsetY.value.dp.toPx()
                                }
                                .paint(
                                    painterResource(
                                        if (isSelected) dest.filledIcon else dest.outlineIcon
                                    ),
                                    colorFilter = ColorFilter.tint(tint)
                                )
                        )
                    }
                }

                // Label
                BasicText(
                    text = dest.label,
                    style = TextStyle(
                        color = tint,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
