package com.feldman.coretools.ui.pages

import com.feldman.coretools.R
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.speedUnitFlow
import com.feldman.coretools.ui.components.SensorCard
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SpeedometerPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val backdrop = rememberLayerBackdrop()
    val isGlass = appStyle == AppStyle.Glass

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var speed by remember { mutableStateOf(0f) }
    var topSpeed by remember { mutableStateOf(0f) }
    var minSpeed by remember { mutableStateOf(Float.MAX_VALUE) }
    var avgSpeed by remember { mutableStateOf(0f) }
    var totalSpeed by remember { mutableStateOf(0f) }
    var sampleCount by remember { mutableStateOf(0) }

    val speedUnit by context.speedUnitFlow().collectAsState(initial = "kmh")

    // 🔄 Update speed every second
    LaunchedEffect(locationPermission.status, speedUnit) {
        if (locationPermission.status == PermissionStatus.Granted) {
            while (true) {
                val loc = getLastLocationSafe(context)
                if (loc != null) {
                    val rawSpeed = loc.speed // m/s
                    val converted = if (speedUnit == "kmh") rawSpeed * 3.6f else rawSpeed * 2.23694f
                    speed = converted
                    topSpeed = maxOf(topSpeed, converted)
                    minSpeed = minOf(minSpeed, converted)
                    totalSpeed += converted
                    sampleCount++
                    avgSpeed = if (sampleCount > 0) totalSpeed / sampleCount else 0f
                }
                delay(1000)
            }
        }
    }

    if (locationPermission.status is PermissionStatus.Denied) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { locationPermission.launchPermissionRequest() }) {
                Text("Allow GPS Access")
            }
        }
        return
    }

    // 🧭 Detect orientation
    val isLandscape =
        androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (!isLandscape) {
        // 📱 Portrait → Gauge on top, stats below
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SpeedometerContent(
                    speed = speed,
                    isGlass = isGlass,
                    backdrop = backdrop,
                    isKmh = speedUnit == "kmh"
                )
            }

            Spacer(Modifier.height(16.dp))

            SpeedStatsRow(
                minSpeed = if (minSpeed == Float.MAX_VALUE) 0f else minSpeed,
                avgSpeed = avgSpeed,
                maxSpeed = topSpeed,
                backdrop = backdrop,
                isGlass = isGlass,
                isKmh = speedUnit == "kmh",
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    } else {
        // 💻 Landscape → Gauge left, stats right
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gauge (left)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                SpeedometerContent(
                    speed = speed,
                    isGlass = isGlass,
                    backdrop = backdrop,
                    isKmh = speedUnit == "kmh"
                )
            }

            // Stats (right)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SpeedStatsRow(
                    minSpeed = if (minSpeed == Float.MAX_VALUE) 0f else minSpeed,
                    avgSpeed = avgSpeed,
                    maxSpeed = topSpeed,
                    backdrop = backdrop,
                    isGlass = isGlass,
                    isKmh = speedUnit == "kmh"
                )
            }
        }
    }
}




@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SpeedStatsRow(
    minSpeed: Float,
    avgSpeed: Float,
    maxSpeed: Float,
    backdrop: Backdrop,
    isGlass: Boolean,
    modifier: Modifier = Modifier,
    isKmh: Boolean = true
) {
//    val dark = isDarkTheme()
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

    val cookie4Shape = ScaledShape(MaterialShapes.Cookie4Sided.toShape(), scale = 1.4f)
    val archShape = ScaledShape(MaterialShapes.Arch.toShape(), scale = 1.2f)
    val squareShape = ScaledShape(MaterialShapes.Square.toShape(), scale = 1.2f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isGlass) 10.dp else 30.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 🟦 Min speed
        SensorCard(
            icon = R.drawable.ic_down,
            label = "Min",
            value = "${minSpeed.roundToInt()} ${if (isKmh) "km/h" else "mp/h"}",
            shape = squareShape,
            color = secondaryContainer,
            index = 1,
            backdrop = backdrop,
            cornerRadius = 40.dp
        )

        // 🟨 Avg speed
        SensorCard(
            icon = R.drawable.ic_speed,
            label = "Avg",
            value = "${avgSpeed.roundToInt()} ${if (isKmh) "km/h" else "mp/h"}",
            shape = cookie4Shape,
            color = secondaryContainer,
            index = 2,
            backdrop = backdrop,
            cornerRadius = 40.dp
        )

        // 🟥 Max speed
        SensorCard(
            icon = R.drawable.ic_up,
            label = "Max",
            value = "${maxSpeed.roundToInt()} ${if (isKmh) "km/h" else "mp/h"}",
            shape = archShape,
            color = secondaryContainer,
            index = 3,
            backdrop = backdrop,
            cornerRadius = 40.dp
        )
    }
}


private suspend fun getLastLocationSafe(context: Context): Location? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) return null
    val fused = LocationServices.getFusedLocationProviderClient(context)
    return try {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            fused.lastLocation
                .addOnSuccessListener { cont.resume(it, null) }
                .addOnFailureListener { cont.resume(null, null) }
        }
    } catch (_: Exception) { null }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SpeedometerContent(
    speed: Float,
    isGlass: Boolean,
    backdrop: Backdrop,
    isKmh: Boolean = true
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = min(speed, 240f),
        animationSpec = tween(400)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isGlass) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                SpeedometerGauge(
                    speed = animatedSpeed,
                    modifier = Modifier.fillMaxSize(0.9f)
                )

            }
        } else {

            Box(contentAlignment = Alignment.Center) {
                WavySpeedometerGauge(
                    speed = animatedSpeed,
                    modifier = Modifier.fillMaxSize(0.9f)
                )

            }

        }

        // 🧭 Speed readout
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedSpeed.toInt()}",
                fontSize = 90.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isKmh) "km/h" else "mp/h",
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavySpeedometerGauge(
    speed: Float,
    modifier: Modifier = Modifier,
    maxSpeed: Float = 270f,
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    strokeWidth: Float = 32f,
    totalSweep: Float = 270f,
    cyclesPerFullCircle: Float = 16f,
    amplitudeFactor: Float = 0.56f
) {
    val progress = (speed / maxSpeed).coerceIn(0f, 1f)
    val startAngle = totalSweep / 2f

    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val radius = (diameter - strokeWidth) / 2f
        val amplitude = strokeWidth * amplitudeFactor

        val trackRadius = radius - (amplitude * 0.15f)

        rotate(degrees = startAngle, pivot = center) {
            val maxCycles = cyclesPerFullCircle * (totalSweep / 360f)
            val rawCycles = maxCycles * progress
            val adjustedCycles = ((rawCycles * 2f).roundToInt() / 2f).coerceIn(0f, maxCycles)
            val indicatorSweep = totalSweep * (if (maxCycles == 0f) 0f else adjustedCycles / maxCycles)

            val pointsPerCycle = 64
            val totalPoints = maxOf(2, (adjustedCycles * pointsPerCycle).toInt())

            val path = Path()
            for (i in 0..totalPoints) {
                val t = i.toFloat() / totalPoints
                val angleDeg = t * indicatorSweep
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val phase = t * adjustedCycles * 2.0 * Math.PI
                val offset = sin(phase).toFloat() * amplitude

                val r = radius + offset
                val x = center.x + cos(angleRad).toFloat() * r
                val y = center.y + sin(angleRad).toFloat() * r

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            //track
            if (indicatorSweep < totalSweep) {
                val eps = 0.0001f
                drawArc(
                    color = trackColor,
                    startAngle = indicatorSweep + eps,
                    sweepAngle = totalSweep - indicatorSweep - eps,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - trackRadius,
                        center.y - trackRadius
                    ),
                    size = Size(trackRadius * 2, trackRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            //wavy indicator
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidth * 1.3f, cap = StrokeCap.Round)
            )


        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SpeedometerGauge(
    speed: Float,
    modifier: Modifier = Modifier,
    maxSpeed: Float = 270f,
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    strokeWidth: Float = 32f,
    totalSweep: Float = 270f,
    amplitudeFactor: Float = 0.48f
) {
    val progress = (speed / maxSpeed).coerceIn(0f, 1f)
    val indicatorSweep = totalSweep * progress
    val startAngle = totalSweep / 2f

    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val radius = (diameter - strokeWidth) / 2f
        val amplitude = strokeWidth * amplitudeFactor

        val trackRadius = radius - (amplitude * 0.15f)

        rotate(degrees = startAngle, pivot = center) {
            if (indicatorSweep > 0f) {
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = indicatorSweep,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth * 1.3f, cap = StrokeCap.Round)
                )
            }

            if (indicatorSweep < totalSweep) {
                val eps = 0.0001f
                drawArc(
                    color = trackColor,
                    startAngle = indicatorSweep + eps,
                    sweepAngle = totalSweep - indicatorSweep - eps,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - trackRadius,
                        center.y - trackRadius
                    ),
                    size = Size(trackRadius * 2, trackRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }
}