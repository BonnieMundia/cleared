package app.cleared

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.cleared.ui.ClearedApp
import app.cleared.ui.theme.ClearedTheme

/** The single Activity. Everything above it is Compose. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ClearedTheme {
                ClearedApp()
            }
        }
    }
}
