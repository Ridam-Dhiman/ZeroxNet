package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.MainViewModel
import com.example.ui.chat.ChatScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.sos.SosAlertScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PermissionHelper

sealed class Screen {
    object Onboarding : Screen()
    object Chat : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val grantedAll = results.values.all { it }
        if (grantedAll) {
            Toast.makeText(this, "Permissions granted. Mesh network enabled.", Toast.LENGTH_SHORT).show()
            viewModel.startService()
        } else {
            Toast.makeText(this, "Mesh networking requires Location & Bluetooth permissions to route offline signals.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Check and request runtime permissions on launch
        if (!PermissionHelper.hasAllPermissions(this)) {
            requestPermissionLauncher.launch(PermissionHelper.getRequiredPermissions().toTypedArray())
        } else {
            viewModel.startService()
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
                    val displayNameState by viewModel.displayName.collectAsState()

                    LaunchedEffect(displayNameState) {
                        if (displayNameState.isNotEmpty()) {
                            if (currentScreen == Screen.Onboarding) {
                                currentScreen = Screen.Chat
                            }
                        } else {
                            currentScreen = Screen.Onboarding
                        }
                    }

                    // Global SOS Alert Overlay popup
                    SosAlertScreen(viewModel = viewModel)

                    Crossfade(targetState = currentScreen, label = "screen_routing") { screen ->
                        when (screen) {
                            is Screen.Onboarding -> {
                                OnboardingScreen(
                                    viewModel = viewModel,
                                    onOnboarded = { currentScreen = Screen.Chat }
                                )
                            }
                            is Screen.Chat -> {
                                ChatScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { currentScreen = Screen.Settings }
                                )
                            }
                            is Screen.Settings -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = Screen.Chat }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
