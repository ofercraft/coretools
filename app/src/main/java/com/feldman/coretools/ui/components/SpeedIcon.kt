package com.feldman.coretools.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.feldman.coretools.R

@Composable
fun AnimatedSpeedOutlineIcon(
    tint: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val needleRotation = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            // Smooth speedometer-like sweep
            needleRotation.snapTo(0f)
            needleRotation.animateTo(50f, tween(250, easing = LinearEasing))
            needleRotation.animateTo(-50f, tween(250, easing = LinearEasing))
            needleRotation.animateTo(0f, tween(300, easing = LinearEasing))
        } else {
            needleRotation.snapTo(0f)
        }
    }

    Box(modifier.size(28.dp)) {
        // Base outline (dial body)
        Image(
            painter = painterResource(R.drawable.ic_speed_base),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.matchParentSize()
        )

        // Needle outline (rotates around its base)
        Image(
            painter = painterResource(R.drawable.ic_speed_needle),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = needleRotation.value
                    // pivot set to the needle base point (around 418,620 of 960x960)
                    transformOrigin = TransformOrigin(418f / 960f, 620f / 960f)
                }
        )
    }
}
