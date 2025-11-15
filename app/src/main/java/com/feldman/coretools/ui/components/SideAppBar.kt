package com.feldman.coretools.ui.components

import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feldman.coretools.Dest
import com.feldman.coretools.DestBackStack
import com.feldman.coretools.topLevel
import com.feldman.coretools.ui.pages.scaledHeight
import com.feldman.coretools.ui.pages.scaledWidth

@Composable
fun SideAppBar(
    backStack: DestBackStack,
    onNavigate: (Dest) -> Unit,
    modifier: Modifier = Modifier
) {
    val railWidth = scaledWidth(92.dp)
    val railInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)

    val current = backStack.currentTop

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .windowInsetsPadding(railInsets)
            .fillMaxHeight()
            .width(railWidth)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            topLevel.forEach { dest ->

                val isSelected = current == dest || current.parent == dest

                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) onNavigate(dest)
                    },
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (isSelected) dest.filledIcon else dest.outlineIcon
                            ),
                            contentDescription = dest.label,
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(scaledHeight(30.dp))
                        )
                    },
                    label = {
                        Text(
                            text = dest.label,
                            fontSize = scaledWidth(13.sp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    },
                    alwaysShowLabel = true
                )
            }
        }
    }
}
