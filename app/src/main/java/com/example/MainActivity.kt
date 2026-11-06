package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.AlHadithiAppRoot
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Initialize the shared Media3 player and index verified user-downloaded audio
        com.example.media.AudioPlayerController.init(applicationContext)

        // STEP 3: Enable Edge-to-Edge rendering and launch direct-to-Home Compose root
        enableEdgeToEdge()
        setContent {
            AlHadithiAppRoot()
        }
    }
}
