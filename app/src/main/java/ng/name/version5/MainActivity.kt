package ng.name.version5

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import ng.name.version5.data.AppServices
import ng.name.version5.data.LocalServices
import ng.name.version5.ui.KidsApp
import ng.name.version5.ui.theme.Version5Theme

class MainActivity : ComponentActivity() {
    private lateinit var services: AppServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The game has a dark night-sky background, so use light status-bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        services = AppServices(applicationContext)
        setContent {
            Version5Theme {
                CompositionLocalProvider(LocalServices provides services) {
                    KidsApp()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) services.sfx.release()
    }
}
