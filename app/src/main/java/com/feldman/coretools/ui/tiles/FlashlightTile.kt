package com.feldman.coretools.ui.tiles

import android.content.Context
import android.graphics.drawable.Icon
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.FlashlightRepo
import com.feldman.coretools.R
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.showTileInfoFlow
import com.feldman.coretools.ui.components.SensorCard
import com.feldman.coretools.ui.components.adaptive.AdaptiveSlider
import com.feldman.coretools.ui.pages.RoundedLiquidButton
import com.feldman.coretools.ui.pages.ScaledShape
import com.feldman.coretools.ui.pages.settings.FlashlightSettingsPage
import com.feldman.coretools.ui.theme.AppTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FlashlightTile : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        setFinishOnTouchOutside(true)
        setContent {

            AppTheme {
                FlashlightTile(
                    onDismiss = { finish() }
                )
            }
        }
    }
}
object FlashlightController {
    private val _torchLevel = MutableStateFlow(0)
    val torchLevel: StateFlow<Int> = _torchLevel.asStateFlow()

    fun startListening(context: Context) {
        val camMgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        camMgr.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (!enabled) {
                    _torchLevel.value = 0
                }
            }

            override fun onTorchStrengthLevelChanged(cameraId: String, level: Int) {
                _torchLevel.value = level
            }
        }, null)
    }

    private fun cameraId(context: Context): String? {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    fun getMaxLevel(context: Context): Int {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cameraId(context) ?: return 1
        return cm.getCameraCharacteristics(id)
            .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
    }

    /** level: 0 = off. On API < 33, any level > 0 turns torch on (no strength control). */
    fun setIntensity(context: Context, level: Int) {
        _torchLevel.value = level

        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cameraId(context) ?: return
        try {
            if (level <= 0) cm.setTorchMode(id, false)
            else cm.turnOnTorchWithStrengthLevel(id, level)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


@OptIn(FlowPreview::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlashlightTile(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass
    val backdrop = rememberLayerBackdrop()

    val maxLevel = remember { FlashlightController.getMaxLevel(context) }
    val level by FlashlightController.torchLevel.collectAsState()

    fun applyLevel(newLevel: Int) {
        val clamped = newLevel.coerceIn(0, maxLevel)
        FlashlightController.setIntensity(context, clamped)
    }

    fun fallbackHalf(): Int =
        if (maxLevel > 1) maxOf(1, (maxLevel * 0.5f).toInt()) else 1

    val archShape = ScaledShape(MaterialShapes.Arch.toShape(), scale = 1.2f)
    val cookie4Shape = ScaledShape(MaterialShapes.Cookie4Sided.toShape(), scale = 1.4f)
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

    val percent = if (maxLevel <= 1) if (level > 0) 100 else 0
    else ((level.toFloat() / maxLevel) * 100).toInt()
    val showTileInfo by context.showTileInfoFlow().collectAsState(initial = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Flashlight",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column {
                AdaptiveSlider(
                    level = level,
                    maxLevel = maxLevel,
                    applyLevel = { applyLevel(it) },
                    valueRange = 0f..maxLevel.toFloat(),
                    visibilityThreshold = 0.01f,
                    backdrop = backdrop
                )
                if (showTileInfo){
                    Row(
                        modifier = Modifier
                            .padding(top = if (isGlass) 0.dp else 30.dp )
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            if (isGlass) 10.dp else 40.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SensorCard(
                            icon = R.drawable.ic_brightness,
                            label = "Brightness",
                            value = "$percent%",
                            shape = cookie4Shape,
                            color = secondaryContainer,
                            index = 1,
                            backdrop = backdrop,
                            cornerRadius = 50.dp
                        )
                        SensorCard(
                            icon = R.drawable.ic_layers,
                            label = "Level",
                            value = "$level/$maxLevel",
                            shape = archShape,
                            color = secondaryContainer,
                            index = 2,
                            backdrop = backdrop,
                            cornerRadius = 50.dp
                        )
                    }

                    PresetButtonRow(
                        maxLevel = maxLevel,
                        applyLevel = ::applyLevel,
                        fallbackHalf = ::fallbackHalf,
                        iconSize = 60.dp,
                        showText = false
                    )
                }

            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
            ) {
                Text("Done")
            }
        }
    )
}

class FlashlightTileService : TileService() {

    private var cameraManager: CameraManager? = null
    private var torchCallback: CameraManager.TorchCallback? = null
    private var currentTorchOn = false

    override fun onStartListening() {
        super.onStartListening()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Register callback to receive actual torch state changes
        torchCallback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                currentTorchOn = enabled
                updateTileState(enabled)
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                updateTileState(false)
            }
        }

        cameraManager?.registerTorchCallback(torchCallback!!, null)

        // Initialize tile state from current torch mode (if available)
        val id = cameraManager?.cameraIdList?.firstOrNull {
            cameraManager?.getCameraCharacteristics(it)
                ?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        val torchOn = id?.let {
            // Query system torch mode via callback (API >= 33)
            try {
                (cameraManager?.getTorchStrengthLevel(it) ?: 0) > 0
            } catch (_: Exception) {
                false
            }
        } ?: false

        updateTileState(torchOn)
    }

    override fun onClick() {
        super.onClick()
        val cm = cameraManager ?: getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return

        // Toggle actual torch state
        val newState = !currentTorchOn
        cm.setTorchMode(id, newState)
        currentTorchOn = newState
        updateTileState(newState)
    }

    override fun onStopListening() {
        super.onStopListening()
        torchCallback?.let { cameraManager?.unregisterTorchCallback(it) }
        torchCallback = null
        cameraManager = null
    }

    private fun updateTileState(isOn: Boolean) {
        qsTile?.apply {
            if (icon == null) {
                icon = Icon.createWithResource(this@FlashlightTileService, R.drawable.ic_flashlight)
            }
            label = "Flashlight"
            state = if (isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}


fun Float.isCloseTo(other: Float, epsilon: Float = 0.5f): Boolean {
    return kotlin.math.abs(this - other) < epsilon
}



@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PresetButtonRow(
    maxLevel: Int,
    applyLevel: (Int) -> Unit,
    fallbackHalf: () -> Int,
    iconSize: Dp = 72.dp,
    showText: Boolean = true
) {
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    val options = listOf("0", "33", "50", "100")
    val levels = listOf(0, maxLevel / 3, fallbackHalf(), maxLevel)

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            if (isGlass) {
                RoundedLiquidButton(
                    icon = painterResource(
                        when (index) {
                            0 -> R.drawable.ic_brightness_0
                            1 -> R.drawable.ic_brightness_1
                            2 -> R.drawable.ic_brightness_2
                            else -> R.drawable.ic_brightness_3
                        }
                    ),
                    contentDescription = label,
                    selected = selectedIndex == index,
                    onClick = {
                        applyLevel(levels[index])
                        selectedIndex = index
                        scope.launch {
                            delay(300)
                            selectedIndex = -1
                        }
                    },
                    modifier = Modifier.padding(2.dp),
                    iconSize = iconSize
                )
            } else {
                ToggleButton(
                    checked = selectedIndex == index,
                    onCheckedChange = {
                        selectedIndex = index
                        applyLevel(levels[index])
                        scope.launch {
                            delay(300)
                            selectedIndex = -1
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                ) {
                    val icon = painterResource(
                        when (index) {
                            0 -> R.drawable.ic_brightness_0
                            1 -> R.drawable.ic_brightness_1
                            2 -> R.drawable.ic_brightness_2
                            else -> R.drawable.ic_brightness_3
                        }
                    )
                    Icon(
                        painter = icon,
                        contentDescription = "brightness level",
                        modifier = Modifier.size(24.dp)
                    )
                    if(showText){
                        Spacer(Modifier.width(4.dp))
                        Text(label)
                    }
                }
            }
        }
    }
}