package com.feldman.coretools.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.feldman.coretools.MainActivity.Dest
val Context.dataStore by preferencesDataStore(name = "usage_prefs")
object PrefKeys {
    //App
    val DEFAULT_PAGE = stringPreferencesKey("default_page")


    //Customization
    val APP_STYLE = stringPreferencesKey("app_style")
    val THEME_COLOR = intPreferencesKey("theme_color") //0 - auto, 1 - light, 2 - dark
    val ORIENTATION_MODE = stringPreferencesKey("orientation_mode")


    //Flashlight
    val INSTANT_FLASHLIGHT = booleanPreferencesKey("instant_flashlight")
    val DEFAULT_FLASHLIGHT_LEVEL = intPreferencesKey("default_flashlight_level")
    val AUTO_FLASHLIGHT_OFF = booleanPreferencesKey("auto_flashlight_off")


    //Compass
    val COMPASS_SHAPE = intPreferencesKey("compass_shape")
    val SHOW_INTERCARDINALS = booleanPreferencesKey("show_intercardinals")
    val SHOW_WEATHER_ROW = booleanPreferencesKey("show_weather_row")
    val TRUE_NORTH = booleanPreferencesKey("true_north")
    val COMPASS_VIBRATION_FEEDBACK = booleanPreferencesKey("compass_vibration_feedback")


    //Usage
    val THEMED_ICONS = booleanPreferencesKey("themed_icons")
    val HIDE_SAVER = booleanPreferencesKey("hide_saver") //screensaver apps
    val HIDE_HOME = booleanPreferencesKey("hide_home") //home apps (launchers)


    //Level
    val LEVEL_VIBRATION_FEEDBACK = booleanPreferencesKey("level_vibration_feedback")
    val LEVEL_MODE = stringPreferencesKey("level_mode") // "normal" or "reversed"


    //Tiles
    val SHOW_TILE_INFO = booleanPreferencesKey("show_tile_info")
    val OPEN_FULL_PAGES = booleanPreferencesKey("open_full_pages")


    // Speedometer
    val SPEED_UNIT = stringPreferencesKey("speed_unit")
}




//App
data class DefaultPage(val dest: Dest)
fun Context.defaultPageFlow(): Flow<Dest> =
    dataStore.data.map { prefs ->
        val key = prefs[PrefKeys.DEFAULT_PAGE]
        Dest.entries.find { it.name == key } ?: Dest.Flashlight
    }

suspend fun Context.setDefaultPage(dest: Dest) {
    dataStore.edit { it[PrefKeys.DEFAULT_PAGE] = dest.name }
}




//Customization
fun Context.themeColorFlow() =
    dataStore.data.map { it[PrefKeys.THEME_COLOR] ?: 0 }
suspend fun Context.setThemeColor(theme: Int) {
    dataStore.edit { it[PrefKeys.THEME_COLOR] = theme }
}

fun Context.appStyleFlow(): Flow<AppStyle> =
    dataStore.data.map { prefs -> AppStyle.fromKey(prefs[PrefKeys.APP_STYLE]) }
suspend fun Context.setAppStyle(style: AppStyle) {
    dataStore.edit { it[PrefKeys.APP_STYLE] = style.key }
}

fun Context.orientationModeFlow(): Flow<OrientationMode> =
    dataStore.data.map { prefs ->
        OrientationMode.fromKey(prefs[PrefKeys.ORIENTATION_MODE])
    }
suspend fun Context.setOrientationMode(mode: OrientationMode) {
    dataStore.edit { it[PrefKeys.ORIENTATION_MODE] = mode.key }
}




//Flashlight
fun Context.instantFlashlightFlow(): Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[PrefKeys.INSTANT_FLASHLIGHT] ?: false }

suspend fun Context.setInstantFlashlight(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.INSTANT_FLASHLIGHT] = enabled }
}

fun Context.defaultFlashlightLevelFlow(): Flow<Int> =
    dataStore.data.map { prefs -> prefs[PrefKeys.DEFAULT_FLASHLIGHT_LEVEL] ?: 50 }

suspend fun Context.setDefaultFlashlightLevel(percent: Int) {
    dataStore.edit { it[PrefKeys.DEFAULT_FLASHLIGHT_LEVEL] = percent.coerceIn(0, 100) }
}
fun Context.autoFlashlightOffFlow(): Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[PrefKeys.AUTO_FLASHLIGHT_OFF] ?: true }

suspend fun Context.setAutoFlashlightOff(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.AUTO_FLASHLIGHT_OFF] = enabled }
}




//Compass
fun Context.compassShapeFlow() = dataStore.data.map { it[PrefKeys.COMPASS_SHAPE] ?: 0 }
suspend fun Context.setCompassShape(shape: Int) {
    dataStore.edit { it[PrefKeys.COMPASS_SHAPE] = shape }
}

fun Context.showIntercardinalsFlow() = dataStore.data.map { it[PrefKeys.SHOW_INTERCARDINALS] ?: true }
suspend fun Context.setShowIntercardinals(show: Boolean) {
    dataStore.edit { it[PrefKeys.SHOW_INTERCARDINALS] = show }
}

fun Context.showWeatherRowFlow() = dataStore.data.map { it[PrefKeys.SHOW_WEATHER_ROW] ?: true }
suspend fun Context.setShowWeatherRow(show: Boolean) {
    dataStore.edit { it[PrefKeys.SHOW_WEATHER_ROW] = show }
}

fun Context.trueNorthFlow() = dataStore.data.map { it[PrefKeys.TRUE_NORTH] ?: false }
suspend fun Context.setTrueNorth(enabled: Boolean) { dataStore.edit { it[PrefKeys.TRUE_NORTH] = enabled } }

fun Context.compassVibrationFeedbackFlow(): Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[PrefKeys.COMPASS_VIBRATION_FEEDBACK] ?: true }
suspend fun Context.setCompassVibrationFeedback(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.COMPASS_VIBRATION_FEEDBACK] = enabled }
}



//Usage
fun Context.hideHomeFlow() =
    dataStore.data.map { it[PrefKeys.HIDE_HOME] ?: true }
suspend fun Context.setHideHome(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.HIDE_HOME] = enabled }
}

fun Context.hideSaverFlow() =
    dataStore.data.map { it[PrefKeys.HIDE_SAVER] ?: true }
suspend fun Context.setHideSaver(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.HIDE_SAVER] = enabled }
}

fun Context.themedIconsFlow() =
    dataStore.data.map { it[PrefKeys.THEMED_ICONS] ?: false }
suspend fun Context.setThemedIcons(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.THEMED_ICONS] = enabled }
}




//Level
fun Context.levelVibrationFeedbackFlow(): Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[PrefKeys.LEVEL_VIBRATION_FEEDBACK] ?: true }

suspend fun Context.setLevelVibrationFeedback(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.LEVEL_VIBRATION_FEEDBACK] = enabled }
}

fun Context.levelModeFlow(): Flow<String> =
    dataStore.data.map { prefs -> prefs[PrefKeys.LEVEL_MODE] ?: "normal" }

suspend fun Context.setLevelMode(mode: String) {
    dataStore.edit { it[PrefKeys.LEVEL_MODE] = mode }
}




//Tiles
fun Context.showTileInfoFlow(): Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[PrefKeys.SHOW_TILE_INFO] ?: false }

suspend fun Context.setShowTileInfo(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.SHOW_TILE_INFO] = enabled }
}


fun Context.openFullPagesFlow(): Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[PrefKeys.OPEN_FULL_PAGES] ?: false }

suspend fun Context.setOpenFullPages(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.OPEN_FULL_PAGES] = enabled }
}




//Speedometer
fun Context.speedUnitFlow(): Flow<String> =
    dataStore.data.map { prefs -> prefs[PrefKeys.SPEED_UNIT] ?: "kmh" }

suspend fun Context.setSpeedUnit(unit: String) {
    dataStore.edit { it[PrefKeys.SPEED_UNIT] = unit }
}