package com.dev.mandadito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dev.mandadito.presentation.navigation.AppNavigation
import com.dev.mandadito.ui.theme.MandaditoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MandaditoTheme {
                AppNavigation()
            }
        }
    }
}



