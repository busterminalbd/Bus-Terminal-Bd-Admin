package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.FoodBillViewModel
import com.example.ui.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

  private val foodBillViewModel: FoodBillViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val themeMode by foodBillViewModel.themeMode.collectAsStateWithLifecycle()
      val themeColor by foodBillViewModel.themeColor.collectAsStateWithLifecycle()
      MyApplicationTheme(themeMode = themeMode, themeColor = themeColor) {
        HomeScreen(viewModel = foodBillViewModel)
      }
    }
  }
}

