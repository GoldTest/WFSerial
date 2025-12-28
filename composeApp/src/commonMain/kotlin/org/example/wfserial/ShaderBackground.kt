package org.example.wfserial

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ShaderBackground(
    modifier: Modifier = Modifier,
    shaderCode: String = DefaultShader
)

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

