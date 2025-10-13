package com.feldman.coretools.ui.components.liquid

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.feldman.coretools.MainActivity
import com.feldman.coretools.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
fun LiquidBottomBar(
    current: MainActivity.Dest,
    onSelect: (MainActivity.Dest) -> Unit,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    val items = MainActivity.Dest.entries.toTypedArray()
    val selectedIndex = items.indexOf(current)
    val shape = RoundedCornerShape(32.dp)
    val dark = isDarkTheme()

    val leftWeight by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(200),
        label = "highlightWeight"
    )
    val rightWeight = (items.size - 1).toFloat() - leftWeight

    Box(
        modifier = modifier
            .padding(horizontal = 15.dp)
            .padding(top = 20.dp, bottom = 40.dp)
            .fillMaxWidth()
            .height(90.dp)
            .drawBackdrop(
                backdrop = backdrop,
                effects = {
                    vibrancy()
                    blur(100.dp.toPx())
                    lens(12f.dp.toPx(), 60f.dp.toPx(), false)
                },
                shape = { RoundedCornerShape(40.dp) },
                onDrawSurface = {
                    drawRect(
                        if (dark) Color(0xFF000000).copy(alpha = 0.85f)
                        else Color.White.copy(alpha = 0.85f)
                    )
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 🔹 Highlight row — same parent as items, uses weights to sit in the correct slot
            Row(Modifier.matchParentSize()) {
                if (leftWeight > 0f) Spacer(Modifier.weight(leftWeight))


                val isFirst = selectedIndex == 0
                val isLast = selectedIndex == items.size - 1

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .drawBackdrop(
                            backdrop = backdrop,
                            effects =  {
                                vibrancy()
                                blur(4.dp.toPx());
                                lens(24f.dp.toPx(), 60f.dp.toPx(), true)
                            },
                            shape = { RoundedCornerShape(32.dp) },
                            onDrawSurface = {
                                drawRect(if (!dark) Color(0xFF313131).copy(alpha = 0.1f) else Color(0xFFBDBDBD).copy(alpha = 0.2f))
                            }
                        )
                )
                if (rightWeight > 0f) Spacer(Modifier.weight(rightWeight))
            }

            // 🔹 Items row — same slot model (weights), so it lines up perfectly
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { dest ->
                    val selected = current == dest
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(dest) }
                            .padding(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            painter = if (selected) painterResource(dest.filledIcon) else painterResource(dest.outlineIcon),
                            contentDescription = dest.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

    }
}