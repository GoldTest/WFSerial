package org.example.wfserial

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
actual fun rememberPlatformConfiguration(): PlatformConfig = PlatformConfig(
    cardWidth = 320.dp,
    cardHeight = 520.dp,
    swipeThreshold = 250f
)
