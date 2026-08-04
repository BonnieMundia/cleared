package app.cleared

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The single Activity. Compose Navigation hangs off this once there are destinations to host —
 * scaffolding only for now, so no screens and no nav graph.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // Placeholder theme. Replaced by the real Color/Type/Theme in step 2.
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    content = {}
                )
            }
        }
    }
}
