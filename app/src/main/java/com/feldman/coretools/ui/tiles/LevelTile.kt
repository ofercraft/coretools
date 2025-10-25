package com.feldman.coretools.ui.tiles

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feldman.coretools.LevelAngles
import com.feldman.coretools.LevelEngine
import com.feldman.coretools.R
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.ui.components.adaptive.AdaptiveButton
import com.feldman.coretools.ui.pages.CompassPageActivity
import com.feldman.coretools.ui.pages.LevelPageActivity
import com.feldman.coretools.ui.pages.drawDial
import com.feldman.coretools.ui.pages.rememberLevelEngine
import com.feldman.coretools.ui.theme.AppTheme
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("DEPRECATION")
private fun TileService.launchAndCollapse(target: Class<*>) {
    val intent = Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val pending = PendingIntent.getActivity(
        this, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    startActivityAndCollapse(pending)
}

/**
 * Quick Settings Tile for the Bubble Level (Combined mode only)
 */
class LevelTileService : TileService(), SensorEventListener {
    private var sensorManager: SensorManager? = null

    override fun onStartListening() {
        super.onStartListening()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        qsTile?.apply {
            icon = Icon.createWithResource(this@LevelTileService, R.drawable.ic_level)
            label = "Level"
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        sensorManager?.unregisterListener(this)
        sensorManager = null
    }

    override fun onSensorChanged(event: SensorEvent?) = Unit
    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) = Unit

    override fun onClick() {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
        launchAndCollapse(LevelTile::class.java)
    }
}


class LevelTile : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        setFinishOnTouchOutside(true)

        setContent {
            AppTheme {
                CombinedLevelTilePage(
                    onDismiss = { finish() },
                    modifier = Modifier,
                    reversed = false,
                    vibration = true
                )
            }
        }
    }
}

@Composable
fun CombinedLevelTilePage(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    reversed: Boolean = false,
    vibration: Boolean = true
) {
    val context = LocalContext.current
    val engine = remember { LevelEngine(context) }
    val angles by engine.anglesFlow.collectAsState(initial = LevelAngles())
    val backdrop = rememberLayerBackdrop()
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass
    val dark = isDarkTheme()

    val primaryColor = MaterialTheme.colorScheme.primary
    var lineColor = if (dark) Color.Black else Color.White
    if (isGlass) {
        lineColor = if (!dark) Color.Black else Color.White
    }

    val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java) }
    val lastVibrationTime = remember { mutableLongStateOf(0L) }

    // Simple vibration feedback for level
    if (vibration) {
        LaunchedEffect(angles.horizAbsDeg, angles.vertAbsDeg) {
            val combined = kotlin.math.sqrt(
                (angles.horizAbsDeg * angles.horizAbsDeg + angles.vertAbsDeg * angles.vertAbsDeg)
                    .coerceAtMost(900).toDouble()
            )
            val now = System.currentTimeMillis()
            val deadZone = 1.5f
            val delay = (150 + (combined / 30f) * 450)
                .toLong()
                .coerceIn(150, 600)

            if (now - lastVibrationTime.longValue < delay) return@LaunchedEffect
            if (combined < deadZone) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(25, 60))
                lastVibrationTime.longValue = now
            } else if (combined < 30f) {
                val dur = (10 + (combined / 30f) * 40).toLong()
                val amp = (40 + (combined / 30f) * 200).toInt().coerceIn(40, 255)
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(dur, amp))
                lastVibrationTime.longValue = now
            }
        }
    }
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
                Text("Level")
                val context = LocalContext.current
                IconButton(onClick = {
                    context.startActivity(Intent(context, LevelPageActivity::class.java))
                }) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
                }
            }
        },
        text = {
            Box(
                modifier = modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isGlass) {
                    val bubbleSize = 260.dp
                    val shape = CircleShape

                    Box(
                        modifier = Modifier
                            .size(bubbleSize)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                this.shape = shape
                                clip = true
                            }
                            .drawBackdrop(
                                backdrop = backdrop,
                                effects = {
                                    vibrancy()
                                    blur(6.dp.toPx())
                                    lens(
                                        refractionHeight = 24.dp.toPx(),
                                        refractionAmount = 80.dp.toPx(),
                                        depthEffect = true
                                    )
                                },
                                shape = { shape }, // ensures circular lens/refraction region
                                onDrawSurface = {
                                    drawRect(
                                        if (dark) Color(0xFF2C2C2C).copy(alpha = 0.2f)
                                        else Color(0xFFEEEEEE).copy(alpha = 0.2f)
                                    )
                                }
                            )
                            .background(Color.Transparent, shape)
                    )

                }

                Canvas(modifier = Modifier.size(300.dp)) {
                    val radius = size.minDimension / 2.3f
                    val cx = center.x
                    val cy = center.y
                    val bubbleR = radius * 0.15f
                    val maxDisp = radius - bubbleR - 8.dp.toPx()

                    if(!isGlass){
                        // Outer circle
                        drawCircle(
                            color = primaryColor,
                            radius = radius,
                            style = Fill
                        )

                        // Circle border
                        drawCircle(
                            color = lineColor,
                            radius = radius,
                            style = Stroke(width = 4f)
                        )
                    }

                    // Crosshair
                    val cross = radius * 0.25f
                    drawLine(
                        color = lineColor,
                        start = Offset(cx - cross, cy),
                        end = Offset(cx + cross, cy),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(cx, cy - cross),
                        end = Offset(cx, cy + cross),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )

                    // Bubble displacement
                    var dx = -sin(angles.dirRad) * angles.frac * maxDisp
                    var dy = -cos(angles.dirRad) * angles.frac * maxDisp
                    if (reversed) {
                        dx = -dx; dy = -dy
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

        },
        confirmButton = {
            AdaptiveButton(
                onClick = onDismiss,
                backdrop = backdrop,
                surfaceColor = MaterialTheme.colorScheme.primary
            ) {
                Text("Close", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

    }
}