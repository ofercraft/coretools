package com.feldman.coretools.ui.pages

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.compose.ui.unit.sp
import com.feldman.coretools.MainApp
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.UsageItem
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.storage.hideHomeFlow
import com.feldman.coretools.storage.hideSaverFlow
import com.feldman.coretools.storage.setHideHome
import com.feldman.coretools.storage.setHideSaver
import com.feldman.coretools.storage.setThemedIcons
import com.feldman.coretools.storage.themedIconsFlow
import com.feldman.coretools.Dest
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay
import kotlin.collections.iterator



private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun Drawable.toBitmap(width: Int, height: Int): android.graphics.Bitmap {
    if (this is android.graphics.drawable.BitmapDrawable) return this.bitmap
    val bitmap = createBitmap(width, height)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

private fun formatDuration(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "${s}s"
    }
}

private fun launcherPackages(pm: PackageManager): Set<String> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val infos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return infos.map { it.activityInfo.packageName }.toSet()
}
private fun screensaverPackages(pm: PackageManager): Set<String> {
    val intent = Intent("android.service.dreams.DreamService")
    val infos = pm.queryIntentServices(intent, 0)
    return infos.map { it.serviceInfo.packageName }.toSet()
}

private fun tryLoadMonochromeIcon(pm: PackageManager, pkg: String): Drawable? {
    val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg)
    val ri = pm.queryIntentActivities(launchIntent, 0).firstOrNull() ?: return null
    val actInfo = ri.activityInfo

    val monoRes: Int = try {
        val field = actInfo.javaClass.getField("monochromeIcon")
        field.getInt(actInfo)
    } catch (_: Throwable) { 0 }
    if (monoRes != 0) {
        return try { pm.getDrawable(pkg, monoRes, actInfo.applicationInfo) } catch (_: Throwable) { null }
    }

    val normal = try { ri.loadIcon(pm) } catch (_: Throwable) { null }
    if (normal is AdaptiveIconDrawable) {
        val mono: Drawable? = try {
            AdaptiveIconDrawable::class.java.getMethod("getMonochrome").invoke(normal) as? Drawable
        } catch (_: Throwable) { null }
        if (mono != null) return mono
    }
    return null
}

private fun preferMonochromeIfAvailable(
    drawable: Drawable,
    themed: Boolean,
    tintColor: Int
): Drawable {
    if (!themed) return drawable
    if (drawable is AdaptiveIconDrawable) {
        val mono = drawable.monochrome
        if (mono != null) return mono.mutate().apply { setTint(tintColor) }
    }
    return drawable.mutate().apply { setTint(tintColor) }
}

private fun getUsageItemsForDay(
    context: Context,
    startTime: Long,
    endTime: Long,
    hideHome: Boolean,
    hideSaver: Boolean
): List<UsageItem> {
    val mgr = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val pm = context.packageManager
    val events = mgr.queryEvents(startTime, endTime)
    val evt = UsageEvents.Event()

    val lastStart = mutableMapOf<String, Long>()
    val totalTime = mutableMapOf<String, Long>()

    while (events.hasNextEvent()) {
        events.getNextEvent(evt)
        val pkg = evt.packageName ?: continue
        val ts = evt.timeStamp
        when (evt.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> lastStart[pkg] = ts
            UsageEvents.Event.ACTIVITY_PAUSED -> {
                val t0 = lastStart.remove(pkg)
                val delta = if (t0 != null) ts - t0 else ts - startTime
                if (delta > 0) totalTime[pkg] = totalTime.getOrDefault(pkg, 0L) + delta
            }
        }
    }
    for ((pkg, t0) in lastStart) {
        val delta = endTime - t0
        if (delta > 0) totalTime[pkg] = totalTime.getOrDefault(pkg, 0L) + delta
    }

    val agg = mgr.queryAndAggregateUsageStats(startTime, endTime)
    val homePkgs = if (hideHome) launcherPackages(pm) else emptySet()
    val saverPkgs = if (hideSaver) screensaverPackages(pm) else emptySet()

    val out = mutableListOf<UsageItem>()
    for ((pkg, eventMs) in totalTime) {
        if (pkg in homePkgs || pkg in saverPkgs) continue
        val aggMs = agg[pkg]?.totalTimeInForeground ?: 0L
        val finalMs = minOf(eventMs, aggMs)
        if (finalMs < 60_000) continue

        var label = pkg
        var icon: Drawable = ContextCompat.getDrawable(context, _root_ide_package_.com.feldman.coretools.R.drawable.ic_launcher_foreground)!!
        var themed: Drawable? = null
        try {
            val ai = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(ai).let { label = it.toString() }
            icon = pm.getApplicationIcon(ai)
            themed = tryLoadMonochromeIcon(pm, pkg)
        } catch (_: PackageManager.NameNotFoundException) {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg)
            pm.queryIntentActivities(i, 0).firstOrNull()?.let { ri ->
                label = ri.loadLabel(pm).toString()
                icon = ri.loadIcon(pm)
                themed = tryLoadMonochromeIcon(pm, pkg)
            } ?: continue
        }
        out.add(UsageItem(pkg,label, finalMs, icon, themed))
    }
    return out.sortedByDescending { it.timeUsedMs }
}

// --------------------------------------------------------------------------------------
// Composable Usage Screen (no Activity)
// --------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var usageItems by remember { mutableStateOf(emptyList<UsageItem>()) }
    var totalUsage by remember { mutableLongStateOf(0L) }
    var dateLabel by remember { mutableStateOf("Today") }
    val dayOffset = rememberSaveable { mutableIntStateOf(0) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }


    val hideHome by context.hideHomeFlow().collectAsState(initial = true)
    val hideSaver by context.hideSaverFlow().collectAsState(initial = true)
    val themedIcons by context.themedIconsFlow().collectAsState(initial = false)

    val scope = rememberCoroutineScope()

    fun loadDay(offset: Int) {
        val cal = Calendar.getInstance().apply {
            normalizeToStartOfDay(this)
            add(Calendar.DAY_OF_MONTH, -offset)
        }
        val start = (cal.clone() as Calendar).apply { normalizeToStartOfDay(this) }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        if (offset == 0) end.timeInMillis = System.currentTimeMillis()

        val items = getUsageItemsForDay(
            context = context,
            startTime = start.timeInMillis,
            endTime = end.timeInMillis,
            hideHome = hideHome,
            hideSaver = hideSaver
        )
        usageItems = items
        totalUsage = items.sumOf { it.timeUsedMs }
        dateLabel = when (offset) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(cal.time)
        }
    }

    LaunchedEffect(dayOffset.intValue, hideHome, hideSaver, themedIcons, hasPermission) {
        if (hasPermission) {
            loadDay(dayOffset.intValue)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!hasPermission) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Grant CoreTools usage access\nto use this page",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .height(60.dp)
            ) {
                Text("Open Settings", fontSize = 20.sp)
            }


        }
        return
    }

    UsagePage(
        usageItems = usageItems,
        totalUsage = totalUsage,
        dateLabel = dateLabel,
        canGoBack = dayOffset.intValue < 9,
        canGoForward = dayOffset.intValue > 0,
        sheetVisible = showSheet,
        onDismissSheet = { showSheet = false },
        hideHome = hideHome,
        hideSaver = hideSaver,
        onHideHomeChange = { checked -> scope.launch { context.setHideHome(checked) } },
        onHideSaverChange = { checked -> scope.launch { context.setHideSaver(checked) } },
        themedIcons = themedIcons,
        onThemedIconsChange = { checked -> scope.launch { context.setThemedIcons(checked) } },
        modifier = modifier,
        dayOffset = dayOffset
    )
}
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DayPickerDialog(
    currentOffset: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
    appStyle: AppStyle,
    backdrop: LayerBackdrop
) {
    val daysByMonth = remember {
        val today = Calendar.getInstance()
        (0..9).map { offset ->
            val cal = today.clone() as Calendar
            cal.add(Calendar.DAY_OF_MONTH, -offset)
            val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            Triple(offset, month, day) // (offset, monthName, dayNumber)
        }.groupBy { it.second } // group by month name, preserves order
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                daysByMonth.forEach { (monthName, entries) ->

                    // Month header
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    // Today / Yesterday row
                    val specialOffsets = listOf(0, 1).filter { off ->
                        entries.any { it.first == off }
                    }
                    if (specialOffsets.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            specialOffsets.forEach { offset ->
                                val label = if (offset == 0) "Today" else "Yesterday"
                                if (appStyle == AppStyle.Glass) {
                                    Button(
                                        onClick = { onPick(offset) },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentOffset == offset)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = if (currentOffset == offset)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .drawBackdrop(
                                                backdrop = backdrop,
                                                effects = {
                                                    vibrancy()
                                                    blur(4.dp.toPx())
                                                    lens(20.dp.toPx(), 20.dp.toPx(), true)
                                                },
                                                shape = { RoundedCornerShape(50) },
                                            )
                                            .height(50.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                    ) { Text(label, style = MaterialTheme.typography.bodyMedium) }
                                } else {
                                    Button(
                                        onClick = { onPick(offset) },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentOffset == offset)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = if (currentOffset == offset)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                    ) { Text(label, style = MaterialTheme.typography.bodyMedium) }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        entries
                            .filterNot { it.first == 0 || it.first == 1 }
                            .forEach { (offset, _, dayNum) ->
                                if (appStyle == AppStyle.Glass) {
                                    Button(
                                        onClick = { onPick(offset) },
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentOffset == offset)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (currentOffset == offset)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier
                                            .size(40.dp)
                                            .drawBackdrop(
                                                backdrop = backdrop,
                                                effects = {
                                                    vibrancy()
                                                    blur(4.dp.toPx())
                                                    lens(20.dp.toPx(), 20.dp.toPx(), true)
                                                },
                                                shape = { RoundedCornerShape(50) },
                                            )
                                    ) { Text(dayNum.toString(), style = MaterialTheme.typography.bodyMedium) }
                                } else {
                                    Button(
                                        onClick = { onPick(offset) },
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentOffset == offset)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (currentOffset == offset)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(40.dp)
                                    ) { Text(dayNum.toString(), style = MaterialTheme.typography.bodyMedium) }
                                }
                            }
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {}
    )
}





@Composable
private fun DayHeader(
    dateLabel: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    backdrop: Backdrop,
    showDayPicker: MutableState<Boolean>
) {
    val chipHeight = 48.dp
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass


    val dark = isDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDayPicker.value = true }
            .padding(horizontal = 16.dp)
            .height(chipHeight),
        contentAlignment = Alignment.Center
    ) {
        if (isGlass) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(chipHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (canGoBack) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    effects =  {
                                        vibrancy()
                                        blur(4.dp.toPx())
                                        lens(
                                            refractionHeight = 24f.dp.toPx(),
                                            refractionAmount = 60f.dp.toPx(),
                                            depthEffect = true
                                        )
                                    },
                                    shape = { RoundedCornerShape(32.dp) },
                                    onDrawSurface = { drawRect(if(dark) Color(0xFF313131).copy(alpha = 0.2f) else Color(0xFFBDBDBD).copy(alpha = 0.2f) ) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = onPrevDay, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Day",
                                    tint = if (dark) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .height(chipHeight)
                        .width(200.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            effects =  {
                                vibrancy()
                                blur(4.dp.toPx())
                                lens(
                                    refractionHeight = 24f.dp.toPx(),
                                    refractionAmount = 60f.dp.toPx(),
                                    depthEffect = true
                                )
                            },
                            shape = { RoundedCornerShape(32.dp) },
                            onDrawSurface = { drawRect(if(dark) Color(0xFF313131).copy(alpha = 0.2f) else Color(0xFFBDBDBD).copy(alpha = 0.2f) ) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (dark) Color.White else Color.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    modifier = Modifier.size(chipHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (canGoForward) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    effects =  {
                                        vibrancy()
                                        blur(4.dp.toPx())
                                        lens(
                                            refractionHeight = 24f.dp.toPx(),
                                            refractionAmount = 60f.dp.toPx(),
                                            depthEffect = true
                                        )
                                    },
                                    shape = { RoundedCornerShape(32.dp) },
                                    onDrawSurface = { drawRect(if(dark) Color(0xFF313131).copy(alpha = 0.2f) else Color(0xFFBDBDBD).copy(alpha = 0.2f) ) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = onNextDay, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Day",
                                    tint = if (dark) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
        else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(chipHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (canGoBack) {
                        IconButton(onClick = onPrevDay, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Day",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .height(chipHeight)
                        .width(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(4.dp))

                Box(
                    modifier = Modifier.size(chipHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (canGoForward) {
                        IconButton(onClick = onNextDay, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Day",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

        }
    }

}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsagePage(
    usageItems: List<UsageItem>,
    totalUsage: Long,
    dateLabel: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    sheetVisible: Boolean,
    onDismissSheet: () -> Unit,
    hideHome: Boolean,
    hideSaver: Boolean,
    onHideHomeChange: (Boolean) -> Unit,
    onHideSaverChange: (Boolean) -> Unit,
    themedIcons: Boolean,
    onThemedIconsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    dayOffset: MutableState<Int>
) {

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val showDayPicker = remember { mutableStateOf(false) }

    val backdrop = rememberLayerBackdrop()
    val appStyle by context.appStyleFlow().collectAsState(initial =AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top= if (isGlass) 12.dp else 0.dp)
        ) {
            DayHeader(
                dateLabel = dateLabel,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onPrevDay = { if (dayOffset.value < 9) dayOffset.value++ },
                onNextDay = { if (dayOffset.value > 0) dayOffset.value-- },
                backdrop = backdrop,
                showDayPicker = showDayPicker
            )
            Spacer(Modifier.height(8.dp))
            val dark = isDarkTheme()
            if (isGlass){
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .drawBackdrop(
                            backdrop = backdrop,
                            effects =  {
                                vibrancy()
                                blur(4.dp.toPx())
                                lens(
                                    refractionHeight = 24f.dp.toPx(),
                                    refractionAmount = 60f.dp.toPx(),
                                    depthEffect = true
                                )
                            },
                            shape = { RoundedCornerShape(32.dp) },
                            onDrawSurface = { drawRect(if(dark) Color(0xFF313131).copy(alpha = 0.2f) else Color(0xFFBDBDBD).copy(alpha = 0.2f) ) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Total: ${formatDuration(totalUsage)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (dark) Color.White else Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else{
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(100.dp)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Total: ${formatDuration(totalUsage)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

            val columns = if (isLandscape) 5 else 3
            val hSpacing = 12.dp
            val vSpacing = 12.dp
            val backdrop = rememberLayerBackdrop()

            val animatedFlags = remember(dayOffset.value) { mutableStateMapOf<String, Boolean>() }

            LaunchedEffect(dayOffset.value, usageItems) {
                usageItems.forEachIndexed { index, item ->
                    delay(60L)
                    animatedFlags[item.appName] = true
                }
            }

            LazyVerticalGrid(
                state = rememberLazyGridState(),
                columns = GridCells.Fixed(columns),
                verticalArrangement = Arrangement.spacedBy(vSpacing),
                horizontalArrangement = Arrangement.spacedBy(hSpacing),
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = if (appStyle == AppStyle.Glass) 110.dp else 10.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = usageItems,
                    key = { _, item -> item.packageName },
                    contentType = { _: Int, _: UsageItem -> "usageCard" }
                ) { idx: Int, item: UsageItem ->
                    val visible = animatedFlags[item.appName] == true
                    UsageCard(
                        item = item,
                        themedIcons = themedIcons,
                        backdrop = backdrop,
                        index = idx,
                        visible = visible
                    )
                }

            }

        }

        if (sheetVisible) {
            ModalBottomSheet(onDismissRequest = onDismissSheet, sheetState = sheetState) {
                FiltersSheet(
                    hideHome = hideHome,
                    hideSaver = hideSaver,
                    onHideHomeChange = onHideHomeChange,
                    onHideSaverChange = onHideSaverChange,
                    themedIcons = themedIcons,
                    onThemedIconsChange = onThemedIconsChange
                )
            }
        }
        if (showDayPicker.value) {
            DayPickerDialog(
                currentOffset = dayOffset.value,
                onPick = { offset ->
                    dayOffset.value = offset
                    showDayPicker.value = false
                },
                onDismiss = { showDayPicker.value = false },
                appStyle = appStyle,
                backdrop = backdrop
            )
        }
    }
}

@Composable
fun FiltersSheet(
    hideHome: Boolean,
    hideSaver: Boolean,
    onHideHomeChange: (Boolean) -> Unit,
    onHideSaverChange: (Boolean) -> Unit,
    themedIcons: Boolean,
    onThemedIconsChange: (Boolean) -> Unit
) {


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Filters", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Hide home/launcher apps", color = MaterialTheme.colorScheme.onSurface)
            Switch(checked = hideHome, onCheckedChange = onHideHomeChange)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Hide screensaver apps", color = MaterialTheme.colorScheme.onSurface)
            Switch(checked = hideSaver, onCheckedChange = onHideSaverChange)
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Themed icons", color = MaterialTheme.colorScheme.onSurface)
            Switch(checked = themedIcons, onCheckedChange = onThemedIconsChange)
        }
        HorizontalDivider()

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UsageCard(
    item: UsageItem,
    themedIcons: Boolean,
    backdrop: Backdrop,
    index: Int,
    visible: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "UsageCardScale"
    )

    val funkyShapes = listOf(
        ScaledShape(MaterialShapes.Arch.toShape(), 1f),
        ScaledShape(MaterialShapes.Square.toShape(), 1f),
        ScaledShape(MaterialShapes.Cookie4Sided.toShape(), 1.2f)
    )

    val tintInt = MaterialTheme.colorScheme.onSurface.toArgb()

    val displayDrawable = remember(item.appIcon, item.themedIcon, themedIcons, tintInt) {
        if (themedIcons && item.themedIcon != null)
            preferMonochromeIfAvailable(item.themedIcon, true, tintInt)
        else
            preferMonochromeIfAvailable(item.appIcon, false, tintInt)
    }

    val bitmap = remember(displayDrawable) { displayDrawable.toBitmap(96, 96) }

    val baseModifier = Modifier
        .fillMaxSize()
        .height(140.dp)
        .graphicsLayer {
            alpha = if (visible) 1f else 0f
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
        }
    val context = LocalContext.current
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass
    val dark = isDarkTheme()

    if (isGlass) {
        Box(
            modifier = baseModifier
                .padding(4.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    effects = {
                        vibrancy()
                        blur(4.dp.toPx())
                        lens(
                            refractionHeight = 24f.dp.toPx(),
                            refractionAmount = 60f.dp.toPx(),
                            depthEffect = true
                        )
                    },
                    shape = { RoundedCornerShape(32.dp) },
                    onDrawSurface = { drawRect(if(dark) Color(0xFF313131).copy(alpha = 0.2f) else Color(0xFFBDBDBD).copy(alpha = 0.2f) ) }
                )
        ) {
            UsageCardContent(item, bitmap)
        }
    } else {
        Card(
            modifier = baseModifier.padding(4.dp),
            shape = funkyShapes[index % funkyShapes.size],
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            UsageCardContent(item, bitmap)
        }
    }
}





@Composable
private fun UsageCardContent(item: UsageItem, bitmap: android.graphics.Bitmap) {
    val dark = isDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = item.appName,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.appName,
            style = MaterialTheme.typography.bodySmall,
            color = if (dark) Color.White else Color.Black,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatDuration(item.timeUsedMs),
            style = MaterialTheme.typography.bodySmall,
            color = if (dark) Color.White else Color.Black
        )
    }
}

// --------------------------------------------------------------------------------------
// Small util
// --------------------------------------------------------------------------------------
private fun normalizeToStartOfDay(cal: Calendar) {
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
}
class UsagePageActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainApp(startDestination = Dest.Usage)

        }
    }
}
