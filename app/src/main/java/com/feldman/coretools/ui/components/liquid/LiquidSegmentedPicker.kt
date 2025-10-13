package com.feldman.coretools.ui.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
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
fun LiquidSegmentedPicker(
    options: List<String>,
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    icons: List<Painter?>? = null,
) {
    require(options.isNotEmpty()) { "options must not be empty" }

    val isLightTheme = !isDarkTheme()
    val accentColor = MaterialTheme.colorScheme.primary
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    val tabsCount = options.size
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
                with(density) { 4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }

        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val dampedDragAnimationState = remember { mutableStateOf<DampedDragAnimation?>(null) }

        val dampedDragAnimation = dampedDragAnimationState.value ?: DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex().toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    val snappedValue = targetValue.coerceIn(0f, (tabsCount - 1).toFloat())
                    val targetIdx =
                        if (snappedValue < 0.4f) 0
                        else if (snappedValue > tabsCount - 1 - 0.4f) tabsCount - 1
                        else snappedValue.roundToInt()

                    animationScope.launch {
                        val anim = dampedDragAnimationState.value ?: return@launch
                        val current = anim.value
                        val target = targetIdx.toFloat()
                        Animatable(current).animateTo(
                            target,
                            spring(dampingRatio = 0.8f, stiffness = 300f)
                        ) { anim.updateValue(value) }
                    }

                    if (targetIdx != selectedIndex() || targetIdx == 0) {
                        onSelected(targetIdx)
                    }
                    didDrag = false
                }
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 200f))
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) didDrag = dragAmount.x != 0f
                updateValue(
                    (targetValue + dragAmount.x / tabWidth)
                        .fastCoerceIn(-0.0001f, (tabsCount - 1 + 0.0001f))
                )
                animationScope.launch {
                    offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                }
            }
        ).also { dampedDragAnimationState.value = it }

        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { selectedIndex().toFloat() }
                .collectLatest { index ->
                    if (dampedDragAnimation.targetValue != index) {
                        dampedDragAnimation.animateToValue(index)
                    }
                }
        }
        LaunchedEffect(selectedIndex()) {
            val index = selectedIndex().toFloat()
            if (dampedDragAnimation.targetValue != index) {
                dampedDragAnimation.animateToValue(index)
            }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
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
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEachIndexed { idx, label ->
                val proximity = 1f - abs(dampedDragAnimation.value - idx).fastCoerceIn(0f, 1f)
                val textColor = lerp(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    accentColor,
                    proximity
                )
                val weight = 1f
                val icon = icons?.getOrNull(idx)

                Box(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            animationScope.launch {
                                dampedDragAnimation.animateToValue(idx.toFloat())
                                onSelected(idx)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (icon != null) {
                            Icon(
                                painter = icon,
                                contentDescription = label,
                                tint = textColor,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(end = 6.dp)
                                    .graphicsLayer {
                                        renderEffect = null
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                            )
                        }
                        Text(
                            text = label,
                            color = textColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = lerp(
                                FontWeight.Normal.weight.toFloat(),
                                FontWeight.SemiBold.weight.toFloat(),
                                proximity
                            ).fastRoundToInt().let { FontWeight(it) }
                        )
                    }
                }
            }

        }

        // HIGHLIGHT LAYER (for the glow/vibrancy pass — mirrors your tabs approach)
        Row(
            Modifier
                .clearAndSetSemantics {}
                .alpha(0f) // content invisible; used for highlight pass only
                .layerBackdrop(tabsBackdrop)
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    highlight = {
                        val p = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = p)
                    },
                    effects = {
                        val p = dampedDragAnimation.pressProgress
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx() * p, 24f.dp.toPx() * p)
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(56f.dp)
                .fillMaxWidth()
                .padding(horizontal = 4f.dp)
                .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // mirror the structure to match the tabs' highlight behavior
            options.forEachIndexed { idx, label ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val icon = icons?.getOrNull(idx)
                    val proximity = 1f - abs(dampedDragAnimation.value - idx).fastCoerceIn(0f, 1f)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (icon != null) {
                            Icon(
                                painter = icon,
                                contentDescription = label,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(end = 6.dp)
                                    .graphicsLayer {
                                        renderEffect = null
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = lerp(
                                FontWeight.Normal.weight.toFloat(),
                                FontWeight.SemiBold.weight.toFloat(),
                                proximity
                            ).fastRoundToInt().let { FontWeight(it) }
                        )
                    }
                }
            }
        }

        // MOVING LIQUID CAPSULE
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX = (dampedDragAnimation.value * tabWidth + panelOffset)
                        .coerceAtLeast(0f)
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { ContinuousCapsule },
                    highlight = {
                        val p = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = p)
                    },
                    shadow = {
                        val p = dampedDragAnimation.pressProgress
                        Shadow(alpha = p)
                    },
                    innerShadow = {
                        val p = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 8f.dp * p, alpha = p)
                    },
                    effects = {
                        val p = dampedDragAnimation.pressProgress
                        lens(
                            12f.dp.toPx() * p,
                            12f.dp.toPx() * p,
                            chromaticAberration = true
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val v = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (v * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (v * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val p = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.1f) else Color.White.copy(0.1f),
                            alpha = 1f - p
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * p))
                    }
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}

/**
 * Tiny helper to interpolate floats for FontWeight mapping.
 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
