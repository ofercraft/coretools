package com.feldman.coretools.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RowScope.SensorCard(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    shape: Shape,
    color: Color,
    index: Int,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    speed: Long = 80L
) {
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    var shape = shape

    val square = MaterialShapes.Square.toShape()

    if (isGlass){
        shape = square
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * speed)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CardScale"
    )
    val dark = isDarkTheme()
    if (isGlass) {

        Box(
            modifier = modifier
                .weight(1f)
                .aspectRatio(1f)
                .graphicsLayer {
                    alpha = if (visible) 1f else 0f
                    this.shape = RoundedCornerShape(cornerRadius)
                    clip = true
                    transformOrigin = TransformOrigin.Center
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            Column(
                modifier = Modifier
//                    .drawBackdrop(
//                        backdrop = backdrop,
//                        effects =  {
//                            vibrancy()
//                            blur(12.dp.toPx())
//                            lens(
//                                refractionHeight = 40.dp.toPx(),
//                                refractionAmount = 120.dp.toPx(),
//                                depthEffect = true
//                            )
//                        },
//                        shape = { RoundedCornerShape(cornerRadius) },
//                        onDrawSurface = { drawRect(if(dark) Color(0xFF313131).copy(alpha = 0.2f) else Color(0xFFBDBDBD).copy(alpha = 0.2f) ) }
//                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,

            ) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.titleMedium, color= MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color= MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    else {

        ElevatedCard(
            modifier = modifier
                .weight(1f)
                .aspectRatio(1f)
                .graphicsLayer {
                    alpha = if (visible) 1f else 0f
                    this.shape = shape
                    clip = true
                    transformOrigin = TransformOrigin.Center
                    scaleX = scale
                    scaleY = scale
                },
            colors = CardDefaults.elevatedCardColors(containerColor = color),
            shape = shape,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColumnScope.SensorCard(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    shape: Shape,
    color: Color,
    index: Int,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    speed: Long = 80L
) {
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    var shape = shape

    val square = MaterialShapes.Square.toShape()

    if (isGlass){
        shape = square
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * speed)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CardScale"
    )
    val dark = isDarkTheme()
    if (isGlass) {

        Box(
            modifier = modifier
                .weight(1f)
                .aspectRatio(1f)
                .graphicsLayer {
                    alpha = if (visible) 1f else 0f
                    this.shape = RoundedCornerShape(cornerRadius)
                    clip = true
                    transformOrigin = TransformOrigin.Center
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            Column(
                modifier = Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        effects =  {
                            vibrancy()
                            blur(12.dp.toPx())
                            lens(
                                refractionHeight = 40.dp.toPx(),
                                refractionAmount = 120.dp.toPx(),
                                depthEffect = true
                            )
                        },
                        shape = { RoundedCornerShape(cornerRadius) },
                        onDrawSurface = { drawRect(if(dark) Color(0xFF313131).copy(alpha = 0.2f) else Color(0xFFBDBDBD).copy(alpha = 0.2f) ) }

                    )
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,

                ) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.titleMedium, color= MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color= MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    else {

        ElevatedCard(
            modifier = modifier
                .weight(1f)
                .aspectRatio(1f)
                .graphicsLayer {
                    alpha = if (visible) 1f else 0f
                    this.shape = shape
                    clip = true
                    transformOrigin = TransformOrigin.Center
                    scaleX = scale
                    scaleY = scale
                },
            colors = CardDefaults.elevatedCardColors(containerColor = color),
            shape = shape,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}