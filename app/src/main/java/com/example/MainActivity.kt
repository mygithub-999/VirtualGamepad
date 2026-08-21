package com.example

import android.annotation.SuppressLint
import android.Manifest
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var gamepadManager by mutableStateOf<GamepadManager?>(null)

    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                initializeGamepad()
            } else {
                // Fallback to IP control
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on while playing the virtual gamepad
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Immersive full-screen mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        enableEdgeToEdge()

        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            initializeGamepad()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    @SuppressLint("NewApi")
                    val connectionState = gamepadManager?.connectionState?.collectAsState(initial = BluetoothProfile.STATE_DISCONNECTED)
                    val isConnected = connectionState?.value == BluetoothProfile.STATE_CONNECTED
                    
                    Box(modifier = Modifier.padding(innerPadding)) {
                        GamepadScreen(
                            gamepadManager = gamepadManager,
                            isConnected = isConnected,
                            onMakeDiscoverable = {
                                val discoverableIntent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                    putExtra(android.bluetooth.BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                                }
                                startActivity(discoverableIntent)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun initializeGamepad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            gamepadManager = GamepadManager(this)
        }
    }

    @SuppressLint("NewApi")
    override fun onDestroy() {
        super.onDestroy()
    }
}
