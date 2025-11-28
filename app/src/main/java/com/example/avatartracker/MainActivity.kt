package com.example.avatartracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.avatartracker.ui.theme.AvatarGoalTrackerTheme
import com.example.avatartracker.MainNavScreen
import com.example.avatartracker.SoundManager
// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
// If your theme function is named differently (check ui/theme/Theme.kt),
// change this import and the call below to match it.

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SoundManager.init(applicationContext)  //Add this

        enableEdgeToEdge()
        setContent {
            AvatarGoalTrackerTheme {        // <- or whatever your theme is named
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavScreen()            // NO parameters now
                }
            }
        }
    }
}


fun sampleTasks(): List<Task> {
    return listOf(
        Task(
            title = "15 min walk",
            category = Category.FITNESS,
            difficulty = Difficulty.EASY
        ),
        Task(
            title = "Read 10 pages",
            category = Category.LEARNING,
            difficulty = Difficulty.MEDIUM
        ),
        Task(
            title = "Plan tomorrow's schedule",
            category = Category.DISCIPLINE,
            difficulty = Difficulty.MEDIUM
        )
    )
}


