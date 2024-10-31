package com.risyan.quickshutdownphone.feature

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.risyan.quickshutdownphone.base.ui.theme.QuickShutdownPhoneTheme
import com.risyan.quickshutdownphone.feature.navigator.OnboardNavigationHost

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickShutdownPhoneTheme {
                navController = rememberNavController()
                OnboardNavigationHost(navController)
            }
        }
    }
}
