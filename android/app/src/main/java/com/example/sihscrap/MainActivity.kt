package com.example.sihscrap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sihscrap.data.sync.SyncWorker
import com.example.sihscrap.theme.SIHScrapTheme
import com.example.sihscrap.ui.screens.*
import com.example.sihscrap.voice.VoiceEngine
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sihscrap.ui.SharedViewModel



class MainActivity : ComponentActivity() {
    private lateinit var voiceEngine: VoiceEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        




        voiceEngine = VoiceEngine(this)

        // Request necessary permissions on startup
        requestPermissionsIfNeeded()

        // Initialize WorkManager low-bandwidth sync engine
        SyncWorker.schedulePeriodicSync(this)

        setContent {
            SIHScrapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {




                    val navController = rememberNavController()
                    val sharedViewModel: SharedViewModel = viewModel()
                    
                    NavHost(navController = navController, startDestination = "role_selection") {
                        composable("role_selection") {
                            RoleSelectionScreen(
                                onCollectorClick = { navController.navigate("dashboard") },
                                onRecyclerClick = { navController.navigate("recycler_dashboard") },
                                voiceEngine = voiceEngine
                            )
                        }
                        composable("recycler_dashboard") {
                            RecyclerDashboardScreen(onBack = { navController.popBackStack() })
                        }
                        composable("dashboard") {
                            DashboardScreen(navController, voiceEngine)
                        }
                        composable("camera") {
                            CameraScreen(navController, sharedViewModel)
                        }
                        composable("calculator") {
                            ValuationScreen(
                                navController = navController,
                                voiceEngine = voiceEngine,
                                sharedViewModel = sharedViewModel
                            )
                        }
                        composable("voice") {
                            VoiceScreen(navController, voiceEngine)
                        }
                        composable("locator") {
                            LocatorScreen(navController)
                        }
                        composable("history") {
                            HistorySyncScreen(navController)
                        }
                        composable("duress_calc") {
                            DuressCalculatorScreen(navController)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine.shutdown()
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()
        val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                100
            )
        }
    }
}
