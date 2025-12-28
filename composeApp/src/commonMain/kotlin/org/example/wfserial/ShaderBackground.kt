package org.example.wfserial

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ShaderBackground(
    modifier: Modifier = Modifier,
    shaderCode: String = DefaultShader
)

fun processShaderCode(code: String): String {
    if (code.isBlank()) return DefaultShader
    
    // 检查是否包含 Shadertoy 的 mainImage
    val hasMainImage = code.contains("mainImage")
    // 检查是否包含 AGSL 的 main
    val hasMain = code.contains("vec4 main")
    
    var processed = code
    
    // 自动补全 Uniform 声明
    val uniforms = mutableListOf<String>()
    if (!code.contains("uniform float2 iResolution") && !code.contains("uniform vec2 iResolution")) {
        uniforms.add("uniform float2 iResolution;")
    }
    if (!code.contains("uniform float iTime")) {
        uniforms.add("uniform float iTime;")
    }
    
    if (uniforms.isNotEmpty()) {
        processed = uniforms.joinToString("\n") + "\n" + processed
    }
    
    // 如果只有 mainImage 没有 main，则自动包装
    if (hasMainImage && !hasMain) {
        processed += """
            
            vec4 main(in vec2 fragCoord) {
                vec4 fragColor;
                mainImage(fragColor, fragCoord);
                return fragColor;
            }
        """.trimIndent()
    }
    
    return processed
}

val DefaultShader = """
    uniform float2 iResolution;
    uniform float iTime;

    vec4 main(in vec2 fragCoord) {
        vec2 uv = fragCoord / iResolution.xy;
        float time = iTime * 0.2;
        
        // Animated plasma effect
        float x = uv.x * 2.0 - 1.0;
        float y = uv.y * 2.0 - 1.0;
        
        float mov0 = x + y + cos(sin(time) * 2.0) * 2.0 + sin(x + time);
        float mov1 = y / 0.9 +  time;
        float mov2 = x / 0.2;
        
        float c1 = abs(sin(mov1 + time)/2.0 + mov2/2.0 - mov1 - mov2 + time);
        float c2 = abs(sin(c1 + sin(mov0/1000.0 + time) + sin(y/40.0 + time) + sin((x+y)/100.0) * 3.0));
        float c3 = abs(sin(c2 + cos(mov1 + mov2 + c2) + cos(mov2) + sin(x/1000.0)));
        
        // Deep purple/blue theme for dark mode
        vec3 color1 = vec3(0.1, 0.0, 0.2); // Dark Purple
        vec3 color2 = vec3(0.0, 0.1, 0.3); // Deep Blue
        vec3 color3 = vec3(0.2, 0.0, 0.4); // Magenta-ish
        
        vec3 finalColor = mix(color1, color2, c1);
        finalColor = mix(finalColor, color3, c2);
        finalColor += c3 * 0.15; // Glow effect
        
        return vec4(finalColor * 0.6, 1.0); // Slightly dimmed for readability
    }
""".trimIndent()

