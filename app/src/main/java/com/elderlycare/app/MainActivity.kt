package com.elderlycare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.elderlycare.app.navigation.AppNavGraph
import com.elderlycare.app.ui.theme.ElderlyCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ElderlyCareTheme {
                AppNavGraph()
            }
        }
    }
}
