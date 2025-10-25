package com.feldman.coretools.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feldman.coretools.R
import com.feldman.coretools.storage.AppStyle
import com.feldman.coretools.storage.appStyleFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HourlyForecast(
    val time: String,
    val temperature: Int,
    val feelsLike: Int,
    val cloudCover: Int,
    val rain: Double = 0.0,
    val snow: Double = 0.0
)
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HourlyWeatherIcon(hour: HourlyForecast) {
    when {
        hour.snow > 0.0 -> {
            Image(
                painter = painterResource(R.drawable.ic_snow),
                contentDescription = "Snow",
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(Color(0xFF80DEEA))
            )
        }
        hour.rain > 0.0 -> {
            Image(
                painter = painterResource(R.drawable.ic_rainy),
                contentDescription = "Rain",
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(Color(0xFF2196F3))
            )
        }
        hour.cloudCover < 20 -> {
//            Box(
//                modifier = Modifier
//                    .size(32.dp)
//                    .background(
//                        color = Color(0xFFFFD54F),
//                        shape = MaterialShapes.Sunny.toShape()
//                    )
//            )
            Image(
                painter = painterResource(R.drawable.ic_sun),
                contentDescription = "Sunny",
                modifier = Modifier.size(32.dp),
            )
        }
        hour.cloudCover < 70 -> {
            Image(
                painter = painterResource(R.drawable.ic_partly_cloudy),
                contentDescription = "Partly Cloudy",
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(Color(0xFF90CAF9))
            )
        }
        else -> {
            Image(
                painter = painterResource(R.drawable.ic_cloud),
                contentDescription = "Cloudy",
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(Color(0xFFB0BEC5))
            )
        }
    }
}
@Composable
fun HourlyForecastCard(
    hours: List<HourlyForecast>
) {
    val context = LocalContext.current

    val currentHour = remember {
        SimpleDateFormat("HH", Locale.getDefault()).format(Date()).toInt()
    }

    val filteredHours = remember(hours) {
        hours.filter { hour ->
            // Handle "HH:mm" or "HH" format gracefully
            val hourInt = hour.time.substringBefore(":").toIntOrNull() ?: 0
            hourInt >= currentHour
        }.mapIndexed { index, hour ->
            if (index == 0) hour.copy(time = "Now") else hour
        }
    }
    val appStyle by context.appStyleFlow().collectAsState(initial = AppStyle.Material)
    val isGlass = appStyle == AppStyle.Glass

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                if (isGlass) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(32.dp)
            ),
    ) {
        Text(
            text = "Next Hours",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredHours) { hour ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(80.dp)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {

                    Text(
                        text = "${hour.temperature}°",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HourlyWeatherIcon(hour)

                    Text(
                        text = hour.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                }
            }
        }
    }
}

@Composable
fun HourlyWeatherEmojiIcon(hour: HourlyForecast) {
    val emoji = when {
        hour.snow > 0.0 -> "❄️"
        hour.rain > 0.0 -> "🌧️"
        hour.cloudCover < 20 -> "☀️"
        hour.cloudCover < 70 -> "⛅"
        else -> "☁️"
    }

    Text(
        text = emoji,
        fontSize = 28.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.size(40.dp),
    )
}
