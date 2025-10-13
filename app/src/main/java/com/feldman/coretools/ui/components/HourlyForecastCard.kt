package com.feldman.coretools.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.feldman.coretools.R

data class HourlyForecast(
    val time: String,
    val temperature: Int,
    val feelsLike: Int,
    val cloudCover: Int
)

@Composable
fun HourlyForecastCard(
    hours: List<HourlyForecast>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(32.dp)),
    ) {
        Text(
            text = "Next Hours",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp)
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)

        ) {
            items(hours) { hour ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(80.dp).padding(16.dp)
                ) {
                    Text(hour.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    Image(
                        painter = painterResource(
                            when {
                                hour.cloudCover < 20 -> R.drawable.ic_sunny
                                hour.cloudCover < 70 -> R.drawable.ic_partly_cloudy
                                else -> R.drawable.ic_cloud
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        colorFilter = ColorFilter.tint(
                            when {
                                hour.cloudCover < 20 -> Color(0xFFFFD54F) // bright yellow sun
                                hour.cloudCover < 70 -> Color(0xFF90CAF9) // light blue partly cloudy
                                else -> Color(0xFFB0BEC5) // grey clouds
                            }
                        )
                    )
                    Text("${hour.temperature}°", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
