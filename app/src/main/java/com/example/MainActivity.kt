package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.example.ui.AlHadithiAppRoot
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the activity caption empty; the product identity belongs inside the Compose UI.
        title = ""

        // STEP 1: Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Initialize the shared Media3 player and index verified user-downloaded audio
        com.example.media.AudioPlayerController.init(applicationContext)

        // STEP 3: Draw edge-to-edge so the Compose surface owns the whole window.
        //
        // The black rectangle at the top of the screen was caused by the platform window
        // background (pure black from the DeviceDefault theme) showing through while the app
        // only drew below the status bar inset. `decorFitsSystemWindows = false` plus the
        // transparent system bars declared in themes.xml make the app background continuous
        // from the very top of the display, including behind a notch or punch-hole.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            AlHadithiAppRoot()
        }
    }
}
