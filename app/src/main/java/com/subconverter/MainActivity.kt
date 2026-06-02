package com.subconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.subconverter.ui.MainScreen
import com.subconverter.ui.MainViewModel
import com.subconverter.ui.theme.SubConverterTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as SubConverterApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SubConverterTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
