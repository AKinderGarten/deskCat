package com.example.deskcat

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.deskcat.overlay.DesktopPetOverlayService
import com.example.deskcat.overlay.OverlayPermissionHelper
import com.example.deskcat.ui.theme.DeskCatTheme

class MainActivity : ComponentActivity() {
    private var overlayRunningState by mutableStateOf(false)
    private var overlayGrantedState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayGrantedState = OverlayPermissionHelper.canDrawOverlays(this)

        setContent {
            DeskCatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    BakingScreen(
                        overlayGranted = overlayGrantedState,
                        overlayRunning = overlayRunningState,
                        onOpenOverlayPermission = {
                            startActivity(OverlayPermissionHelper.createManageOverlayPermissionIntent(this))
                        },
                        onStartOverlay = {
                            if (!OverlayPermissionHelper.canDrawOverlays(this)) {
                                overlayGrantedState = false
                                startActivity(OverlayPermissionHelper.createManageOverlayPermissionIntent(this))
                            } else {
                                overlayGrantedState = true
                                val intent = Intent(this, DesktopPetOverlayService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(intent)
                                } else {
                                    startService(intent)
                                }
                                overlayRunningState = true
                            }
                        },
                        onStopOverlay = {
                            stopService(Intent(this, DesktopPetOverlayService::class.java))
                            overlayRunningState = false
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGrantedState = OverlayPermissionHelper.canDrawOverlays(this)
    }
}
