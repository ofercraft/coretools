package com.feldman.coretools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
fun SettingCard(
    index: Int,
    totalCount: Int,
    isGlass: Boolean,
    backdrop: Backdrop,
    dark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = when {
        isGlass -> RoundedCornerShape(28.dp)
        index == 0 && totalCount == 1 -> RoundedCornerShape(28.dp) // only one item
        index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        index == totalCount - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(6.dp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                        shape = { shape },
                        onDrawSurface = {
                            drawRect(
                                if (dark)
                                    Color(0xFF313131).copy(alpha = 0.2f)
                                else
                                    Color(0xFFBDBDBD).copy(alpha = 0.2f)
                            )
                        }
                    )
                } else {
                    Modifier.background(
                        color = if (dark)
                            Color(0xFF1E1E1E)
                        else
                            Color.White.copy(alpha = 0.7f),
                        shape = shape
                    )
                }
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        content()
    }
}
