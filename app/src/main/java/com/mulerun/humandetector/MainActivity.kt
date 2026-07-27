package com.mulerun.humandetector

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mulerun.humandetector.ui.screens.RadarScreen
import com.mulerun.humandetector.ui.screens.SettingsScreen
import com.mulerun.humandetector.ui.theme.HdTheme
import com.mulerun.humandetector.viewmodel.RadarViewModel

class MainActivity : ComponentActivity() {

    private val vm: RadarViewModel by viewModels()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refreshCapabilities() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()

        setContent {
            HdTheme(nightMode = true) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    var showSettings by remember { mutableStateOf(false) }
                    if (showSettings) {
                        SettingsScreen(vm) { showSettings = false }
                    } else {
                        RadarScreen(vm) { showSettings = true }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshCapabilities()
    }

    private fun requestNeededPermissions() {
        val perms = mutableListOf<String>()
        perms += Manifest.permission.CAMERA
        if (Build.VERSION.SDK_INT >= 31) {
            perms += Manifest.permission.BLUETOOTH_SCAN
            perms += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            perms += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= 33) {
            perms += Manifest.permission.NEARBY_WIFI_DEVICES
            perms += Manifest.permission.UWB_RANGING
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        permLauncher.launch(perms.toTypedArray())
    }
}
