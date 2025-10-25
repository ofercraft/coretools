package com.feldman.coretools.ui.pages

import com.feldman.coretools.MainActivity.Dest
import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.feldman.coretools.MainActivity
import com.feldman.coretools.MainApp
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.ui.tiles.FlashlightController
import com.feldman.coretools.R
import com.feldman.coretools.storage.autoFlashlightOffFlow
import com.feldman.coretools.storage.defaultFlashlightLevelFlow
import com.feldman.coretools.storage.instantFlashlightFlow
import com.feldman.coretools.ui.components.SensorCard
import com.feldman.coretools.ui.components.adaptive.AdaptiveButton
import com.feldman.coretools.ui.components.adaptive.AdaptiveSlider
import com.feldman.coretools.ui.components.liquid.LiquidButton
import com.feldman.coretools.ui.theme.AppTheme
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FlashlightPageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainApp(startDestination = Dest.Flashlight)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlashlightPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    FlashlightController.startListening(context)
    val maxLevel = remember { FlashlightController.getMaxLevel(context) }
    val luminance = rememberAmbientLuminance(context)

    val autoFlashlightOff by context.autoFlashlightOffFlow().collectAsState(initial = true)
    val instantFlashlight by context.instantFlashlightFlow().collectAsState(initial = false)
    val defaultFlashlightLevel by context.defaultFlashlightLevelFlow().collectAsState(initial = 100)

    val level by FlashlightController.torchLevel.collectAsState()

    DisposableEffect(lifecycleOwner, autoFlashlightOff) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                FlashlightController.setIntensity(context, 0)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(instantFlashlight, defaultFlashlightLevel) {
        if (instantFlashlight && level == 0) {
            val brightness = (defaultFlashlightLevel / 100f * maxLevel).toInt().coerceIn(0, maxLevel)
            FlashlightController.setIntensity(context, brightness)
        }
    }


    fun fallbackHalf(): Int =
        if (maxLevel > 1) maxOf(1, (maxLevel * 0.5f).toInt()) else 1

    fun applyLevel(newLevel: Int) {
        val clamped = newLevel.coerceIn(0, maxLevel)
        FlashlightController.setIntensity(context, clamped)
    }

    val archShape = ScaledShape(MaterialShapes.Arch.toShape(), scale = 1.2f)
    val squareShape = ScaledShape(MaterialShapes.Square.toShape(), scale = 1.2f)
    val cookie4Shape = ScaledShape(MaterialShapes.Cookie4Sided.toShape(), scale = 1.4f)

    var showBlinkDialog by remember { mutableStateOf(false) }
    var showMorseDialog by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }


    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(vertical = if (isLandscape) 0.dp else 32.dp, horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        if (!isLandscape){
            Spacer(Modifier.height(100.dp))
        }

        val backdrop = rememberLayerBackdrop()
        val context = LocalContext.current
        val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
        val isGlass = appStyle == AppStyle.Glass

        AdaptiveSlider(
            level = level,
            maxLevel = maxLevel,
            applyLevel = { applyLevel(it) },
            valueRange = 0f..maxLevel.toFloat(),
            visibilityThreshold = 0.01f,
            backdrop = backdrop,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        val percent = if (maxLevel <= 1) if (level > 0) 100 else 0
        else ((level.toFloat() / maxLevel) * 100).toInt()

        Spacer(Modifier.height(32.dp))
        if (!isLandscape){
            Row(
                modifier = Modifier
                    .fillMaxWidth(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isGlass) 10.dp else 30.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SensorCard(
                    icon = R.drawable.ic_brightness,
                    label = "Brightness",
                    value = "$percent%",
                    shape = cookie4Shape,
                    color = secondaryContainer,
                    index = 1,
                    backdrop = backdrop,
                    cornerRadius = 50.dp,
                    modifier = Modifier
                        .then(
                            Modifier.padding(0.dp)
                        )
                )
                SensorCard(
                    icon = R.drawable.ic_layers,
                    label = "Level",
                    value = "$level/$maxLevel",
                    shape = archShape,
                    color = secondaryContainer,
                    index = 2,
                    backdrop = backdrop,
                    cornerRadius = 50.dp,
                    modifier = Modifier
                        .then(
                            Modifier.padding(0.dp)
                        )
                )

                SensorCard(
                    icon = R.drawable.ic_light,
                    label = "Luminance",
                    value = "${luminance.toInt()} lx",
                    shape = squareShape,
                    color = secondaryContainer,
                    index = 3,
                    backdrop = backdrop,
                    cornerRadius = 50.dp,
                    modifier = Modifier
                        .then(
                            Modifier.padding(0.dp)
                        )
                )
            }

            Spacer(Modifier.height(32.dp))
        }

        if (!isLandscape) {
            PresetButtonRow(maxLevel = maxLevel, applyLevel = ::applyLevel, fallbackHalf = ::fallbackHalf)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AdaptiveButton(
                    onClick = { showBlinkDialog = true },
                    backdrop = backdrop,
                    tint = MaterialTheme.colorScheme.primary
                ) {
                    Text("Blink", color = MaterialTheme.colorScheme.onPrimary)
                }

                AdaptiveButton(
                    onClick = { showMorseDialog = true },
                    backdrop = backdrop,
                    tint = MaterialTheme.colorScheme.primary
                ) {
                    Text("Morse", color = MaterialTheme.colorScheme.onPrimary)
                }

                AdaptiveButton(
                    onClick = { showSosDialog = true },
                    backdrop = backdrop,
                    tint = MaterialTheme.colorScheme.primary
                ) {
                    Text("SOS", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        else {
            var selectedIndex by remember { mutableIntStateOf(-1) }
            val scope = rememberCoroutineScope()
            val levels = listOf(0, maxLevel / 3, fallbackHalf(), maxLevel)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 48.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        listOf(0 to "0%", 1 to "33%").forEach { (index, label) ->
                            Button(
                                onClick = {
                                    selectedIndex = index
                                    applyLevel(levels[index])
                                    scope.launch {
                                        delay(300)
                                        selectedIndex = -1
                                    }
                                },
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedIndex == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (selectedIndex == index)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(if (selectedIndex == index) 50 else 36)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (index) {
                                            0 -> R.drawable.ic_brightness_0
                                            1 -> R.drawable.ic_brightness_1
                                            2 -> R.drawable.ic_brightness_2
                                            else -> R.drawable.ic_brightness_3
                                        }
                                    ),
                                    contentDescription = label,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(label, fontSize = 20.sp)
                            }

                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        listOf(2 to "50%", 3 to "100%").forEach { (index, label) ->
                            Button(
                                onClick = {
                                    selectedIndex = index
                                    applyLevel(levels[index])
                                    scope.launch {
                                        delay(300)
                                        selectedIndex = -1
                                    }
                                },
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedIndex == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (selectedIndex == index)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(if (selectedIndex == index) 50 else 36)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (index) {
                                            0 -> R.drawable.ic_brightness_0
                                            1 -> R.drawable.ic_brightness_1
                                            2 -> R.drawable.ic_brightness_2
                                            else -> R.drawable.ic_brightness_3
                                        }
                                    ),
                                    contentDescription = label,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(label, fontSize = 20.sp)
                            }

                        }
                    }
                }

                // --- RIGHT SECTION: Blink / Morse / SOS buttons ---
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(start = 64.dp)
                        .weight(1f, fill = false)
                ) {
                    AdaptiveButton(
                        onClick = { showBlinkDialog = true },
                        backdrop = backdrop,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(120.dp).height(60.dp)
                    ) { Text("Blink", color = MaterialTheme.colorScheme.onPrimary) }

                    AdaptiveButton(
                        onClick = { showMorseDialog = true },
                        backdrop = backdrop,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(120.dp).height(60.dp)
                    ) { Text("Morse", color = MaterialTheme.colorScheme.onPrimary) }

                    AdaptiveButton(
                        onClick = { showSosDialog = true },
                        backdrop = backdrop,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(120.dp).height(60.dp)
                    ) { Text("SOS", color = MaterialTheme.colorScheme.onPrimary) }
                }
            }
        }





        if (showBlinkDialog) {
            BlinkFlashDialog(
                onDismiss = { showBlinkDialog = false },
                maxLevel = maxLevel,
                context = context
            )
        }
        if (showMorseDialog) {
            MorseFlashDialog(
                onDismiss = { showMorseDialog = false },
                maxLevel = maxLevel,
                context = context
            )
        }
        if (showSosDialog) {
            SOSFlashDialog(
                onDismiss = { showSosDialog = false },
                maxLevel = maxLevel,
                context = context
            )
        }
    }
}
@Composable
fun BlinkFlashDialog(
    context: Context,
    maxLevel: Int,
    onDismiss: () -> Unit
) {
    var interval by remember { mutableFloatStateOf(300f) }

    val defaultFlashlightLevel by context.defaultFlashlightLevelFlow().collectAsState(initial = -1)
    var brightnessPercent by remember { mutableFloatStateOf(100f) }

    LaunchedEffect(defaultFlashlightLevel) {
        if (defaultFlashlightLevel != -1) {
            brightnessPercent = defaultFlashlightLevel.toFloat()
        }
    }

    var isBlinking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            stopBlinking(context)
            onDismiss()
        },
        title = { Text("Blink Settings") },
        text = {
            Column {
                Text("Interval: ${interval.toInt()} ms")
                Slider(
                    value = interval,
                    onValueChange = { interval = it },
                    valueRange = 100f..1000f
                )

                // Brightness slider
                Text("Brightness: ${brightnessPercent.toInt()}%")
                Slider(
                    value = brightnessPercent,
                    onValueChange = { brightnessPercent = it },
                    valueRange = 1f..100f
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            if (isBlinking) {
                                stopBlinking(context)
                                isBlinking = false
                            } else {
                                startBlinking(
                                    context,
                                    intervalState = derivedStateOf { interval },
                                    brightnessState = derivedStateOf {
                                        (brightnessPercent / 100f * maxLevel).coerceIn(0f, maxLevel.toFloat())
                                    }
                                )
                                isBlinking = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        Text(
                            if (isBlinking) "Stop" else "Start",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                stopBlinking(context)
                onDismiss()
            }) {
                Text("Close")
            }
        }
    )
}
@Composable
fun MorseFlashDialog(
    context: Context,
    maxLevel: Int,
    onDismiss: () -> Unit
) {
    var message by remember { mutableStateOf("HELLO") }
    var wpm by remember { mutableFloatStateOf(15f) }

    val defaultFlashlightLevel by context.defaultFlashlightLevelFlow().collectAsState(initial = -1)
    var brightnessPercent by remember { mutableFloatStateOf(100f) }

    LaunchedEffect(defaultFlashlightLevel) {
        if (defaultFlashlightLevel != -1) {
            brightnessPercent = defaultFlashlightLevel.toFloat()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            stopBlinking(context)
            onDismiss()
        },
        title = { Text("Morse Code Flashlight") },
        text = {
            Column {
                Text("Message:")
                androidx.compose.material3.OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Enter text") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Speed: ${wpm.toInt()} WPM")
                Slider(value = wpm, onValueChange = { wpm = it }, valueRange = 5f..30f)

                Text("Brightness: ${brightnessPercent.toInt()}%")
                Slider(value = brightnessPercent, onValueChange = { brightnessPercent = it }, valueRange = 1f..100f)

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isPlaying) {
                            stopBlinking(context)
                            isPlaying = false
                        } else {
                            isPlaying = true
                            startMorse(context, message, wpm, brightnessPercent, maxLevel)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Text(if (isPlaying) "Stop" else "Play")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                stopBlinking(context)
                onDismiss()
            }) { Text("Close") }
        }
    )
}
@Composable
fun SOSFlashDialog(
    context: Context,
    maxLevel: Int,
    onDismiss: () -> Unit
) {
    var wpm by remember { mutableFloatStateOf(20f) }

    val defaultFlashlightLevel by context.defaultFlashlightLevelFlow().collectAsState(initial = -1)
    var brightnessPercent by remember { mutableFloatStateOf(100f) }

    LaunchedEffect(defaultFlashlightLevel) {
        if (defaultFlashlightLevel != -1) {
            brightnessPercent = defaultFlashlightLevel.toFloat()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            stopBlinking(context)
            onDismiss()
        },
        title = { Text("SOS Flashlight") },
        text = {
            Column {
                Text("Speed: ${wpm.toInt()} WPM")
                Slider(value = wpm, onValueChange = { wpm = it }, valueRange = 5f..40f)

                Text("Brightness: ${brightnessPercent.toInt()}%")
                Slider(value = brightnessPercent, onValueChange = { brightnessPercent = it }, valueRange = 1f..100f)

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isPlaying) {
                            stopBlinking(context)
                            isPlaying = false
                        } else {
                            isPlaying = true
                            startMorse(context, "SOS", wpm, brightnessPercent, maxLevel)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Text(if (isPlaying) "Stop" else "Send SOS")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                stopBlinking(context)
                onDismiss()
            }) { Text("Close") }
        }
    )
}

private val MORSE_CODE_MAP = mapOf(
    // 🇺🇸 English / Latin A–Z
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
    'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
    'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
    'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..",

    // 🇩🇪 German (Umlauts)
    'Ä' to ".-.-", 'Ö' to "---.", 'Ü' to "..--", 'ẞ' to "...--..",

    // 🇫🇷 French (accents)
    'À' to ".--.-", 'Â' to ".-", 'Æ' to ".-.-", 'Ç' to "-.-..", 'É' to "..-..", 'È' to ".-..-",
    'Ê' to "-..-.", 'Ë' to "..-..", 'Ô' to "---.", 'Œ' to "---.", 'Ù' to "..--", 'Û' to "..--",
    'Ü' to "..--", 'Ÿ' to "-.--..",

    // 🇷🇺 Russian / Cyrillic
    'А' to ".-", 'Б' to "-...", 'В' to ".--", 'Г' to "--.", 'Д' to "-..", 'Е' to ".",
    'Ж' to "...-", 'З' to "--..", 'И' to "..", 'Й' to ".---", 'К' to "-.-", 'Л' to ".-..",
    'М' to "--", 'Н' to "-.", 'О' to "---", 'П' to ".--.", 'Р' to ".-.", 'С' to "...",
    'Т' to "-", 'У' to "..-", 'Ф' to "..-.", 'Х' to "....", 'Ц' to "-.-.", 'Ч' to "---.",
    'Ш' to "----", 'Щ' to "--.-", 'Ъ' to "--.--", 'Ы' to "-.--", 'Ь' to "-..-", 'Э' to "..-..",
    'Ю' to "..--", 'Я' to ".-.-",

    // 🇮🇱 Hebrew (based on ITU-Wabun extensions)
    'א' to ".-", 'ב' to "-...", 'ג' to "--.", 'ד' to "-..", 'ה' to "....", 'ו' to ".--",
    'ז' to "--..", 'ח' to "----", 'ט' to "-", 'י' to "..", 'כ' to "-.-", 'ל' to ".-..",
    'מ' to "--", 'נ' to "-.", 'ס' to "...", 'ע' to "---", 'פ' to ".--.", 'צ' to "-.-.",
    'ק' to "--.-", 'ר' to ".-.", 'ש' to "----", 'ת' to "-",

    // Numbers
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
    '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
)

fun startMorse(
    context: Context,
    message: String,
    wpm: Float,
    brightnessPercent: Float,
    maxLevel: Int
) {
    blinkJob?.cancel()
    blinkJob = CoroutineScope(Dispatchers.Default).launch {
        val dot = (1200 / wpm).toLong()
        val dash = dot * 3
        val gap = dot
        val letterGap = dot * 3
        val wordGap = dot * 7
        val brightness = (brightnessPercent / 100f * maxLevel).toInt().coerceAtLeast(1)

        for (char in message.uppercase()) {
            if (!isActive) break

            if (char == ' ') {
                delay(wordGap)
                continue
            }

            val code = MORSE_CODE_MAP[char]
            if (code == null) {
                delay(letterGap)
                continue
            }

            for (symbol in code) {
                FlashlightController.setIntensity(context, brightness)
                delay(if (symbol == '.') dot else dash)
                FlashlightController.setIntensity(context, 0)
                delay(gap)
            }

            delay(letterGap)
        }

        FlashlightController.setIntensity(context, 0)
    }
}


private var blinkJob: Job? = null

fun startBlinking(
    context: Context,
    intervalState: State<Float>,
    brightnessState: State<Float>
) {
    blinkJob?.cancel()
    blinkJob = CoroutineScope(Dispatchers.Default).launch {
        var isOn = false
        while (isActive) {
            val interval = intervalState.value.toInt()
            val brightness = brightnessState.value.toInt()

            FlashlightController.setIntensity(context, if (isOn) brightness else 0)
            isOn = !isOn
            delay(interval.toLong())
        }
    }
}

fun stopBlinking(context: Context? = null) {
    blinkJob?.cancel()
    blinkJob = null
    context?.let { FlashlightController.setIntensity(it, 0) }
}


@Composable
fun RoundedLiquidButton(
    icon: Painter,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 72.dp
) {
    val backdrop = rememberLayerBackdrop()

    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier.size(iconSize),
        isInteractive = true,
        tint = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(32.dp)
        )
    }

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
                    modifier = Modifier.padding(6.dp),
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

@Composable
fun rememberAmbientLuminance(context: Context): Float {
    var luminance by remember { mutableFloatStateOf(0f) }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    DisposableEffect(lightSensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                luminance = event.values[0]
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return luminance
}
