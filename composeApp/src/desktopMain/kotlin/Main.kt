import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import org.example.wfserial.SerializerViewModel

fun main() = application {
    val viewModel = remember { SerializerViewModel() }
    var isOpen by remember { mutableStateOf(true) }
    
    val appIcon = painterResource("tray_icon.png")

    val state = rememberWindowState(
        width = 800.dp,
        height = 1300.dp,
        position = WindowPosition(Alignment.Center)
    )

    if (isOpen) {
        Window(
            onCloseRequest = { isOpen = false },
            state = state,
            icon = appIcon,
            title = "WFSerial",
            resizable = true
        ) {
            org.example.wfserial.App(viewModel)
        }
    }

    Tray(
        icon = appIcon,
        tooltip = "WFSerial",
        onAction = { isOpen = true },
        menu = {
            Item("Show", onClick = { isOpen = true })
            Separator()
            Item("Exit", onClick = { exitApplication() })
        }
    )
}
