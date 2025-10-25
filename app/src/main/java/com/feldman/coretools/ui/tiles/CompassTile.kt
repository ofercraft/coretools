// CompassTileService.kt
package com.feldman.coretools.ui.tiles

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.feldman.coretools.R
import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.PathMeasure
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.Surface
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.compassShapeFlow
import com.feldman.coretools.storage.compassVibrationFeedbackFlow
import com.feldman.coretools.storage.showIntercardinalsFlow
import com.feldman.coretools.storage.showWeatherRowFlow
import com.feldman.coretools.storage.trueNorthFlow
import com.feldman.coretools.ui.components.adaptive.AdaptiveButton
import com.feldman.coretools.ui.components.liquid.LiquidButton
import com.feldman.coretools.ui.pages.CompassPageActivity
import com.feldman.coretools.ui.pages.CompassShapeRegistry
import com.feldman.coretools.ui.pages.drawDial
import com.feldman.coretools.ui.pages.settings.CompassSettingsPage
import com.feldman.coretools.ui.theme.AppTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.effects.lens
import kotlin.math.abs

@Suppress("DEPRECATION")
@SuppressLint("StartActivityAndCollapseDeprecated")
private fun TileService.launchAndCollapse(target: Class<*>) {
    val intent = Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    val pending = PendingIntent.getActivity(this, /*requestCode=*/0, intent, flags)

    startActivityAndCollapse(pending)
}
class CompassTileService : TileService(), SensorEventListener {
    private var sensorManager: SensorManager? = null

    override fun onStartListening() {
        super.onStartListening()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.also { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        qsTile?.apply {
            icon = Icon.createWithResource(
                this@CompassTileService,
                R.drawable.ic_compass
            )
            state = Tile.STATE_INACTIVE
            label = "Compass"
            updateTile()
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        sensorManager?.unregisterListener(this)
        sensorManager = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (azimuth < 0) azimuth += 360f

            // ✅ Store last azimuth so CompassActivity uses latest value
            CompassCache.lastAzimuth = azimuth

            // Optional: rotate tile icon in real time (Android 14+ only)
            qsTile?.apply {
                label = "${azimuth.toInt()}°"
                updateTile()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onClick() {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
        launchAndCollapse(CompassTile::class.java)
    }
}


object CompassCache {
    @Volatile var lastAzimuth: Float? = null
}
class CompassTile : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        setFinishOnTouchOutside(true)

        setContent {
            AppTheme {
                CompassTile(onDismiss = { finish() })
            }
        }
    }
}

/**
 * Holds all compass-related sensor readings.
 */
@Stable
data class CompassState(
    val azimuth: Float = 0f,
    val calibrationAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val baroAltitude: Float = 0f,
    val gpsAltitude: Double = 0.0,
    val pressureHpa: Float = 0f,
    val lightLevel: Float = 0f,
    val proximity: Float = 0f,
    val magField: Triple<Float, Float, Float> = Triple(0f,0f,0f),

)

/**
 * Remember and manage sensor listeners for compass data.
 */
@Composable
fun rememberCompassState(): CompassState {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService<SensorManager>()!! }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val seeded = CompassCache.lastAzimuth
    var state by remember { mutableStateOf(CompassState(azimuth = seeded ?: 0f)) }

    DisposableEffect(Unit) {
        val azimuth = mutableFloatStateOf(0f)
        val baroAlt = mutableFloatStateOf(0f)
        val pressure = mutableFloatStateOf(0f)
        val light = mutableFloatStateOf(0f)
        val prox = mutableFloatStateOf(0f)
        val mag = mutableStateOf(Triple(0f, 0f, 0f))
        val gpsAlt = mutableDoubleStateOf(0.0)

        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    Log.d("Compass", "Mag accuracy=$accuracy")

                    state = state.copy(calibrationAccuracy = accuracy)
                }
            }

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rot = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rot, event.values)

                        val remapped = FloatArray(9)
                        val rotation = context.display.rotation
                        val (axisX, axisY) = when (rotation) {
                            Surface.ROTATION_0   -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                            Surface.ROTATION_90  -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                        }
                        SensorManager.remapCoordinateSystem(rot, axisX, axisY, remapped)

                        val orient = FloatArray(3)
                        SensorManager.getOrientation(remapped, orient)

                        var newDeg = Math.toDegrees(orient[0].toDouble()).toFloat()
                        if (newDeg < 0) newDeg += 360f

                        azimuth.floatValue = newDeg
                        CompassCache.lastAzimuth = newDeg
                    }

                    Sensor.TYPE_PRESSURE -> {
                        pressure.floatValue = event.values[0]
                        baroAlt.floatValue = SensorManager.getAltitude(
                            SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
                            pressure.floatValue
                        )
                    }

                    Sensor.TYPE_LIGHT -> light.floatValue = event.values[0]
                    Sensor.TYPE_PROXIMITY -> prox.floatValue = event.values[0]
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        mag.value = Triple(event.values[0], event.values[1], event.values[2])
                        // accuracy handled in onAccuracyChanged
                    }
                }

                state = CompassState(
                    azimuth = azimuth.floatValue,
                    calibrationAccuracy = state.calibrationAccuracy,
                    baroAltitude = baroAlt.floatValue,
                    gpsAltitude = gpsAlt.doubleValue,
                    pressureHpa = pressure.floatValue,
                    lightLevel = light.floatValue,
                    proximity = prox.floatValue,
                    magField = mag.value
                )
            }
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.also {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.also {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.also {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.also {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    state = state.copy(gpsAltitude = it.altitude)
                }
            }
        }
    }

    return state
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
fun CompassTile(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberCompassState()

    val context = LocalContext.current

    val compassShape by context.compassShapeFlow().collectAsState(initial = 0)
    val selectedShape = CompassShapeRegistry.byId(compassShape).shapeFactory()

    val showIntercardinals by context.showIntercardinalsFlow().collectAsState(initial = true)
    val trueNorth by context.trueNorthFlow().collectAsState(initial = false)
    val compassVibration by context.compassVibrationFeedbackFlow().collectAsState(initial = true)

    var declination by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(trueNorth) {
        if (trueNorth) {
            val loc = com.feldman.coretools.ui.pages.getCurrentLocation(context)
            if (loc != null) {
                try {
                    val geomag = GeomagneticField(
                        loc.latitude.toFloat(),
                        loc.longitude.toFloat(),
                        loc.altitude.toFloat(),
                        System.currentTimeMillis()
                    )
                    declination = geomag.declination
                } catch (_: Exception) {
                    declination = 0f
                }
            } else declination = 0f
        } else declination = 0f
    }

    val correctedAzimuth = (state.azimuth + declination).mod(360f)


    val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java) }
    val lastVibrationTime = remember { mutableLongStateOf(0L) }

    LaunchedEffect(correctedAzimuth, compassVibration) {
        if (!compassVibration) return@LaunchedEffect

        val now = System.currentTimeMillis()
        val heading = correctedAzimuth.mod(360f)
        val tolerance = 3f
        val vibrationDelay = 500L

        val isCardinal = heading in 360f - tolerance..360f ||
                heading in 0f..tolerance ||
                heading in 90f - tolerance..90f + tolerance ||
                heading in 180f - tolerance..180f + tolerance ||
                heading in 270f - tolerance..270f + tolerance

        if (isCardinal && now - lastVibrationTime.longValue > vibrationDelay) {
            val distance = listOf(
                abs(heading - 0f),
                abs(heading - 90f),
                abs(heading - 180f),
                abs(heading - 270f),
                abs(heading - 360f)
            ).minOrNull() ?: 0f

            val strength = (1f - (distance / tolerance)).coerceIn(0f, 1f)
            val duration = (20 + 40 * strength).toLong() // 20–60ms
            val amplitude = (60 + 195 * strength).toInt().coerceAtMost(255)

            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(duration, amplitude))
            lastVibrationTime.longValue = now
        }
    }

    val textColor = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val arrowShape = MaterialShapes.Triangle.toShape()
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    var showCalibration by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val backdrop = rememberLayerBackdrop()
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass
    val dark = isSystemInDarkTheme()



    AlertDialog (
        onDismissRequest = onDismiss,
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
                shape = { RoundedCornerShape(28.dp) },
                onDrawSurface = { drawRect(if(dark) Color(0xFFA8A8A8).copy(alpha = 0.3f) else Color(
                    0xFF494949
                ).copy(alpha = 0.2f)) }
            ),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Compass")
                val context = LocalContext.current
                IconButton(onClick = {
                    context.startActivity(Intent(context, CompassPageActivity::class.java))
                }) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
                }
            }
        },
        text = {
            if (showCalibration){
                CalibrationDialog(state)
            }
            else if (isGlass){
                Column(
                    modifier = modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .drawBackdrop(
                                backdrop = backdrop,
                                effects = {
                                    vibrancy()
                                    blur(4.dp.toPx())
                                    lens(
                                        refractionHeight = 24.dp.toPx(),
                                        refractionAmount = 60.dp.toPx(),
                                        depthEffect = true
                                    )
                                },
                                shape = { RoundedCornerShape(percent = 50) },
                                onDrawSurface = {
                                    drawRect(if (dark) Color.Black.copy(alpha = 0.7f) else Color.White)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val dialSize = 220.dp
                        Canvas(modifier = Modifier.size(dialSize)) {
                            val r = radius
                            val cx = center.x
                            val cy = center.y

                            rotate(-correctedAzimuth  - 90f, pivot = center) {
                                drawDial(
                                    tickColor = if (dark) Color.White else Color.Black,
                                    tickVariantColor = if (dark) Color.White else Color.Black,
                                    showIntercardinals = showIntercardinals ,
                                    r = r

                                )
                            }

                            val az = (correctedAzimuth  % 360 + 360) % 360
                            val nearNorth = az < 2f || az > 358f
                            val arrowFill = if (nearNorth) Color.Red else if (dark) Color.White else Color.Black

                            drawNorthIndicator(
                                x = cx,
                                y = cy - r + (r * 0.65f),
                                size = r * 0.3f,
                                textColor = arrowFill,
                                arrowShape = arrowShape
                            )
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = Paint().apply {
                                    isAntiAlias = true
                                    color = (if (dark) Color.White else Color.Black).toArgb()
                                    textSize = r * 0.35f
                                    textAlign = AndroidPaint.Align.CENTER
                                }
                                drawText("${correctedAzimuth .toInt()}°", cx, cy + paint.textSize / 3, paint)
                            }

                        }
                    }

                }
            }
            else {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    val dialSize = 200.dp
                    Canvas(modifier = Modifier.size(dialSize)) {
                        val r = radius
                        val cx = center.x
                        val cy = center.y

                        val padding = r * 0.15f
                        val cr = r + padding
                        val size2 = Size(cr * 2f, cr * 2f)

                        val outline = selectedShape.createOutline(size2, layoutDirection, this)
                        translate(left = cx - cr, top = cy - cr) {
                            drawOutline(outline=outline, color=primary, style=Fill)
                        }

                        rotate(-correctedAzimuth - 90f, pivot = center) {
                            drawDial(
                                tickColor = surfaceVariant,
                                tickVariantColor = surface,
                                showIntercardinals = showIntercardinals ,
                                r = r

                            )
                        }

                        val az = (correctedAzimuth % 360 + 360) % 360
                        val nearNorth = az < 2f || az > 358f
                        val arrowFill = if (nearNorth) Color.Red else surface

                        drawNorthIndicator(
                            x = cx,
                            y = cy - r + (r * 0.65f),
                            size = r * 0.3f,
                            textColor = arrowFill,
                            arrowShape = arrowShape
                        )
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = Paint().apply {
                                isAntiAlias = true
                                color = textColor.toArgb()
                                textSize = r * 0.35f
                                textAlign = AndroidPaint.Align.CENTER
                            }
                            drawText("${correctedAzimuth.toInt()}°", cx, cy + paint.textSize / 3, paint)
                        }

                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        },
        confirmButton = {
            if (showCalibration){
                AdaptiveButton(
                    onClick = {
                        showCalibration = false
                    },
                    backdrop = backdrop,
                    surfaceColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("OK", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AdaptiveButton(
                        onClick = { showCalibration = true },
                        backdrop = backdrop,
                        surfaceColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f).height(70.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_calibration),
                            contentDescription = "Calibrate",
                            modifier = Modifier.size(25.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(stringResource(R.string.calibrate), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }

                    AdaptiveButton(
                        onClick = onDismiss,
                        backdrop = backdrop,
                        surfaceColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f).height(70.dp)
                    ) {
                        Text("Close", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

        }
    )
//
//    if (showCalibration) {
//        CalibrationDialog(
//            state = state,
//            onDismiss = { showCalibration = false }
//        )
//    } else {
//        if (isGlass){
//            Box(
//                modifier = Modifier
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
//                        shape = { RoundedCornerShape(28.dp) },
//                        onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.25f)) }
//                    )
//            ) {
//
//            }
//
//        }
//        else {
//            AlertDialog (
//                onDismissRequest = onDismiss,
//                title = {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text("Compass")
//                        val context = LocalContext.current
//                        IconButton(onClick = {
//                            context.startActivity(Intent(context, CompassPageActivity::class.java))
//                        }) {
//                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
//                        }
//                    }
//                },
//                text = {
//                    Column(
//                        modifier = modifier.fillMaxWidth(),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Spacer(modifier = Modifier.height(32.dp))
//
//                        val dialSize = 200.dp
//                        Canvas(modifier = Modifier.size(dialSize)) {
//                            val r = radius
//                            val cx = center.x
//                            val cy = center.y
//
//                            val padding = r * 0.15f
//                            val cr = r + padding
//                            val size2 = Size(cr * 2f, cr * 2f)
//
//                            val outline = selectedShape.createOutline(size2, layoutDirection, this)
//                            translate(left = cx - cr, top = cy - cr) {
//                                drawOutline(outline=outline, color=primary, style=Fill)
//                            }
//
//                            rotate(-correctedAzimuth - 90f, pivot = center) {
//                                drawDial(
//                                    tickColor = surfaceVariant,
//                                    tickVariantColor = surface,
//                                    showIntercardinals = showIntercardinals ,
//                                    r = r
//
//                                )
//                            }
//
//                            val az = (correctedAzimuth % 360 + 360) % 360
//                            val nearNorth = az < 2f || az > 358f
//                            val arrowFill = if (nearNorth) Color.Red else surface
//
//                            drawNorthIndicator(
//                                x = cx,
//                                y = cy - r + (r * 0.65f),
//                                size = r * 0.3f,
//                                textColor = arrowFill,
//                                arrowShape = arrowShape
//                            )
//                            drawContext.canvas.nativeCanvas.apply {
//                                val paint = Paint().apply {
//                                    isAntiAlias = true
//                                    color = textColor.toArgb()
//                                    textSize = r * 0.35f
//                                    textAlign = AndroidPaint.Align.CENTER
//                                }
//                                drawText("${correctedAzimuth.toInt()}°", cx, cy + paint.textSize / 3, paint)
//                            }
//
//                        }
//                        Spacer(modifier = Modifier.height(32.dp))
//                    }
//                },
//                confirmButton = {
//                    Row(
//                        horizontalArrangement = Arrangement.spacedBy(12.dp),
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//
//                        Button(
//                            onClick = { showCalibration = true },
//                            modifier = Modifier.weight(1f).height(90.dp)
//                        ) {
//                            Text("Calibrate", fontSize = 18.sp)
//                        }
//
//                    }
//                }
//            )
//        }
//
//    }

}






private fun DrawScope.drawNorthIndicator(
    x: Float,
    y: Float,
    size: Float,
    textColor: Color,
    arrowShape: Shape
) {
    val outline = arrowShape.createOutline(
        size = Size(size, size),
        layoutDirection = layoutDirection,
        density = this
    )

    val half = size / 2f
    translate(left = x - half, top = y - half) {
        // 3a) fill the shape
        drawOutline(outline, color = textColor, style = Fill)
        // 3b) stroke the shape
        drawOutline(outline, color = textColor, style = Stroke(width = 4f))
    }
}

private val DrawScope.radius get() = size.minDimension / 1.7f

fun accuracyToText(accuracy: Int): String = when (accuracy) {
    SensorManager.SENSOR_STATUS_UNRELIABLE -> "Uncalibrated – move phone in a figure-8"
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low accuracy"
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium accuracy"
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High accuracy (Calibrated)"
    else -> "Unknown"
}

fun accuracyToColor(accuracy: Int): Color = when (accuracy) {
    SensorManager.SENSOR_STATUS_UNRELIABLE -> Color.Red
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> Color(0xFFFFA500)
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color.Yellow
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color(0xFF72E372)
    else -> Color.Gray
}
@Composable
fun CalibrationDialog(state: CompassState) {
    val primary = MaterialTheme.colorScheme.primary
    val size = 500f
    val anim = rememberInfiniteTransition()
    val phase by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Move your phone in a figure-8 pattern until the heading stabilizes.")
        Spacer(Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            val path = Path().apply {
                moveTo(center.x, center.y)
                cubicTo(
                    center.x - size, center.y - size,
                    center.x - size, center.y + size,
                    center.x, center.y
                )
                cubicTo(
                    center.x + size, center.y - size,
                    center.x + size, center.y + size,
                    center.x, center.y
                )
            }

            drawPath(path, Color.Gray.copy(alpha = 0.3f), style = Stroke(width = 20f))

            val measure = PathMeasure()
            measure.setPath(path.asAndroidPath(), false)
            val pos = FloatArray(2)
            measure.getPosTan(measure.length * phase, pos, null)

            drawCircle(
                color = primary,
                radius = 24f,
                center = Offset(pos[0], pos[1])
            )
        }
        Text(
            text = accuracyToText(state.calibrationAccuracy),
            color = accuracyToColor(state.calibrationAccuracy),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
