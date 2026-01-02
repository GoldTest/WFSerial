package org.example.wfserial

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
actual fun rememberPlatformConfiguration(): PlatformConfig = PlatformConfig(
    cardWidth = 400.dp,
    cardHeight = 640.dp,
    swipeThreshold = 180f
)
