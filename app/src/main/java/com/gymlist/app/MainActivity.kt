package com.gymlist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gymlist.app.ui.GymListApp
import com.gymlist.app.ui.theme.GymListTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repository = (application as GymListApplication).container.repository

        setContent {
            GymListTheme {
                GymListApp(repository = repository)
            }
        }
    }
}
