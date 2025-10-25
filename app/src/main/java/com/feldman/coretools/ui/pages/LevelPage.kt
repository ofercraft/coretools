package com.feldman.coretools.ui.pages

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import com.feldman.coretools.R
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.LevelAngles
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import com.feldman.coretools.LevelEngine
import com.feldman.coretools.MainActivity.Dest
import com.feldman.coretools.MainApp
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.levelModeFlow
import com.feldman.coretools.storage.levelVibrationFeedbackFlow
import com.feldman.coretools.ui.components.SensorCard
import com.feldman.coretools.ui.theme.isDarkTheme
import com.feldman.coretools.ui.theme.primary2
import com.feldman.coretools.ui.theme.primary5
import com.feldman.coretools.ui.theme.textColor
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay

@Composable
fun rememberLevelEngine(): LevelEngine {
    val context = LocalContext.current
    val engine = remember { LevelEngine(context) }
    DisposableEffect(Unit) {
        onDispose { engine.close() }
    }
    return engine
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LevelPage(modifier: Modifier = Modifier) {
    val engine = rememberLevelEngine()
    val angles by engine.anglesFlow.collectAsState(initial = LevelAngles())
    val context = LocalContext.current
    val vibrationFeedback by context.levelVibrationFeedbackFlow().collectAsState(initial = true)
    val levelMode by context.levelModeFlow().collectAsState(initial = "normal")
    val reversed = levelMode == "reversed"

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE

    val options2 = listOf("Horizontal", "Combined", "Vertical")
    val unCheckedIcons = listOf(R.drawable.ic_level, R.drawable.ic_circle, R.drawable.level_vertical)
    val checkedIcons   = listOf(R.drawable.ic_level, R.drawable.ic_circle_filled, R.drawable.level_vertical)
    var selectedIndex by remember { mutableIntStateOf(1) }
    val backdrop = rememberLayerBackdrop()
    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🎛 Picker column — fixed width on the far left
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .padding(end = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    options2.forEachIndexed { index, label ->
                        ToggleButton(
                            checked = selectedIndex == index,
                            onCheckedChange = { selectedIndex = index },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { role = Role.RadioButton },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                options2.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (selectedIndex == index)
                                        checkedIcons[index]
                                    else
                                        unCheckedIcons[index]
                                ),
                                contentDescription = label
                            )
                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp)
            ) {
                when (selectedIndex) {
                    0 -> HorizontalLevel(angles, reversed, vibrationFeedback, backdrop = backdrop)
                    1 -> CombinedPage(angles, modifier, reversed, backdrop, vibrationFeedback)
                    2 -> VerticalLevel(angles, reversed, vibrationFeedback, backdrop)
                }
            }
        }
    }
    else {
        Row(
            modifier = modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            options2.forEachIndexed { index, label ->
                ToggleButton(
                    checked = selectedIndex == index,
                    onCheckedChange = {
                        selectedIndex = index
                    },
                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    shapes = when (index) {
                        0              -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options2.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else           -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                ) {
                    Icon(
                        painter = painterResource( id = if (selectedIndex == index) checkedIcons[index] else unCheckedIcons[index] ),
                        contentDescription = label
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        softWrap = false
                    )
                }
            }
        }
        Box(
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth()
        ){
            if (selectedIndex==0){
                HorizontalLevel(angles, reversed, vibrationFeedback, backdrop)
            }
            else if (selectedIndex==1){
                CombinedPage(angles, modifier, reversed, backdrop, vibrationFeedback)
            }
            else if (selectedIndex==2){
                VerticalLevel(angles, reversed, vibrationFeedback, backdrop)
            }
        }

    }
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CombinedCanvas(
    angles: LevelAngles,
    reversed: Boolean,
    backdrop: Backdrop,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val dark = isDarkTheme()

    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    if (isGlass) {
        Box(
            modifier = Modifier
                .size(350.dp)
                .clip(MaterialShapes.Circle.toShape())
                .drawBackdrop(
                    backdrop = backdrop,
                    effects =  {
                        vibrancy()
                        blur(4.dp.toPx())
                        lens(
                            refractionHeight = 24.dp.toPx(),
                            refractionAmount = 60.dp.toPx(),
                            depthEffect = true
                        )
                    },
                    shape = { RoundedCornerShape(percent = 50) },
                    onDrawSurface = { drawRect(if(dark) Color(0xFF313131).copy(alpha = 0.2f) else Color(0xFFBDBDBD).copy(alpha = 0.2f)) }
                )
                .background(Color.Transparent)

        )
    }
    Canvas(modifier = Modifier.size(350.dp)) {
        val radius = size.minDimension / 2.2f
        val cx = center.x
        val cy = center.y

        val circleCenter = Offset(cx, cy)
        var lineColor = if (dark) Color.Black else Color.White
        if (isGlass) {
            lineColor = if (!dark) Color.Black else Color.White
        }
        val strokeWidth = 4f

        if(!isGlass){
            // Outer circle
            drawCircle(
                color = primaryColor,
                radius = radius,
                center = circleCenter,
                style = Fill
            )

            // Circle border
            drawCircle(
                color = lineColor,
                radius = radius,
                center = circleCenter,
                style = Stroke(width = strokeWidth)
            )
        }

        // Center crosshair
        val crossLength = radius * 0.25f
        val crossStroke = 12f
        drawLine(
            color = lineColor,
            start = Offset(cx - crossLength, cy),
            end = Offset(cx + crossLength, cy),
            strokeWidth = crossStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = lineColor,
            start = Offset(cx, cy - crossLength),
            end = Offset(cx, cy + crossLength),
            strokeWidth = crossStroke,
            cap = StrokeCap.Round
        )

        // Bubble (white position dot)
        val bubbleR = radius * 0.15f
        val maxDisp = radius - bubbleR - 8.dp.toPx()

        var dx = -sin(angles.dirRad) * angles.frac * maxDisp
        var dy = -cos(angles.dirRad) * angles.frac * maxDisp

        if (reversed){
            dx = -dx
            dy = -dy
        }
        val bubbleCenter = Offset(cx + dx, cy + dy)
        drawCircle(
            color = if (isGlass) primaryColor else Color.White,
            radius = bubbleR,
            center = bubbleCenter,
            style = Fill
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CombinedPage(
    angles: LevelAngles,
    modifier: Modifier,
    reversed: Boolean,
    backdrop: Backdrop,
    vibration: Boolean,
    ) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer
    val dark = isDarkTheme()

    val archShape = ScaledShape(MaterialShapes.Arch.toShape(), scale = 1.2f)
    val cookie4Shape = ScaledShape(MaterialShapes.Cookie4Sided.toShape(), scale = 1.4f)

    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val lastVibrationTime = remember { mutableLongStateOf(0L) }

    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

    if (vibration){
        LaunchedEffect(angles.horizAbsDeg, angles.vertAbsDeg) {
            // Combine horizontal and vertical deviation for total "off-level" feedback
            val combinedAngle = kotlin.math.sqrt(
                (angles.horizAbsDeg * angles.horizAbsDeg + angles.vertAbsDeg * angles.vertAbsDeg)
                    .coerceAtMost(900).toDouble()
            )
            val now = System.currentTimeMillis()
            val deadZoneDeg = 1.5f

            // Faster feedback near level, slower when tilted far
            val vibrationDelay = (150 + (combinedAngle / 30f) * 450)
                .toLong()
                .coerceIn(150, 600)

            if (now - lastVibrationTime.longValue < vibrationDelay) return@LaunchedEffect

            if (combinedAngle > deadZoneDeg && combinedAngle < 30f) {
                val duration = (10 + (combinedAngle / 30f) * 40).toLong() // 10–50 ms
                val amplitude = (40 + (combinedAngle / 30f) * 200).toInt().coerceIn(40, 255)
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(duration, amplitude)
                )
                lastVibrationTime.longValue = now
            } else if (combinedAngle <= deadZoneDeg) {
                // Gentle tick when perfectly level
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(25, 60)
                )
                lastVibrationTime.longValue = now
            }
        }
    }


    if (isLandscape) {
        // LANDSCAPE: Canvas left, cards right
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Canvas (left)
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .fillMaxWidth(0.6f) // 👈 about 60% of total width
                    .aspectRatio(1f) // keeps it circular
                    .padding(end = 16.dp), // space between canvas and right column
                contentAlignment = Alignment.Center
            ) {
                CombinedCanvas(angles, reversed, backdrop)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.Start
            ) {
                SensorCard(
                    index = 1,
                    icon = R.drawable.ic_level,
                    label = "Horizontal",
                    value = "${angles.horizAbsDeg}°",
                    shape = archShape,
                    color = secondaryContainerColor,
                    backdrop = backdrop,
                    cornerRadius = 70.dp
                )

                SensorCard(
                    index = 2,
                    icon = R.drawable.level_vertical,
                    label = "Vertical",
                    value = "${angles.vertAbsDeg}°",
                    shape = cookie4Shape,
                    color = secondaryContainerColor,
                    backdrop = backdrop,
                    cornerRadius = 70.dp
                )


            }
        }
    }
    else{
        Box(
            modifier = modifier.fillMaxSize().padding(top=100.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            CombinedCanvas(angles, reversed, backdrop)


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top=400.dp)
                    .padding(horizontal = if (isGlass) 18.dp else 36.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isGlass) 10.dp else 50.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SensorCard(
                    index = 1,
                    icon  = R.drawable.ic_level,
                    label = "Horizontal",
                    value = "${angles.horizAbsDeg}°",
                    shape = archShape,
                    color = secondaryContainerColor,
                    backdrop = backdrop,
                    cornerRadius = 70.dp
                )
                SensorCard(
                    index = 2,
                    icon  = R.drawable.level_vertical,
                    label = "Vertical",
                    value = "${angles.vertAbsDeg}°",
                    shape = cookie4Shape,
                    color = secondaryContainerColor,
                    backdrop = backdrop,
                    cornerRadius = 70.dp
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HorizontalLevel(
    angles: LevelAngles,
    reversed: Boolean,
    vibration: Boolean,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val textColor = textColor()
    val circleShape = MaterialShapes.Circle.toShape()
    val animatedHorizDeg = animateFloatAsState(
        targetValue = angles.horizSignedDeg,
        animationSpec = tween(durationMillis = 120)
    )
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer
    val cookie4Shape = ScaledShape(MaterialShapes.Cookie4Sided.toShape(), scale = 1.4f)
    val ghostShape = ScaledShape(MaterialShapes.Ghostish.toShape(), scale = 1.3f)



    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    val background = MaterialTheme.colorScheme.background
    val deadZoneDeg = 1.5f
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    val lastVibrationTime = remember { mutableLongStateOf(0L) }

    if (vibration){
        LaunchedEffect(animatedHorizDeg.value) {
            val absAngle = abs(animatedHorizDeg.value)
            val now = System.currentTimeMillis()

            val vibrationDelay = (150 + (absAngle / 30f) * 450).toLong().coerceIn(150, 600)

            if (now - lastVibrationTime.longValue < vibrationDelay) return@LaunchedEffect

            if (absAngle > deadZoneDeg && absAngle < 30f) {
                val duration = (10 + (absAngle / 30f) * 40).toLong()
                val amplitude = (40 + (absAngle / 30f) * 200).toInt()
                    .coerceIn(40, 255)
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(duration, amplitude)
                )
                lastVibrationTime.longValue = now
            } else if (absAngle <= deadZoneDeg) {
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(25, 60)
                )
                lastVibrationTime.longValue = now
            }
        }
    }


    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE



    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = if (isLandscape) 0.dp else 100.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            val barHeight   = 100.dp.toPx()
            val trackRadius = barHeight / 2f
            val bubbleR     = barHeight * 0.42f
            val margin      = 8.dp.toPx()

            val left  = 0f
            val right = size.width
            val top   = cy - barHeight / 2f
            val rectSize = Size(width = right - left, height = barHeight)

            drawRoundRect(
                color = primary,
                topLeft = Offset(left, top),
                size = rectSize,
                cornerRadius = CornerRadius(trackRadius, trackRadius)
            )

            val crossLength = 25.dp.toPx()
            val crossStroke = 12f
            drawLine(
                color = background,
                start = Offset(cx - crossLength, cy),
                end = Offset(cx + crossLength, cy),
                strokeWidth = crossStroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = background,
                start = Offset(cx, cy - crossLength),
                end = Offset(cx, cy + crossLength),
                strokeWidth = crossStroke,
                cap = StrokeCap.Round
            )

            // Displacement range for the bubble
            val halfTrack  = rectSize.width / 2f
            val innerRange = halfTrack - bubbleR - margin


            // Bubble logic
            var dx = (animatedHorizDeg.value / 90f) * innerRange
            if (reversed) dx = -dx
            if (abs(animatedHorizDeg.value) <= deadZoneDeg) dx = 0f

            val bubbleCx = cx + dx
            val bubbleCy = cy
            val markerSize = 100f
            val halfMarker = markerSize / 2f

            val circleOutline = circleShape.createOutline(
                size = Size(markerSize, markerSize),
                layoutDirection = layoutDirection,
                density = this
            )

            translate(left = cx + dx - halfMarker, top = cy - halfMarker) {
                rotate(degrees = 0f, pivot = Offset(bubbleCx, bubbleCy)) {
                    drawOutline(outline = circleOutline, color = textColor, style = Fill)
                    drawOutline(outline = circleOutline, color = textColor, style = Stroke(width = 4f))
                }
            }
        }


        val displayHorizAbs = angles.horizAbsDeg
        val displayVertAbs  = angles.vertAbsDeg

        Spacer(Modifier.height(if(isLandscape) 12.dp else 24.dp))
        Row(
            modifier = Modifier
                .then(
                    if (isLandscape)
                        Modifier.padding(60.dp)

                    else
                        Modifier.padding(16.dp)

                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isGlass) 10.dp else 60.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
                SensorCard(
                    index = 1,
                    icon  = R.drawable.ic_level,
                    label = "Horizontal",
                    value = "$displayHorizAbs°",
                    shape = ghostShape,
                    color = secondaryContainerColor,
                    backdrop = backdrop,
                    cornerRadius = 70.dp
                )
                SensorCard(
                    index = 2,
                    icon  = R.drawable.ic_level,
                    label = "Vertical",
                    value = "$displayVertAbs°",
                    shape = cookie4Shape,
                    color = secondaryContainerColor,
                    backdrop = backdrop,
                    cornerRadius = 70.dp
                )
            }
        }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VerticalLevel(
    angles: LevelAngles,
    reversed: Boolean,
    vibration: Boolean,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer
    val background = MaterialTheme.colorScheme.background
    val textColor = textColor()
    val archShape = ScaledShape(MaterialShapes.Arch.toShape(), scale = 0.9f)
    val cookie4Shape = ScaledShape(MaterialShapes.Cookie4Sided.toShape(), scale = 0.95f)
    val circleShape = MaterialShapes.Circle.toShape()

    val animatedVertDeg by animateFloatAsState(targetValue = angles.vertSignedDeg,  animationSpec = tween(120))

    val displayVertAbs  = angles.vertAbsDeg
    val displayHorizAbs = angles.horizAbsDeg

    val deadZoneDeg = 1.5f
    val context = LocalContext.current

    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val lastVibrationTime = remember { mutableLongStateOf(0L) }
    if (vibration){
        LaunchedEffect(animatedVertDeg) {
            val absAngle = abs(animatedVertDeg)
            val now = System.currentTimeMillis()

            val vibrationDelay = (150 + (absAngle / 30f) * 450)
                .toLong()
                .coerceIn(150, 600)

            if (now - lastVibrationTime.longValue < vibrationDelay) return@LaunchedEffect

            if (absAngle > deadZoneDeg && absAngle < 30f) {
                val duration = (10 + (absAngle / 30f) * 40).toLong()   // 10–50ms
                val amplitude = (40 + (absAngle / 30f) * 200).toInt().coerceIn(40, 255)
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(duration, amplitude)
                )
                lastVibrationTime.longValue = now
            } else if (absAngle <= deadZoneDeg) {
                // gentle pulse when exactly level
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(25, 60)
                )
                lastVibrationTime.longValue = now
            }
        }
    }




    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE

    Row(
        modifier = modifier
            .then(
                if (isLandscape)
                    Modifier.padding(0.dp).fillMaxSize()
                else
                    Modifier.padding(16.dp).height(500.dp)
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .then(
                        if (isLandscape)
                            Modifier.fillMaxHeight()

                        else
                            Modifier.height(500.dp)

                    )
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                val barWidth = 100.dp.toPx()
                val trackRadius = barWidth / 2f
                val bubbleR = barWidth * 0.42f
                val margin = 8.dp.toPx()

                val left = cx - barWidth / 2f
                val rectSize = Size(width = barWidth, height = size.height)

                // Track background
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(left, 0f),
                    size = rectSize,
                    cornerRadius = CornerRadius(trackRadius, trackRadius)
                )

                // Crosshair
                val crossLength = 25.dp.toPx()
                val crossStroke = 12f
                drawLine(
                    color = background,
                    start = Offset(cx - crossLength, cy),
                    end = Offset(cx + crossLength, cy),
                    strokeWidth = crossStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = background,
                    start = Offset(cx, cy - crossLength),
                    end = Offset(cx, cy + crossLength),
                    strokeWidth = crossStroke,
                    cap = StrokeCap.Round
                )

                // Bubble movement
                val halfTrack = size.height / 2f
                val innerRange = halfTrack - bubbleR - margin

                var dy = (animatedVertDeg / 90f) * innerRange
                if (reversed) dy = -dy
                if (abs(animatedVertDeg) <= deadZoneDeg) dy = 0f

                val bubbleCx = cx
                val bubbleCy = cy + dy
                val markerSize = 100f
                val halfMarker = markerSize / 2f

                val circleOutline = circleShape.createOutline(
                    size = Size(markerSize, markerSize),
                    layoutDirection = layoutDirection,
                    density = this
                )

                translate(left = bubbleCx - halfMarker, top = bubbleCy - halfMarker) {
                    drawOutline(outline = circleOutline, color = textColor, style = Fill)
                    drawOutline(outline = circleOutline, color = textColor, style = Stroke(width = 4f))
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {

            SensorCard(
                index = 1,
                icon  = R.drawable.level_vertical,
                label = "Vertical",
                value = "$displayVertAbs°",
                shape = cookie4Shape,
                color = secondaryContainerColor,
                backdrop = backdrop
            )


            SensorCard(
                index = 2,
                icon  = R.drawable.ic_level,
                label = "Horizontal",
                value = "$displayHorizAbs°",
                shape = archShape,
                color = secondaryContainerColor,
                backdrop = backdrop
            )

        }


    }
}


class LevelPageActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainApp(startDestination = Dest.Level)

        }
    }
}
