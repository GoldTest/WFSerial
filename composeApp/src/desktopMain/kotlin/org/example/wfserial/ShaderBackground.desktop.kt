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
        val processedCode = processShaderCode(shaderCode)
        try {
            RuntimeEffect.makeForShader(processedCode)
        } catch (e: Exception) {
            println("Shader compilation failed: ${e.message}")
            try {
                RuntimeEffect.makeForShader(processShaderCode(DefaultShader))
            } catch (e2: Exception) {
                null
            }
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        if (runtimeEffect != null) {
            val shaderBuilder = RuntimeShaderBuilder(runtimeEffect)
            shaderBuilder.uniform("iResolution", width, height)
            shaderBuilder.uniform("iTime", time)
            
            val shader = shaderBuilder.makeShader()
            
            drawContext.canvas.nativeCanvas.drawPaint(
                org.jetbrains.skia.Paint().apply {
                    this.shader = shader
                }
            )
        } else {
            // Fallback to solid color if everything fails
            drawRect(color = androidx.compose.ui.graphics.Color(0xFF121212))
        }
    }
}
