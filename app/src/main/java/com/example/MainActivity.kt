package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.NovaPilotApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.NovaPilotViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Instantiate the centralized NovaPilotViewModel
        val viewModel = ViewModelProvider(this)[NovaPilotViewModel::class.java]
        
        setContent {
            MyApplicationTheme {
                NovaPilotApp(viewModel = viewModel)
            }
        }
    }
}
