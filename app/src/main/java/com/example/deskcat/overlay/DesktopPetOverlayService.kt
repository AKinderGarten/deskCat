package com.example.deskcat.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager

class DesktopPetOverlayService : Service() {
    private var overlayView: DesktopPetOverlayView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        OverlayNotificationFactory.ensureChannel(this)
        startForeground(
            OverlayNotificationFactory.NOTIFICATION_ID,
            OverlayNotificationFactory.createNotification(this),
        )

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = DesktopPetOverlayView(this, windowManager)
        overlayView?.show()
    }

    override fun onDestroy() {
        overlayView?.remove()
        overlayView = null
        super.onDestroy()
    }
}
