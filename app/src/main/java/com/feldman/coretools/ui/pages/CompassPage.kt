package com.feldman.coretools.ui.pages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Paint
import android.hardware.GeomagneticField
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.withSave
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.setCompassShape
import com.feldman.coretools.storage.setShowIntercardinals
import com.feldman.coretools.storage.setShowWeatherRow
import com.feldman.coretools.R
import com.feldman.coretools.ui.tiles.CalibrationDialog
import com.feldman.coretools.ui.tiles.CompassState
import com.feldman.coretools.ui.tiles.rememberCompassState
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import android.graphics.Paint as AndroidPaint
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.feldman.coretools.BottomSpacer
import com.feldman.coretools.Dest
import com.feldman.coretools.MainApp
import com.feldman.coretools.storage.compassShapeFlow
import com.feldman.coretools.storage.compassVibrationFeedbackFlow
import com.feldman.coretools.storage.showIntercardinalsFlow
import com.feldman.coretools.storage.showWeatherRowFlow
import com.feldman.coretools.storage.trueNorthFlow
import com.feldman.coretools.ui.components.HourlyForecast
import com.feldman.coretools.ui.components.HourlyForecastCard
import com.feldman.coretools.ui.components.SensorCard
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.effects.lens
import kotlin.math.abs


data class CompassShapeOption(
    val id: Int,
    val label: String,
    val shapeFactory: @Composable () -> Shape
)
object CompassShapeRegistry {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val shapes = listOf(
        CompassShapeOption(
            id = 0,
            label = "Classic",
            shapeFactory = { ScaledShape(MaterialShapes.Cookie12Sided.toShape(), scale = 0.95f) }
        ),
        CompassShapeOption(
            id = 1,
            label = "Circle",
            shapeFactory = { ScaledShape(MaterialShapes.Circle.toShape(), scale = 0.9f) }
        ),
        CompassShapeOption(
            id = 2,
            label = "Cookie",
            shapeFactory = { MaterialShapes.Cookie9Sided.toShape() }
        ),
        CompassShapeOption(
            id = 3,
            label = "Sunny",
            shapeFactory = { ScaledShape(MaterialShapes.Sunny.toShape(), scale = 1f) }
        ),
        CompassShapeOption(
            id = 4,
            label = "Pixel Circle",
            shapeFactory = { ScaledShape(MaterialShapes.PixelCircle.toShape(), scale = 0.9f) }
        )
    )

    fun byId(id: Int): CompassShapeOption =
        shapes.firstOrNull { it.id == id } ?: shapes.first()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CompassShapePicker(
    currentShapeId: Int,
    onShapeChange: (Int) -> Unit,
    isGlass: Boolean,
    backdrop: Backdrop
) {
    val shapes = CompassShapeRegistry.shapes
    val selectedIndex = shapes.indexOfFirst { it.id == currentShapeId }

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            itemsIndexed(shapes) { index, option ->
                val isSelected = currentShapeId == option.id

                val scaleXAnim = remember { Animatable(1f) }
                val scaleYAnim = remember { Animatable(1f) }
                val rotationAnim = remember { Animatable(0f) }
                var pivot by remember { mutableStateOf(TransformOrigin.Center) }

                LaunchedEffect(selectedIndex) {
                    if (selectedIndex == -1) return@LaunchedEffect

                    when {
                        isSelected -> {
                            val hasLeft = selectedIndex > 0
                            val hasRight = selectedIndex < shapes.lastIndex

                            pivot = when {
                                hasLeft && !hasRight -> TransformOrigin(1f, 0.5f)
                                hasRight && !hasLeft -> TransformOrigin(0f, 0.5f)
                                else -> TransformOrigin.Center
                            }

                            // widen a bit
                            scaleXAnim.snapTo(1f)
                            scaleXAnim.animateTo(
                                1.1f,
                                animationSpec = tween(120, easing = LinearOutSlowInEasing)
                            )
                            scaleXAnim.animateTo(
                                1f,
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            )

                            // spin
                            rotationAnim.snapTo(0f)
                            rotationAnim.animateTo(
                                360f,
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                            rotationAnim.snapTo(0f)
                        }
                        index == selectedIndex - 1 || index == selectedIndex + 1 -> {
                            scaleXAnim.animateTo(0.95f, animationSpec = tween(120))
                            scaleXAnim.animateTo(1f, animationSpec = tween(180))
                        }
                        else -> {
                            scaleXAnim.animateTo(1f, animationSpec = tween(150))
                        }
                    }
                    scaleYAnim.animateTo(1f, animationSpec = tween(150))
                }
                var containerColor = MaterialTheme.colorScheme.surfaceContainer
                if (isGlass){
                    containerColor = Color.Transparent
                }

                val dark = isDarkTheme()
                val accentColor =
                    if (!dark) Color(0xFF0088FF)
                    else Color(0xFF0091FF)
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface

                Card(
                    onClick = { onShapeChange(option.id) },
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            scaleX = scaleXAnim.value
                            scaleY = scaleYAnim.value
                            transformOrigin = when {
                                isSelected -> pivot
                                index == selectedIndex - 1 -> TransformOrigin(0f, 0.5f)
                                index == selectedIndex + 1 -> TransformOrigin(1f, 0.5f)
                                else -> TransformOrigin.Center
                            }
                        }
                        .then(
                            if (isSelected && !isGlass) {
                                Modifier.border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialShapes.Square.toShape()
                                )
                            } else Modifier
                        )
                        .then(
                            if (isGlass) {
                                Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    effects = {
                                        vibrancy()
                                        blur(12.dp.toPx())
                                        lens(
                                            refractionHeight = 40.dp.toPx(),
                                            refractionAmount = 120.dp.toPx(),
                                            depthEffect = true
                                        )
                                    },
                                    shape = { RoundedCornerShape(32.dp) },
                                    onDrawSurface = {
                                        if (isSelected){
                                            drawRect(
                                                if (dark)
                                                    Color(0xFFBDBDBD).copy(alpha = 0.2f)
                                                else
                                                    Color(0xFF313131).copy(alpha = 0.2f)
                                            )
                                        }
                                        else{
                                            drawRect(
                                                if (dark)
                                                    Color(0xFF313131).copy(alpha = 0.2f)
                                                else
                                                    Color(0xFFBDBDBD).copy(alpha = 0.2f)
                                            )
                                        }
                                    }
                                )
                            } else {
                                Modifier.background(
                                    color = if (dark)
                                        Color(0xFF1E1E1E)
                                    else
                                        Color.White.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(32.dp)
                                )
                            }
                        ),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = containerColor
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer { rotationZ = rotationAnim.value }
                                .background(
                                    color = if (isGlass && isSelected) accentColor else if (isGlass) onSurfaceColor else MaterialTheme.colorScheme.primary,
                                    shape = option.shapeFactory()
                                )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            option.label,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isGlass && isSelected) accentColor else onSurfaceColor
                        )
                    }
                }

            }
        }
    }

}

class CompassPageActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainApp(startDestination = Dest.Compass)

        }
    }
}







class ScaledShape(
    private val base: Shape,
    private val scale: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // Scale the size, but keep it centered
        val scaledSize = Size(size.width * scale, size.height * scale)
        val outline = base.createOutline(scaledSize, layoutDirection, density)

        // Translate back to center within the original bounds
        return when (outline) {
            is Outline.Rectangle -> {
                val left = (size.width - scaledSize.width) / 2f
                val top = (size.height - scaledSize.height) / 2f
                Outline.Rectangle(outline.rect.translate(left, top))
            }
            is Outline.Rounded -> {
                val left = (size.width - scaledSize.width) / 2f
                val top = (size.height - scaledSize.height) / 2f
                Outline.Rounded(
                    outline.roundRect.copy(
                        left = outline.roundRect.left + left,
                        top = outline.roundRect.top + top
                    )
                )
            }
            is Outline.Generic -> {
                val dx = (size.width - scaledSize.width) / 2f
                val dy = (size.height - scaledSize.height) / 2f
                Outline.Generic(outline.path.apply {
                    translate(Offset(dx, dy))
                })
            }
        }
    }
}
suspend fun getCurrentLocation(context: Context): Location? {
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) return null

    val fused = LocationServices.getFusedLocationProviderClient(context)
    return suspendCancellableCoroutine { cont ->
        try {
            fused.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } catch (_: SecurityException) {
            cont.resume(null)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MaterialSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            AnimatedContent(
                targetState = checked,
                transitionSpec = {
                    (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith fadeOut()
                },
                label = "SwitchIconAnim"
            ) { state ->
                Icon(
                    imageVector = if (state) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        },
        colors = SwitchDefaults.colors(
            checkedIconColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier
    )
}

@Composable
fun LocationCard(
    name: String,
    index: Int,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    ) {
    var visible by remember(name) { mutableStateOf(false) }
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass
    val dark = isDarkTheme()

    LaunchedEffect(name) {
        // restart the animation when the name changes
        visible = false
        delay(index * 120L) // stagger based on index
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LocationCardScale"
    )
    if (isGlass){
        Box(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    // keep composing while hidden so the animation can run
                    alpha = if (visible) 1f else 0f
                    // avoid any square flash before shape is applied
                    this.shape = shape
                    clip = true
                    transformOrigin = TransformOrigin.Center
                    scaleX = scale
                    scaleY = scale
                }
                .background(MaterialTheme.colorScheme.surfaceVariant)

        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_location),
                    contentDescription = "Location",
                    tint = if (dark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (dark) Color.White else Color.Black
                )
            }
        }
    }
    else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = if (visible) 1f else 0f
                    this.shape = shape
                    clip = true
                    transformOrigin = TransformOrigin.Center
                    scaleX = scale
                    scaleY = scale
                },
            shape = shape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_location),
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}




@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class
)
@Composable
fun CompassPage() {
    val state = rememberCompassState()
    val context = LocalContext.current
    val trueNorth by context.trueNorthFlow().collectAsState(initial = false)

    val correctedAzimuth by produceState(initialValue = 0f, state.azimuth, trueNorth) {
        if (trueNorth) {
            val loc = getCurrentLocation(context)
            if (loc != null) {
                try {
                    val geomagnetic = GeomagneticField(
                        loc.latitude.toFloat(),
                        loc.longitude.toFloat(),
                        loc.altitude.toFloat(),
                        System.currentTimeMillis()
                    )
                    val declination = geomagnetic.declination
                    value = (state.azimuth + declination).mod(360f)
                } catch (e: Exception) {
                    e.printStackTrace()
                    value = state.azimuth
                }
            } else value = state.azimuth
        } else {
            value = state.azimuth
        }
    }

    val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java) }
    val lastVibrationTime = remember { mutableLongStateOf(0L) }
    val compassVibration by context.compassVibrationFeedbackFlow().collectAsState(initial = true)

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


    val backdrop = rememberLayerBackdrop()

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)

    var timeStr by remember { mutableStateOf("--:--") }
    var locationName by remember { mutableStateOf<String?>(null) }
    var forecastJson by remember { mutableStateOf(JSONObject()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date())
            delay(60_000)
        }
    }

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status == PermissionStatus.Granted) {
            val loc = getCurrentLocation(context)
            if (loc != null) {
                val lat = loc.latitude
                val lon = loc.longitude

                coroutineScope {
                    val forecastDeferred = async(Dispatchers.IO) {
                        try {
                            val url = buildString {
                                append("https://api.open-meteo.com/v1/forecast?")
                                append("latitude=$lat&longitude=$lon")
                                append("&current=temperature_2m,relative_humidity_2m,wind_speed_10m,apparent_temperature,precipitation,rain,cloud_cover")
                                append("&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,apparent_temperature,precipitation,rain,cloud_cover")
                                append("&daily=temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min")
                                append("&timezone=auto")
                            }

                            println("Fetching weather from: $url")

                            val resp = URL(url)
                                .openConnection()
                                .getInputStream()
                                .bufferedReader()
                                .use { it.readText() }

                            JSONObject(resp)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            JSONObject()
                        }
                    }

                    val geoDeferred = async(Dispatchers.IO) {
                        try {
                            val geoUrl =
                                "https://nominatim.openstreetmap.org/reverse?" +
                                        "lat=$lat&lon=$lon&format=json&accept-language=en"

                            val conn = URL(geoUrl).openConnection() as HttpURLConnection
                            conn.setRequestProperty(
                                "User-Agent",
                                "com.feldman.coretools/1.0 (feldman.ofer11@gmail.com)"
                            )

                            val resp = conn.inputStream.bufferedReader().use { it.readText() }
                            JSONObject(resp)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            JSONObject()
                        }
                    }

                    try {
                        println(1)
                        forecastJson = forecastDeferred.await()
                        println("2 $forecastJson")
                        val geoJson = geoDeferred.await()

                        val address = geoJson.optJSONObject("address")
                        locationName = address?.optString("town", null)
                            ?: address?.optString("city", "Unknown")

                    } catch (e: Exception) {
                        println("1.5")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val scrollState = rememberScrollState()
    val screenHeight = LocalResources.current.displayMetrics.heightPixels / LocalResources.current.displayMetrics.density
    val expandTrigger = screenHeight * 0.2f

    var mainHeightPx by remember { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()


    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {

            val halfway = expandTrigger
            val target = if (scrollState.value < halfway / 2f) 0 else mainHeightPx.toInt()

            scope.launch {
                scrollState.scrollTo(target)
            }
        }
    }


    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val pagerConnection = PagerDefaults.pageNestedScrollConnection(
        state = pagerState,
        orientation = Orientation.Vertical
    )
    val customPagerConnection = object : NestedScrollConnection by pagerConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Optional custom behavior
            return Offset(available.x, available.y * 0.5f)
        }
    }
    VerticalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(
                PagerDefaults.pageNestedScrollConnection(
                    state = pagerState,
                    orientation = Orientation.Vertical
                )
            ),
    ) { page ->
        when (page) {
            0 -> {
                // 🧭 Compass content page
                CompassContentMain(
                    state = state.copy(azimuth = correctedAzimuth),
                    backdrop = backdrop,
                    appStyle = appStyle,
                    forecastJson = forecastJson,
                    timeStr = timeStr,
                    locationName = locationName,
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            mainHeightPx = coords.size.height.toFloat()
                        }
                )
            }
            1 -> {
                // 🌦️ Weather page
                WeatherAndLocationSection(
                    appStyle = appStyle,
                    forecastJson = forecastJson,
                    timeStr = timeStr,
                    backdrop = backdrop
                )
            }
        }
    }
}
//@Composable
//fun scaledPadding(
//    base: Dp = 32.dp,
//    referencePhoneWidth: Float = 360f,
//    min: Dp = 32.dp,
//    max: Dp = 2000.dp   // prevent huge values
//): Dp {
//    val config = LocalConfiguration.current
//    val scaleFactor = config.screenWidthDp / referencePhoneWidth
//
//    val scaled = base * scaleFactor
//    return scaled.coerceIn(min, max)
//}
//@Composable
//fun scaledLandscapePaddingDp(
//    base: Dp,
//    referencePhoneWidth: Float = 800f,
//    min: Dp = base,
//    max: Dp = base * 10
//): Dp {
//    val config = LocalConfiguration.current
//    val scaleFactor = config.screenWidthDp / referencePhoneWidth
//    val scaled = base * scaleFactor
//    return scaled.coerceIn(min, max)
//}
//
//@Composable
//fun scaledSp(
//    base: TextUnit,
//    min: TextUnit = base,
//    max: TextUnit = base * 10,
//    referencePortraitWidth: Float = 360f,
//    referenceLandscapeWidth: Float = 800f
//): TextUnit {
//    val config = LocalConfiguration.current
//    val orientation = config.orientation
//
//    // Select reference width based on orientation
//    val referenceWidth = when (orientation) {
//        Configuration.ORIENTATION_LANDSCAPE -> referenceLandscapeWidth
//        else -> referencePortraitWidth
//    }
//
//    // Compute scale factor based on screenWidth
//    val scaleFactor = config.screenWidthDp / referenceWidth
//
//    // Scale raw value
//    val scaledValue = base.value * scaleFactor
//
//    // Clamp safely using floats
//    val clamped = scaledValue.coerceIn(min.value, max.value)
//
//    return clamped.sp
//}
//@Composable
//fun scaledDp(
//    base: Dp,
//    min: Dp = base,
//    max: Dp = base * 10,
//    referencePortraitWidth: Float = 360f,
//    referenceLandscapeWidth: Float = 800f
//): Dp {
//    val config = LocalConfiguration.current
//    val orientation = config.orientation
//
//    val referenceWidth = when (orientation) {
//        Configuration.ORIENTATION_LANDSCAPE -> referenceLandscapeWidth
//        else -> referencePortraitWidth
//    }
//
//    val scaleFactor = config.screenWidthDp / referenceWidth
//
//    val scaled = base * scaleFactor
//    return scaled.coerceIn(min, max)
//}

@Composable
inline fun <reified T> scaled(
    base: T,
    min: T? = null,
    max: T? = null,
    referencePortraitWidth: Float = 360f,
    referenceLandscapeWidth: Float = 800f
): T {
    val config = LocalConfiguration.current
    val orientation = config.orientation

    val referenceWidth = when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> referenceLandscapeWidth
        else -> referencePortraitWidth
    }

    val scaleFactor = config.screenWidthDp / referenceWidth

    // ----- DP -----
    if (base is Dp) {
        val minDp = (min as? Dp) ?: base
        val maxDp = (max as? Dp) ?: (base * 10)
        val scaledDp = base * scaleFactor
        return scaledDp.coerceIn(minDp, maxDp) as T
    }

    // ----- SP -----
    if (base is TextUnit && base.type == TextUnitType.Sp) {
        val minSp = (min as? TextUnit) ?: base
        val maxSp = (max as? TextUnit) ?: (base * 10)

        val scaledValue = base.value * scaleFactor
        val clamped = scaledValue.coerceIn(minSp.value, maxSp.value)

        return clamped.sp as T
    }

    throw IllegalArgumentException("scaled() only supports Dp or Sp")
}
@Composable
inline fun <reified T> scaledWidth(
    base: T,
    min: T? = null,
    max: T? = null,
    referencePortraitWidth: Float = 360f,
    referenceLandscapeWidth: Float = 800f
): T {
    val config = LocalConfiguration.current
    val orientation = config.orientation

    val reference = when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> referenceLandscapeWidth
        else -> referencePortraitWidth
    }

    val scaleFactor = config.screenWidthDp / reference

    // ----- DP -----
    if (base is Dp) {
        val minDp = (min as? Dp) ?: base
        val maxDp = (max as? Dp) ?: (base * 10)
        val scaled = base * scaleFactor
        return scaled.coerceIn(minDp, maxDp) as T
    }

    // ----- SP -----
    if (base is TextUnit && base.type == TextUnitType.Sp) {
        val minSp = (min as? TextUnit) ?: base
        val maxSp = (max as? TextUnit) ?: (base * 10)

        val scaled = base.value * scaleFactor
        val clamped = scaled.coerceIn(minSp.value, maxSp.value)
        return clamped.sp as T
    }

    throw IllegalArgumentException("scaledWidth supports Dp or Sp only")
}
@Composable
inline fun <reified T> scaledHeight(
    base: T,
    min: T? = null,
    max: T? = null,
    referencePortraitHeight: Float = 800f,     // typical phone portrait height
    referenceLandscapeHeight: Float = 360f     // typical phone landscape height
): T {
    val config = LocalConfiguration.current
    val orientation = config.orientation

    val reference = when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> referenceLandscapeHeight
        else -> referencePortraitHeight
    }

    val scaleFactor = config.screenHeightDp / reference

    // ----- DP -----
    if (base is Dp) {
        val minDp = (min as? Dp) ?: base
        val maxDp = (max as? Dp) ?: (base * 10)
        val scaled = base * scaleFactor
        return scaled.coerceIn(minDp, maxDp) as T
    }

    // ----- SP -----
    if (base is TextUnit && base.type == TextUnitType.Sp) {
        val minSp = (min as? TextUnit) ?: base
        val maxSp = (max as? TextUnit) ?: (base * 10)

        val scaled = base.value * scaleFactor
        val clamped = scaled.coerceIn(minSp.value, maxSp.value)
        return clamped.sp as T
    }

    throw IllegalArgumentException("scaledHeight supports Dp or Sp only")
}


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CompassContentMain(
    state: CompassState,
    backdrop: Backdrop,
    appStyle: AppStyle,
    forecastJson: JSONObject,
    timeStr: String,
    locationName: String?,
    modifier: Modifier
) {
    val context = LocalContext.current

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val isGlass = appStyle == AppStyle.Glass

    val cookie9Shape = ScaledShape(MaterialShapes.Cookie9Sided.toShape(), scale = 1.3f)
    val archShape = ScaledShape(MaterialShapes.Arch.toShape(), scale = 1.2f)
    val squareShape = ScaledShape(MaterialShapes.Square.toShape(), scale = 1.2f)
    val cookie4Shape = ScaledShape(MaterialShapes.Cookie4Sided.toShape(), scale = 1.4f)

    val showWeatherRow by context.showWeatherRowFlow().collectAsState(initial = true)

    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

    if (!isLandscape) {
        Column(
            modifier = modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            val dark = isDarkTheme()
            val arrowShape = MaterialShapes.Triangle.toShape()
            val context = LocalContext.current

            val isGlass = appStyle == AppStyle.Glass
            val compassShape by context.compassShapeFlow().collectAsState(initial = 0)
            val selectedShape = CompassShapeRegistry.byId(compassShape).shapeFactory()
            val showIntercardinals by context.showIntercardinalsFlow().collectAsState(initial = true)

            val primary = MaterialTheme.colorScheme.primary
            val surface = MaterialTheme.colorScheme.surface
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
            val onSurface = MaterialTheme.colorScheme.onSurface

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                val compassSize = scaledCompassSize() // <-- tablet-safe size

                Canvas(
                    Modifier
                        .size(compassSize)    // <-- replaces fillMaxWidth().aspectRatio(1f)
                ) {
                    val width = 0.8f
                    val r = size.minDimension * width / 2
                    val cx = center.x
                    val cy = center.y

                    val shapeRadius = r
                    val shapeSize = Size(shapeRadius * 2.5f, shapeRadius * 2.5f)

                    if (appStyle == AppStyle.Material) {
                        val outline = selectedShape.createOutline(shapeSize, layoutDirection, this)
                        rotate(-state.azimuth, pivot = center) {
                            translate(
                                left = cx - shapeSize.width / 2f,
                                top = cy - shapeSize.height / 2f
                            ) {
                                drawOutline(outline = outline, color = primary, style = Fill)
                            }
                        }
                    }

                    val tickColor = if (appStyle == AppStyle.Material) surfaceVariant else onSurface
                    val tickVariant = if (appStyle == AppStyle.Material) surface else if (dark) Color.White else Color.Black

                    rotate(-state.azimuth - 90f, pivot = center) {
                        drawDial(tickColor, tickVariant, showIntercardinals = showIntercardinals, r = r)
                    }

                    val az = (state.azimuth % 360 + 360) % 360
                    val arrowFill = if (az < 2f || az > 358f) Color.Red else surface

                    drawNorthIndicator(
                        x = cx,
                        y = cy - r + (r * 0.65f),
                        size = r * 0.2f,
                        fillColor = arrowFill,
                        arrowShape = arrowShape
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = Paint().apply {
                            isAntiAlias = true
                            color = tickVariant.toArgb()
                            textSize = r * 0.35f
                            textAlign = AndroidPaint.Align.CENTER
                            style = AndroidPaint.Style.FILL_AND_STROKE
                            strokeJoin = AndroidPaint.Join.ROUND
                            strokeMiter = 10f
                            strokeWidth = 4f
                            isFakeBoldText = true
                        }
                        drawText("${state.azimuth.toInt()}°", cx, cy + paint.textSize / 3, paint)
                    }
                }
            }

            // --- Info Section (Sensor + Weather) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Sensor Row ---
                val magMagnitude = sqrt(
                    state.magField.first * state.magField.first +
                            state.magField.second * state.magField.second +
                            state.magField.third * state.magField.third
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scaled(36.dp)),
                    horizontalArrangement = Arrangement.spacedBy(
                        scaled(if (isGlass) 10.dp else 30.dp),
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SensorCard(
                        icon = R.drawable.ic_altitude,
                        label = stringResource(R.string.altitude),
                        value = "${state.baroAltitude.roundToInt()} m",
                        shape = cookie4Shape,
                        color = secondaryContainer,
                        index = 1,
                        backdrop = backdrop,
                    )

                    val pressurePsi = state.pressureHpa * 0.0145038f

                    SensorCard(
                        icon = R.drawable.ic_pressure,
                        label = "PSI",
                        value = "${pressurePsi.roundToInt()} psi",
                        shape = archShape,
                        color = secondaryContainer,
                        index = 2,
                        backdrop = backdrop,
                    )

                    SensorCard(
                        icon = R.drawable.ic_calibration,
                        label = "Mag",
                        value = "${magMagnitude.roundToInt()} μT",
                        shape = cookie4Shape,
                        color = secondaryContainer,
                        index = 3,
                        backdrop = backdrop,
                    )
                }

                Spacer(Modifier.height(if (isGlass) 10.dp else 30.dp))

                // --- Weather Row ---
                val hasForecast = forecastJson.has("current")
                if (hasForecast) {
                    val cur = forecastJson.getJSONObject("current")
                    val temp = cur.getDouble("temperature_2m")
                    val temperature = "${temp.roundToInt()}°C"
                    val hum = cur.getDouble("relative_humidity_2m")
                    val humidity = "${hum.roundToInt()}%"

                    when {
                        locationPermissionState.status == PermissionStatus.Granted &&
                                temperature != "0.0" && showWeatherRow -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = scaled(36.dp)),
                                horizontalArrangement = Arrangement.spacedBy(
                                    scaled(if (isGlass) 10.dp else 30.dp),
                                    Alignment.CenterHorizontally
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SensorCard(
                                    icon = R.drawable.ic_clock,
                                    label = "Time",
                                    value = timeStr,
                                    shape = squareShape,
                                    color = secondaryContainer,
                                    index = 1,
                                    backdrop = backdrop
                                )
                                SensorCard(
                                    icon = R.drawable.ic_temperature,
                                    label = "Temp",
                                    value = temperature,
                                    shape = cookie9Shape,
                                    color = secondaryContainer,
                                    index = 2,
                                    backdrop = backdrop
                                )
                                SensorCard(
                                    icon = R.drawable.ic_humidity,
                                    label = "Humidity",
                                    value = humidity,
                                    shape = archShape,
                                    color = secondaryContainer,
                                    index = 3,
                                    backdrop = backdrop
                                )
                            }

                            locationName?.let { name ->
                                Spacer(Modifier.height(30.dp))
                                LocationCard(
                                    name = name,
                                    index = 4,
                                    modifier = Modifier.padding(horizontal = scaled(36.dp))
                                )
                            }
                        }

                        locationPermissionState.status is PermissionStatus.Denied -> {
                            Button(
                                onClick = { locationPermissionState.launchPermissionRequest() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Allow Location Services")
                            }
                        }

                        else -> {}
                    }
                }
            }
            Spacer(Modifier.height(BottomSpacer))
        }
    }


    else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🧭 LEFT: The Compass Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                CompassCanvas(
                    state = state,
                    appStyle = appStyle
                )
            }

            // 📊 RIGHT: Scrollable column of cards
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val magMagnitude = sqrt(
                    state.magField.first * state.magField.first +
                            state.magField.second * state.magField.second +
                            state.magField.third * state.magField.third
                )

                // --- Sensor cards row ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (isGlass) scaledWidth(10.dp) else scaledWidth(30.dp),
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SensorCard(
                        icon = R.drawable.ic_altitude,
                        label = stringResource(R.string.altitude),
                        value = "${state.baroAltitude.roundToInt()} m",
                        shape = cookie4Shape,
                        color = secondaryContainer,
                        index = 1,
                        backdrop = backdrop,
                    )

                    val pressurePsi = state.pressureHpa * 0.0145038f
                    SensorCard(
                        icon = R.drawable.ic_pressure,
                        label = "PSI",
                        value = "${pressurePsi.roundToInt()} psi",
                        shape = archShape,
                        color = secondaryContainer,
                        index = 2,
                        backdrop = backdrop,
                    )

                    SensorCard(
                        icon = R.drawable.ic_calibration,
                        label = "Mag",
                        value = "${magMagnitude.roundToInt()} μT",
                        shape = cookie4Shape,
                        color = secondaryContainer,
                        index = 3,
                        backdrop = backdrop,
                    )
                }

                Spacer(Modifier.height(30.dp))

                // --- Weather cards ---
                val hasForecast = forecastJson.has("current")
                if (hasForecast) {
                    val cur = forecastJson.getJSONObject("current")
                    val temp = cur.getDouble("temperature_2m")
                    val temperature = "${temp.roundToInt()}°C"
                    val hum = cur.getDouble("relative_humidity_2m")
                    val humidity = "${hum.roundToInt()}%"

                    if (locationPermissionState.status == PermissionStatus.Granted &&
                        !temp.isNaN() && temp != 0.0 && showWeatherRow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                if (isGlass) scaledWidth(10.dp) else scaledWidth(30.dp),
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SensorCard(
                                icon = R.drawable.ic_cloud,
                                label = "Time",
                                value = timeStr,
                                shape = squareShape,
                                color = secondaryContainer,
                                index = 1,
                                backdrop = backdrop
                            )
                            SensorCard(
                                icon = R.drawable.ic_temperature,
                                label = "Temp",
                                value = temperature,
                                shape = cookie9Shape,
                                color = secondaryContainer,
                                index = 2,
                                backdrop = backdrop
                            )
                            SensorCard(
                                icon = R.drawable.ic_humidity,
                                label = "Humidity",
                                value = humidity,
                                shape = archShape,
                                color = secondaryContainer,
                                index = 3,
                                backdrop = backdrop
                            )
                        }

                        locationName?.let { name ->
                            Spacer(Modifier.height(30.dp))
                            LocationCard(
                                name = name,
                                index = 4
                            )
                        }
                    } else if (locationPermissionState.status is PermissionStatus.Denied) {
                        Button(
                            onClick = { locationPermissionState.launchPermissionRequest() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow Location Services")
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun scaledCompassSize(
    base: Dp = 280.dp,             // ideal phone compass size
    maxTabletSize: Dp = 600.dp     // limit tablets
): Dp {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp

    // Typical phone width = 360dp
    val scale = widthDp / 360f
    val scaled = base * scale

    // safety clamp
    return scaled.coerceIn(base, maxTabletSize)
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WeatherAndLocationSection(
    appStyle: AppStyle,
    forecastJson: JSONObject,
    timeStr: String,
    backdrop: Backdrop
) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val isGlass = appStyle == AppStyle.Glass

    val cookie9Shape = ScaledShape(MaterialShapes.Cookie9Sided.toShape(), scale = 1.1f)
    val archShape = ScaledShape(MaterialShapes.Arch.toShape(), scale = 1f)
    val squareShape = ScaledShape(MaterialShapes.Square.toShape(), scale = 1f)
    val cookie4Shape = ScaledShape(MaterialShapes.Cookie4Sided.toShape(), scale = 1.15f)

    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val hasForecast = forecastJson.has("current")
    val showWeatherRow by context.showWeatherRowFlow().collectAsState(initial = true)

    // 📱 Detect orientation
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

    if (!hasForecast) return

    val cur = forecastJson.getJSONObject("current")
    val daily = forecastJson.getJSONObject("daily")

    val temp = cur.optDouble("temperature_2m", Double.NaN)
    val humidity = cur.optDouble("relative_humidity_2m", Double.NaN)
    val feelsLike = cur.optDouble("apparent_temperature", Double.NaN)
    val precipitation = cur.optDouble("precipitation", Double.NaN)
    val rain = cur.optDouble("rain", Double.NaN)

    val temperatureText = "${temp.roundToInt()}°C"
    val humidityText = "${humidity.roundToInt()}%"
    val feelsLikeText = "${feelsLike.roundToInt()}°C"
    val rainText = "$rain mm"
    val precipText = "$precipitation mm"

    val maxTemp = daily.getJSONArray("temperature_2m_max")[0].toString()
    val minTemp = daily.getJSONArray("temperature_2m_min")[0].toString()

    val hourly = forecastJson.getJSONObject("hourly")
    val hourlyTimes = hourly.getJSONArray("time")
    val hourlyTemps = hourly.getJSONArray("temperature_2m")
    val hourlyFeels = hourly.getJSONArray("apparent_temperature")
    val hourlyClouds = hourly.getJSONArray("cloud_cover")

    val nextHours = (0 until minOf(24, hourlyTemps.length())).map { i ->
        val time = hourlyTimes.getString(i).substringAfter("T")
        val tempH = hourlyTemps.getDouble(i).toInt()
        val feelH = hourlyFeels.getDouble(i).toInt()
        val cloud = hourlyClouds.getInt(i)
        HourlyForecast(time, tempH, feelH, cloud)
    }

    if (locationPermissionState.status == PermissionStatus.Granted &&
        temperatureText != "0.0" && showWeatherRow
    ) {
        if (!isLandscape) {
            // 🌤 Portrait layout (unchanged)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(scaled(20.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WeatherRows(
                    timeStr, temperatureText, humidityText, feelsLikeText,
                    rainText, precipText, maxTemp, minTemp,
                    isGlass, secondaryContainer, backdrop,
                    squareShape, cookie9Shape, archShape, cookie4Shape
                )

                HourlyForecastCard(hours = nextHours)
                Spacer(Modifier.height(BottomSpacer))

            }
        } else {
            // 💻 Landscape layout → nextHours on top right, min/max below it
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: weather info (time/temp/humidity/etc.)
                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            if (isGlass) 10.dp else 30.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SensorCard(R.drawable.ic_clock, "Time", timeStr, squareShape, secondaryContainer, 0, backdrop)
                        SensorCard(R.drawable.ic_temperature, "Temp", temperatureText, cookie9Shape, secondaryContainer, 0, backdrop)
                        SensorCard(R.drawable.ic_humidity, "Humidity", humidityText, archShape, secondaryContainer, 0, backdrop)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            if (isGlass) 10.dp else 30.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SensorCard(R.drawable.ic_thermostat, "Feels like", feelsLikeText, cookie4Shape, secondaryContainer, 0, backdrop)
                        SensorCard(R.drawable.ic_rainy, "Rain", rainText, archShape, secondaryContainer, 0, backdrop)
                        SensorCard(R.drawable.ic_cloud, "Precipitation", precipText, archShape, secondaryContainer, 0, backdrop)
                    }
                }

                // Right side: Hourly forecast + min/max below
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HourlyForecastCard(hours = nextHours)

                    // ↓ Min/Max row goes BELOW HourlyForecastCard
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            if (isGlass) 10.dp else 40.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SensorCard(
                            R.drawable.ic_thermostat, "Max Temp", maxTemp,
                            cookie4Shape, secondaryContainer, 0, backdrop
                        )
                        SensorCard(
                            R.drawable.ic_thermostat, "Min Temp", minTemp,
                            archShape, secondaryContainer, 0, backdrop
                        )
                    }
                }
            }
        }
    } else if (locationPermissionState.status is PermissionStatus.Denied) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                Text("Allow Location Services")
            }
        }
    }
}


@Composable
private fun WeatherRows(
    timeStr: String,
    temperatureText: String,
    humidityText: String,
    feelsLikeText: String,
    rainText: String,
    precipText: String,
    maxTemp: String,
    minTemp: String,
    isGlass: Boolean,
    secondaryContainer: Color,
    backdrop: Backdrop,
    squareShape: ScaledShape,
    cookie9Shape: ScaledShape,
    archShape: ScaledShape,
    cookie4Shape: ScaledShape
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scaled(36.dp)),
            horizontalArrangement = Arrangement.spacedBy(scaled(if (isGlass) 10.dp else 12.dp), Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SensorCard(R.drawable.ic_clock, "Time", timeStr, squareShape, secondaryContainer, 0, backdrop)
            SensorCard(R.drawable.ic_temperature, "Temp", temperatureText, cookie9Shape, secondaryContainer, 0, backdrop)
            SensorCard(R.drawable.ic_humidity, "Humidity", humidityText, archShape, secondaryContainer, 0, backdrop)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scaled(36.dp)),
            horizontalArrangement = Arrangement.spacedBy(scaled(if (isGlass) 10.dp else 12.dp), Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SensorCard(R.drawable.ic_thermostat, "Feels like", feelsLikeText, cookie4Shape, secondaryContainer, 0, backdrop)
            SensorCard(R.drawable.ic_rainy, "Rain", rainText, archShape, secondaryContainer, 0, backdrop)
            SensorCard(R.drawable.ic_cloud, "Precipitation", precipText, archShape, secondaryContainer, 0, backdrop)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scaled(36.dp)),
            horizontalArrangement = Arrangement.spacedBy(scaled(if (isGlass) 10.dp else 12.dp), Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SensorCard(R.drawable.ic_thermostat, "Max Temp", maxTemp, cookie4Shape, secondaryContainer, 0, backdrop)
            SensorCard(R.drawable.ic_thermostat, "Min Temp", minTemp, archShape, secondaryContainer, 0, backdrop)
        }
    }
}

fun DrawScope.drawDial(
    tickColor: Color,
    tickVariantColor: Color,
    showIntercardinals: Boolean,
    r: Float
) {
    val inset = 30f

    val step = 2.5f
    val steps = (0..(360 / step).toInt())

    for (j in steps) {
        val i = j * step
        val angle = Math.toRadians(i.toDouble()).toFloat()
        val length = 36f
        val stroke = when {
            i % 90 == 0f -> 10f
            i % 45 == 0f -> 6f
            else -> 4f
        }

        val startRadius = if (i % 45 == 0f) r - length - inset else r - length - inset / 2f
        val endRadius = if (i % 45 == 0f) r+ inset/2 else r

        val start = center + Offset(startRadius * cos(angle), startRadius * sin(angle))
        val end = center + Offset(endRadius * cos(angle), endRadius * sin(angle))

        val color = when {
            i == 0f -> Color.Red
            i % 90 == 0f -> tickVariantColor
            else -> tickColor
        }

        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }


    // Labels
    val base = mutableListOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
    if (showIntercardinals) {
        base += listOf("NE" to 45f, "SE" to 135f, "SW" to 225f, "NW" to 315f)
    }

    base.forEach { (lbl, deg) ->
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        // also pull labels inward a bit (0.65 → 0.55)
        val pos = center + Offset((r * 0.55f) * cos(rad), (r * 0.55f) * sin(rad))

        val isCardinal = lbl.length == 1
        val textSize = if (isCardinal) 60f else 40f

        drawContext.canvas.nativeCanvas.apply {
            withSave {
                rotate(deg + 90f, pos.x, pos.y)
                drawText(
                    lbl,
                    pos.x,
                    pos.y,
                    AndroidPaint().apply {
                        color = if (lbl == "N") android.graphics.Color.RED
                        else tickColor.toArgb()
                        this.textSize = textSize
                        textAlign = AndroidPaint.Align.CENTER
                        isFakeBoldText = isCardinal
                    }
                )
            }
        }
    }
}


private fun DrawScope.drawNorthIndicator(
    x: Float,
    y: Float,
    size: Float,
    fillColor: Color,
    arrowShape: Shape
) {
    val outline = arrowShape.createOutline(Size(size, size), layoutDirection, this)
    val half = size / 2f
    translate(left = x - half, top = y - half) {
        drawOutline(outline, color = fillColor, style = Fill)
    }
}

private val DrawScope.radius get() = size.minDimension / 1.7f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CompassCanvas(
    state: CompassState,
    appStyle: AppStyle,
) {
    val dark = isDarkTheme()
    val arrowShape = MaterialShapes.Triangle.toShape()
    val context = LocalContext.current

    val compassShape by context.compassShapeFlow().collectAsState(initial = 0)
    val selectedShape = CompassShapeRegistry.byId(compassShape).shapeFactory()
    val showIntercardinals by context.showIntercardinalsFlow().collectAsState(initial = true)

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
//        if (isGlass){
//
////            Box(
////                modifier = Modifier
////                    .fillMaxSize(0.7f)
////                    .aspectRatio(1f)
//////                    .drawBackdrop(
//////                        backdrop = backdrop,
//////                        effects =  {
//////                            vibrancy()
//////                            blur(4.dp.toPx())
//////                            lens(
//////                                refractionHeight = 24.dp.toPx(),
//////                                refractionAmount = 60.dp.toPx(),
//////                                depthEffect = true
//////                            )
//////                        },
//////                        shape = { RoundedCornerShape(percent = 50) },
//////                        onDrawSurface = { drawRect(if(dark) Color(0xFF313131).copy(alpha = 0.2f) else Color(0xFFBDBDBD).copy(alpha = 0.2f)) }
//////                    )
////            ) {
////                Canvas(
////                    modifier = Modifier
////                        .matchParentSize()
////                        .aspectRatio(1f)
////                ) {
////                    val r = size.minDimension / 2f
////                    val cx = center.x
////                    val cy = center.y
////
////                    // dial ticks
////                    rotate(-state.azimuth - 90f, pivot = center) {
////                        drawDial(
////                            tickColor = if (dark) Color.White else Color.Black,
////                            tickVariantColor = if (dark) Color.White else Color.Black,
////                            showIntercardinals = showIntercardinals,
////                            r = r
////                        )
////                    }
////
////                    // north indicator
////                    val az = (state.azimuth % 360 + 360) % 360
////                    val nearNorth = az < 2f || az > 358f
////                    val arrowFill = if (nearNorth) Color.Red else if (dark) Color.White else Color.Black
////
////                    drawNorthIndicator(
////                        x = cx,
////                        y = cy - r + (r * 0.65f),
////                        size = r * 0.2f,
////                        fillColor = arrowFill,
////                        arrowShape = arrowShape
////                    )
////
////                    // text
////                    drawContext.canvas.nativeCanvas.apply {
////                        val paint = Paint().apply {
////                            isAntiAlias = true
////                            color = (if (dark) Color.White else Color.Black).toArgb()
////                            textSize = r * 0.35f
////                            textAlign = AndroidPaint.Align.CENTER
////                            style = AndroidPaint.Style.FILL_AND_STROKE
////                            strokeWidth = 4f
////                            isFakeBoldText = true
////                        }
////                        drawText("${state.azimuth.toInt()}°", cx, cy + paint.textSize / 3, paint)
////                    }
////                }
////            }
//        }
//        else{
//            Canvas(Modifier.fillMaxSize(0.65f)) {
//                val r = radius
//                val cx = center.x
//                val cy = center.y
//
//                val shapePadding = r * -0.20f
//                val shapeRadius = r - shapePadding
//                val shapeSize = Size(shapeRadius * 2f, shapeRadius * 2f)
//
//                if (appStyle== AppStyle.Material) {
//                    val outline = selectedShape.createOutline(shapeSize, layoutDirection, this)
//                    rotate(-state.azimuth, pivot = center) {
//                        translate(left = cx - shapeRadius, top = cy - shapeRadius) {
//                            drawOutline(outline = outline, color = primary, style = Fill)
//                        }
//                    }
//                }
//
//                rotate(-state.azimuth - 90f, pivot = center) {
//                    drawDial(surfaceVariant, surface, showIntercardinals = showIntercardinals,
//                        r = r
//                    )
//                }
//
//                val az = (state.azimuth % 360 + 360) % 360
//                val nearNorth = az < 2f || az > 358f
////            val arrowFill = if (nearNorth) Color.Red else if (waypoint != null && abs(az - waypoint.roundToInt()) < 2) waypointColor else surface
//                val arrowFill = if (nearNorth) Color.Red else surface
//
//                drawNorthIndicator(
//                    x = cx,
//                    y = cy - r + (r * 0.65f),
//                    size = r * 0.2f,
//                    fillColor = arrowFill,
//                    arrowShape = arrowShape
//                )
//                drawContext.canvas.nativeCanvas.apply {
//                    val paint = Paint().apply {
//                        isAntiAlias = true
//                        color = surface.toArgb()
//                        textSize = r * 0.35f
//                        textAlign = AndroidPaint.Align.CENTER
//                        style = AndroidPaint.Style.FILL_AND_STROKE
//                        strokeJoin = AndroidPaint.Join.ROUND
//                        strokeMiter = 10f
//                        strokeWidth = 4f
//                        isFakeBoldText = true
//                    }
//                    drawText("${state.azimuth.toInt()}°", cx, cy + paint.textSize / 3, paint)
//                }
//
//            }
//        }
        Canvas(Modifier.fillMaxSize(0.65f)) {
            val r = radius
            val cx = center.x
            val cy = center.y

            val shapePadding = r * -0.20f
            val shapeRadius = r - shapePadding
            val shapeSize = Size(shapeRadius * 2f, shapeRadius * 2f)

            if (appStyle== AppStyle.Material) {
                val outline = selectedShape.createOutline(shapeSize, layoutDirection, this)
                rotate(-state.azimuth, pivot = center) {
                    translate(left = cx - shapeRadius, top = cy - shapeRadius) {
                        drawOutline(outline = outline, color = primary, style = Fill)
                    }
                }
            }
            val tickColor = if (appStyle== AppStyle.Material) surfaceVariant else onSurface
            val tickColorVarient = if (appStyle== AppStyle.Material) surface else if (dark) Color.White else Color.Black

            rotate(-state.azimuth - 90f, pivot = center) {
                drawDial(tickColor, tickColorVarient, showIntercardinals = showIntercardinals,
                    r = r
                )
            }

            val az = (state.azimuth % 360 + 360) % 360
            val nearNorth = az < 2f || az > 358f
            val arrowFill = if (nearNorth) Color.Red else surface

            drawNorthIndicator(
                x = cx,
                y = cy - r + (r * 0.65f),
                size = r * 0.2f,
                fillColor = arrowFill,
                arrowShape = arrowShape
            )
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    isAntiAlias = true
                    color = tickColorVarient.toArgb()
                    textSize = r * 0.35f
                    textAlign = AndroidPaint.Align.CENTER
                    style = AndroidPaint.Style.FILL_AND_STROKE
                    strokeJoin = AndroidPaint.Join.ROUND
                    strokeMiter = 10f
                    strokeWidth = 4f
                    isFakeBoldText = true
                }
                drawText("${state.azimuth.toInt()}°", cx, cy + paint.textSize / 3, paint)
            }

        }
    }
}
