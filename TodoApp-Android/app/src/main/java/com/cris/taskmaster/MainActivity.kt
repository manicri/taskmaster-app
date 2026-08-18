package com.cris.taskmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.cris.taskmaster.ui.AuthScreen
import com.cris.taskmaster.ui.HomeScreen
import com.cris.taskmaster.ui.theme.TaskMasterTheme
import com.cris.taskmaster.ui.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskMasterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUser by viewModel.currentUser.collectAsState()
                    if (currentUser == null) {
                        AuthScreen(viewModel = viewModel)
                    } else {
                        HomeScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
