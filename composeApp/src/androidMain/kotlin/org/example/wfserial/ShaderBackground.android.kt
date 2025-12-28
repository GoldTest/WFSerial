package org.example.wfserial

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.animation.core.*

@Composable
actual fun ShaderBackground(
    modifier: Modifier,
    shaderCode: String
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val infiniteTransition = rememberInfiniteTransition()
        val time by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(100000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )

        val shader = remember(shaderCode) {
            RuntimeShader(shaderCode)
        }

        Canvas(modifier = modifier.fillMaxSize()) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", time)
            drawRect(brush = ShaderBrush(shader))
        }
    } else {
        // Fallback for older Android versions
        androidx.compose.foundation.background(
            androidx.compose.ui.graphics.Color(0xFF121212)
        ).let { /* Do nothing, already handled by parent surface usually */ }
    }
}
