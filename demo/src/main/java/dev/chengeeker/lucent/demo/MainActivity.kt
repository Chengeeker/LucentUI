package dev.chengeeker.lucent.demo

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chengeeker.lucent.*
import dev.chengeeker.lucent.demo.data.DemoModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: DemoModel = viewModel()
            val style = model.style()
            LucentTheme(
                style.copy(
                    motion =
                        style.motion &&
                            Settings.Global.getFloat(
                                contentResolver,
                                Settings.Global.ANIMATOR_DURATION_SCALE,
                                1f,
                            ) > 0f
                )
            ) {
                DemoApp(model)
            }
        }
    }
}
