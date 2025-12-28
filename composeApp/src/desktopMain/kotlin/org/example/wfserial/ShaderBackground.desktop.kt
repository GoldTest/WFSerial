package org.example.wfserial

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

@Composable
actual fun ShaderBackground(
    modifier: Modifier,
    shaderCode: String
) {
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val runtimeEffect = remember(shaderCode) {
        RuntimeEffect.makeForShader(shaderCode)
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val shaderBuilder = RuntimeShaderBuilder(runtimeEffect)
        shaderBuilder.uniform("iResolution", width, height)
        shaderBuilder.uniform("iTime", time)
        
        val shader = shaderBuilder.makeShader()
        
        drawContext.canvas.nativeCanvas.drawPaint(
            org.jetbrains.skia.Paint().apply {
                this.shader = shader
            }
        )
    }
}
