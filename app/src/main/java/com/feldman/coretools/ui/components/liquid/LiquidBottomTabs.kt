package com.feldman.coretools.ui.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.feldman.coretools.ui.components.utils.DampedDragAnimation
import com.feldman.coretools.ui.components.utils.InteractiveHighlight
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = !isDarkTheme()


    val accentColor = MaterialTheme.colorScheme.primary

    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)
    key(isLightTheme) {
        val tabsBackdrop = rememberLayerBackdrop()

        BoxWithConstraints(
            modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(maxHeight = 64f.dp.roundToPx()))
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val density = LocalDensity.current
            val tabWidth = with(density) {
                (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
            }

            val offsetAnimation = remember { Animatable(0f) }
            val panelOffset by remember(density) {
                derivedStateOf {
                    val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                    with(density) {
                        4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                    }
                }
            }

            val animationScope = rememberCoroutineScope()
            var didDrag by remember { mutableStateOf(false) }
            val dampedDragAnimationState = remember { mutableStateOf<DampedDragAnimation?>(null) }


            val dampedDragAnimation = dampedDragAnimationState.value ?: DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,

                onDragStarted = {},

                onDragStopped = {
                    if (didDrag) {
                        val snappedValue = targetValue.coerceIn(0f, (tabsCount - 1).toFloat())
                        val targetIndex =
                            if (snappedValue < 0.4f) 0
                            else if (snappedValue > tabsCount - 1 - 0.4f) tabsCount - 1
                            else snappedValue.roundToInt()

                        animationScope.launch {
                            val anim = dampedDragAnimationState.value ?: return@launch
                            val current = anim.value
                            val target = targetIndex.toFloat()

                            Animatable(current).animateTo(
                                target,
                                spring(dampingRatio = 0.8f, stiffness = 300f)
                            ) {
                                anim.updateValue(value)
                            }
                        }

                        // ✅ Trigger navigation
                        if (targetIndex != selectedTabIndex() || targetIndex == 0) {
                            onTabSelected(targetIndex)
                        }

                        didDrag = false
                    }

                    // Reset panel offset
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(dampingRatio = 0.8f, stiffness = 200f)
                        )
                    }
                },

                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }

                    updateValue(
                        (targetValue + dragAmount.x / tabWidth)
                            .fastCoerceIn(-0.0001f, (tabsCount - 1 + 0.0001f))
                    )

                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            ).also {
                // Assign it after creation so lambdas can see it
                dampedDragAnimationState.value = it
            }

            LaunchedEffect(dampedDragAnimation) {
                snapshotFlow { selectedTabIndex().toFloat() }
                    .collectLatest { index ->
                        if (dampedDragAnimation.targetValue != index) {
                            dampedDragAnimation.animateToValue(index)
                        }
                    }
            }

            LaunchedEffect(selectedTabIndex()) {
                val index = selectedTabIndex().toFloat()
                if (dampedDragAnimation.targetValue != index) {
                    dampedDragAnimation.animateToValue(index)
                }
            }

            val interactiveHighlight = remember(animationScope) {
                InteractiveHighlight(
                    animationScope = animationScope,
                    position = { size, offset ->
                        Offset(
                            (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                            size.height / 2f
                        )
                    }
                )
            }

            Row(
                Modifier
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(24f.dp.toPx(), 24f.dp.toPx())
                        },
                        layerBlock = {
                            val progress = dampedDragAnimation.pressProgress
                            val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(64f.dp)
                    .fillMaxWidth()
                    .padding(4f.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )

            CompositionLocalProvider(
                LocalLiquidBottomTabScale provides {
                    lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                }
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = progress)
                            },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                vibrancy()
                                blur(8f.dp.toPx())
                                lens(
                                    24f.dp.toPx() * progress,
                                    24f.dp.toPx() * progress
                                )
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .then(interactiveHighlight.modifier)
                        .height(56f.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 4f.dp)
                        .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }

            Box(
                Modifier
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer {
                        // Ensure we can reach index 0 exactly
                        translationX = (dampedDragAnimation.value * tabWidth + panelOffset)
                            .coerceAtLeast(0f)
                    }
                    .then(interactiveHighlight.gestureModifier)
                    .then(dampedDragAnimation.modifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { ContinuousCapsule },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        shadow = {
                            val progress = dampedDragAnimation.pressProgress
                            Shadow(alpha = progress)
                        },
                        innerShadow = {
                            val progress = dampedDragAnimation.pressProgress
                            InnerShadow(
                                radius = 8f.dp * progress,
                                alpha = progress
                            )
                        },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            lens(
                                12f.dp.toPx() * progress,
                                12f.dp.toPx() * progress,
                                chromaticAberration = true
                            )
                        },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            val velocity = dampedDragAnimation.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            drawRect(
                                if (isLightTheme) Color.Black.copy(0.1f)
                                else Color.White.copy(0.1f),
                                alpha = 1f - progress
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        }
                    )
                    .height(56f.dp)
                    .fillMaxWidth(1f / tabsCount)
            )
        }
    }


}


@Composable
fun LiquidSideTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isLightTheme = !isDarkTheme()
    val accentColor = MaterialTheme.colorScheme.primary
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    key(isLightTheme) {
        val tabsBackdrop = rememberLayerBackdrop()
        val animationScope = rememberCoroutineScope()
        val density = LocalDensity.current

        BoxWithConstraints(
            modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(maxWidth = 80.dp.roundToPx()))
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            val tabHeight = with(density) {
                (constraints.maxHeight.toFloat() - 8.dp.toPx()) / tabsCount
            }

            val offsetAnimation = remember { Animatable(0f) }
            val panelOffset by remember(density) {
                derivedStateOf {
                    val fraction = (offsetAnimation.value / constraints.maxHeight).fastCoerceIn(-1f, 1f)
                    with(density) {
                        4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                    }
                }
            }

            var didDrag by remember { mutableStateOf(false) }
            val dampedDragAnimationState = remember { mutableStateOf<DampedDragAnimation?>(null) }

            val dampedDragAnimation = dampedDragAnimationState.value ?: DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        val snappedValue = targetValue.coerceIn(0f, (tabsCount - 1).toFloat())
                        val targetIndex = snappedValue.roundToInt()
                        animationScope.launch {
                            val anim = dampedDragAnimationState.value ?: return@launch
                            Animatable(anim.value).animateTo(
                                targetIndex.toFloat(),
                                spring(dampingRatio = 0.8f, stiffness = 300f)
                            ) { anim.updateValue(value) }
                        }
                        if (targetIndex != selectedTabIndex() || targetIndex == 0) {
                            onTabSelected(targetIndex)
                        }
                        didDrag = false
                    }
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 200f))
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.y != 0f
                    }
                    updateValue(
                        (targetValue - dragAmount.y / tabHeight)
                            .fastCoerceIn(-0.0001f, (tabsCount - 1 + 0.0001f))
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.y)
                    }
                }
            ).also {
                dampedDragAnimationState.value = it
            }

            LaunchedEffect(dampedDragAnimation) {
                snapshotFlow { selectedTabIndex().toFloat() }
                    .collectLatest { index ->
                        if (dampedDragAnimation.targetValue != index) {
                            dampedDragAnimation.animateToValue(index)
                        }
                    }
            }

            LaunchedEffect(selectedTabIndex()) {
                val index = selectedTabIndex().toFloat()
                if (dampedDragAnimation.targetValue != index) {
                    dampedDragAnimation.animateToValue(index)
                }
            }

            val interactiveHighlight = remember(animationScope) {
                InteractiveHighlight(
                    animationScope = animationScope,
                    position = { size, _ ->
                        Offset(
                            size.width / 2f,
                            (dampedDragAnimation.value + 0.5f) * tabHeight + panelOffset
                        )
                    }
                )
            }

            // 🔷 Background container (vertical)
            Column(
                Modifier
                    .graphicsLayer { translationY = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(24.dp.toPx(), 24.dp.toPx())
                        },
                        layerBlock = {
                            val progress = dampedDragAnimation.pressProgress
                            val scale = lerp(1f, 1f + 12.dp.toPx() / size.height, progress)
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .fillMaxHeight()
                    .width(72.dp)
                    .padding(vertical = 4.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                content = content
            )

            // 🔘 Draggable liquid overlay
            Box(
                Modifier
                    .padding(vertical = 4.dp)
                    .graphicsLayer {
                        translationY =
                            (dampedDragAnimation.value * tabHeight + panelOffset).coerceAtLeast(0f)
                    }
                    .then(interactiveHighlight.gestureModifier)
                    .then(dampedDragAnimation.modifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { ContinuousCapsule },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        shadow = {
                            val progress = dampedDragAnimation.pressProgress
                            Shadow(alpha = progress)
                        },
                        innerShadow = {
                            val progress = dampedDragAnimation.pressProgress
                            InnerShadow(radius = 8.dp * progress, alpha = progress)
                        },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            lens(
                                12.dp.toPx() * progress,
                                12.dp.toPx() * progress,
                                chromaticAberration = true
                            )
                        },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            val velocity = dampedDragAnimation.velocity / 10f
                            scaleY /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleX *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            drawRect(
                                if (isLightTheme) Color.Black.copy(0.1f)
                                else Color.White.copy(0.1f),
                                alpha = 1f - progress
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        }
                    )
                    .width(72.dp)
                    .fillMaxHeight(1f / tabsCount)
            )
        }
    }
}
