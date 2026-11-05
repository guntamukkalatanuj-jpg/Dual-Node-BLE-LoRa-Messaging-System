package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BluLinqViewModel
import com.example.ui.screens.MainContainer
import com.example.ui.theme.BluLinqTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluLinqTheme {
                val viewModel: BluLinqViewModel = viewModel()
                MainContainer(viewModel = viewModel)
            }
        }
    }
}
