package com.feldman.coretools.ui.pages

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.R
import com.feldman.coretools.storage.appStyleFlow
import com.feldman.coretools.ui.components.HourlyForecast
import com.feldman.coretools.ui.components.HourlyForecastCard
import com.feldman.coretools.ui.components.SensorCard
import com.feldman.coretools.ui.tiles.rememberCompassState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WeatherPage(modifier: Modifier = Modifier) {

    val state = rememberCompassState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current


    LaunchedEffect(state.azimuth) {
        if (state.azimuth % 90f < 2f) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }
    val backdrop = rememberLayerBackdrop()

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)

    var timeStr by remember { mutableStateOf("--:--") }
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

                    try {
                        forecastJson = forecastDeferred.await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    val isGlass = appStyle == AppStyle.Glass

    val cookie9Shape = ScaledShape(MaterialShapes.Cookie9Sided.toShape(), scale = 1.3f)
    val archShape = ScaledShape(MaterialShapes.Arch.toShape(), scale = 1.2f)
    val squareShape = ScaledShape(MaterialShapes.Square.toShape(), scale = 1.2f)
    val cookie4Shape = ScaledShape(MaterialShapes.Cookie4Sided.toShape(), scale = 1.4f)

    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val hasForecast = forecastJson.has("current")

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasForecast) {
            val cur = forecastJson.getJSONObject("current")
            val daily = forecastJson.getJSONObject("daily")

            val temp = cur.optDouble("temperature_2m", Double.NaN)
            val humidity = cur.optDouble("relative_humidity_2m", Double.NaN)
            val feelsLike = cur.optDouble("apparent_temperature", Double.NaN)
            val precipitation = cur.optDouble("precipitation", Double.NaN)
            val rain = cur.optDouble("rain", Double.NaN)

//            val cloudCover = cur.optDouble("cloud_cover", Double.NaN)

            val temperatureText = "${temp.roundToInt()}°C"
            val humidityText = "${humidity.roundToInt()}%"
            val feelsLikeText = "${feelsLike.roundToInt()}°C"
            val rainText = "${rain} mm"
            val precipText = "${precipitation} mm"

//            val cloudText = "${cloudCover.roundToInt()}%"

            val maxTemp = daily.getJSONArray("temperature_2m_max")[0].toString()
            val minTemp = daily.getJSONArray("temperature_2m_min")[0].toString()

//            val maxApparent = daily.getJSONArray("apparent_temperature_max")[0].toString()
//            val minApparent = daily.getJSONArray("apparent_temperature_min")[0].toString()

            val hourly = forecastJson.getJSONObject("hourly")
            val hourlyTimes = hourly.getJSONArray("time")
            val hourlyTemps = hourly.getJSONArray("temperature_2m")
            val hourlyFeels = hourly.getJSONArray("apparent_temperature")
            val hourlyClouds = hourly.getJSONArray("cloud_cover")

            val nextHours = (0 until minOf(24, hourlyTemps.length())).map { i ->
                val time = hourlyTimes.getString(i).substringAfter("T")
                val temp = hourlyTemps.getDouble(i).toInt()
                val feel = hourlyFeels.getDouble(i).toInt()
                val cloud = hourlyClouds.getInt(i)
                HourlyForecast(time, temp, feel, cloud)
            }

            when {
                locationPermissionState.status==PermissionStatus.Granted && temperatureText!="0.0" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isGlass) 10.dp else 30.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SensorCard(
                            icon = R.drawable.ic_clock,
                            label = "Time",
                            value = timeStr,
                            shape = squareShape,
                            color = secondaryContainer,
                            index = 0,
                            backdrop = backdrop
                        )
                        SensorCard(
                            icon = R.drawable.ic_temperature,
                            label = "Temp",
                            value = temperatureText,
                            shape = cookie9Shape,
                            color = secondaryContainer,
                            index = 0,
                            backdrop = backdrop
                        )
                        SensorCard(
                            icon = R.drawable.ic_humidity,
                            label = "Humidity",
                            value = humidityText,
                            shape = archShape,
                            color = secondaryContainer,
                            index = 0,
                            backdrop = backdrop
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isGlass) 10.dp else 30.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SensorCard(
                            icon = R.drawable.ic_thermostat,
                            label = "Feels like",
                            value = feelsLikeText,
                            shape = cookie4Shape,
                            color = secondaryContainer,
                            index = 0,
                            backdrop = backdrop
                        )
                        SensorCard(
                            icon = R.drawable.ic_rainy,
                            label = "Rain",
                            value = rainText,
                            shape = archShape,
                            color = secondaryContainer,
                            index = 0,
                            backdrop = backdrop
                        )
                        SensorCard(
                            icon = R.drawable.ic_cloud,
                            label = "Precipitation",
                            value = precipText,
                            shape = archShape,
                            color = secondaryContainer,
                            index = 0,
                            backdrop = backdrop
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isGlass) 10.dp else 40.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SensorCard(
                            icon = R.drawable.ic_thermostat,
                            label = "Max Temp",
                            value = maxTemp,
                            shape = cookie4Shape,
                            color = secondaryContainer,
                            index = 0,
                            backdrop = backdrop
                        )
                        SensorCard(
                            icon = R.drawable.ic_thermostat,
                            label = "Min Temp",
                            value = minTemp,
                            shape = archShape,
                            color = secondaryContainer,
                            index = 0,
                            backdrop = backdrop
                        )
                    }

                    Spacer(Modifier.height(16.dp))




                    HourlyForecastCard(
                        hours = nextHours.map { hour ->
                            HourlyForecast(
                                time = hour.time,
                                temperature = hour.temperature,
                                feelsLike = hour.feelsLike,
                                cloudCover = hour.cloudCover
                            )
                        }
                    )





                }


                locationPermissionState.status is PermissionStatus.Denied -> {
                    // Ask for location access
                    Button(
                        onClick = { locationPermissionState.launchPermissionRequest() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Location Services")
                    }
                }
                else -> {

                }
            }
        }
    }



}