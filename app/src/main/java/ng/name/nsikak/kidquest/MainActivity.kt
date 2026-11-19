package ng.name.nsikak.kidquest

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import ng.name.nsikak.kidquest.data.AppServices
import ng.name.nsikak.kidquest.data.LocalServices
import ng.name.nsikak.kidquest.ui.KidsApp
import ng.name.nsikak.kidquest.ui.theme.KidQuestTheme

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
            KidQuestTheme {
                CompositionLocalProvider(LocalServices provides services) {
                    KidsApp()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        services.speaker.stop() // never keep talking in the background
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) services.release()
    }
}
