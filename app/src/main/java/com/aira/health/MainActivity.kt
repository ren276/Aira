package com.aira.health

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply FLAG_SECURE on sensitive builds to prevent screenshots/screen recording
        if (BuildConfig.ENABLE_FLAG_SECURE) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        enableEdgeToEdge()
        setContent {
            // AiraNavHost will be wired in Phase 4
            // Placeholder — theme + nav graph added in 04-01-PLAN
        }
    }
}
