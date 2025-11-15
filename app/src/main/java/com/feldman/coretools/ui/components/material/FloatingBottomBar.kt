package com.feldman.coretools.ui.components.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.feldman.coretools.AutoResizeText
import com.feldman.coretools.CompactNavigationBarItem
import com.feldman.coretools.Dest
import com.feldman.coretools.DestBackStack
import com.feldman.coretools.R
import com.feldman.coretools.topLevel
import com.feldman.coretools.ui.components.AnimatedSpeedOutlineIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomFloatingBottomBar(
    backStack: DestBackStack,
    onNavigate: (Dest) -> Unit
) {
    val current = backStack.currentTop
    val currentTop = current.parent ?: current

    val topDestinations = topLevel
    println(topDestinations)
    var expanded by rememberSaveable { mutableStateOf(true) }

    val exitBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom
    )
    val toolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        HorizontalFloatingToolbar(
            expanded = expanded,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                .zIndex(1f),
            colors = toolbarColors,
            scrollBehavior = exitBehavior,
        ) {

            topDestinations.forEach { dest ->

                val isSelected = (currentTop == dest)

                val offsetY = remember { Animatable(0f) }
                val rotation = remember { Animatable(0f) }
                val usageScales =
                    if (dest == Dest.Usage) List(3) { Animatable(1f) } else null

                LaunchedEffect(isSelected) {
                    if (isSelected) {
                        when (dest) {
                            Dest.Flashlight -> {
                                offsetY.snapTo(0f)
                                offsetY.animateTo(-8f, tween(200) { LinearEasing.transform(it) })
                                offsetY.animateTo(0f, tween(200) { LinearEasing.transform(it) })
                            }

                            Dest.Compass -> {
                                rotation.snapTo(0f)
                                rotation.animateTo(20f, tween(75) { LinearEasing.transform(it) })
                                rotation.animateTo(-20f, tween(150) { LinearEasing.transform(it) })
                                rotation.animateTo(0f, tween(75) { LinearEasing.transform(it) })
                            }

                            Dest.Usage -> {
                                usageScales!!.forEach { it.snapTo(1f) }
                                usageScales.forEach { s ->
                                    s.animateTo(1.4f, tween(150) { LinearEasing.transform(it) })
                                }
                                usageScales.forEach { s ->
                                    launch {
                                        s.animateTo(0.8f, tween(200) { LinearEasing.transform(it) })
                                        s.animateTo(1f, tween(150) { LinearEasing.transform(it) })
                                    }
                                }
                            }

                            Dest.Level -> {
                                rotation.snapTo(0f)
                                rotation.animateTo(16f, tween(120) { LinearEasing.transform(it) })
                                rotation.animateTo(-16f, tween(240) { LinearEasing.transform(it) })
                                rotation.animateTo(0f, tween(120) { LinearEasing.transform(it) })
                            }

                            Dest.Settings -> {
                                rotation.snapTo(0f)
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

                CompactNavigationBarItem(
                    selected = isSelected,
                    onClick = { onNavigate(dest) },
                    icon = {
                        val tint =
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface

                        when {
                            // Usage 3-part animated icon
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
                                println(dest)
                                println(dest)
                                println(dest)
                                Icon(
                                    painter = painterResource(
                                        if (isSelected) dest.filledIcon else dest.outlineIcon
                                    ),
                                    contentDescription = dest.label,
                                    tint = tint,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .graphicsLayer {
                                            rotationZ = rotation.value
                                            translationY = offsetY.value.dp.toPx()
                                        }
                                )
                            }
                        }
                    },
                    label = {
                        AutoResizeText(
                            text = dest.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            minTextSize = 8.sp,
                            maxTextSize = 13.sp
                        )
                    },
                    alwaysShowLabel = true
                )
            }
        }
    }
}
