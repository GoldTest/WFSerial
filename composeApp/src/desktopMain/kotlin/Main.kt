import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.wfserial.App

fun main() = application {
    val state = rememberWindowState(
        width = 500.dp,
        height = 900.dp
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = "WFSerial - 序列化器"
    ) {
        App()
    }
}
