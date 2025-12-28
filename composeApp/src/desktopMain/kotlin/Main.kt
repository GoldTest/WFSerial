import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import org.example.wfserial.SerializerViewModel

fun main() = application {
    val viewModel = remember { SerializerViewModel() }
    var isOpen by remember { mutableStateOf(true) }
    val icon = remember {
        useResource("drawable/icon.svg") { 
            // Simplified for now, in a real project you'd use a PNG or convert SVG
            // For Desktop Compose, we'll try to load a painter.
            // Since we don't have a converter tool, I'll use a placeholder or assume the user might provide one.
            // But let's try to load what we have or just use default.
            null
        }
    }

    val state = rememberWindowState(
        width = 800.dp,
        height = 1300.dp,
        position = WindowPosition(Alignment.Center)
    )

    if (isOpen) {
        Window(
            onCloseRequest = { isOpen = false },
            state = state,
            title = "WFSerial",
            resizable = true
        ) {
            org.example.wfserial.App(viewModel)
        }
    }

    Tray(
        icon = TrayIcon,
        onAction = { isOpen = true }, // Double-click to open
        menu = {
            Item("Show", onClick = { isOpen = true })
            Item("Exit", onClick = { exitApplication() })
        }
    )
}

// Fallback Icon for demo purposes
object TrayIcon : androidx.compose.ui.graphics.painter.Painter() {
    override val intrinsicSize = androidx.compose.ui.geometry.Size(256f, 256f)
    override fun androidx.compose.ui.graphics.drawscope.DrawScope.onDraw() {
        drawCircle(androidx.compose.ui.graphics.Color(0xFF2196F3))
    }
}
