package com.sonny.heartymusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sonny.heartymusic.ui.navigation.HeartyMainScreen
import com.sonny.heartymusic.ui.theme.HeartyMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeartyMusicTheme {
                HeartyMainScreen()
            }
        }
    }
}
