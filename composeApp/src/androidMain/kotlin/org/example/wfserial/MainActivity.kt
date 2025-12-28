package org.example.wfserial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidStorage.context = applicationContext
        setContent {
            val viewModel = remember { SerializerViewModel() }
            App(viewModel)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val viewModel = remember { SerializerViewModel() }
    App(viewModel)
}
