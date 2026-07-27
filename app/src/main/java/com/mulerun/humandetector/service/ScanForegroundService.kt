package com.mulerun.humandetector.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mulerun.humandetector.MainActivity
import com.mulerun.humandetector.R

/**
 * Optional foreground service that keeps BLE / Wi-Fi Aware scanning alive when the app is
 * backgrounded. The concrete scanners still live in RadarViewModel; the service just holds
 * the process alive with an ongoing notification.
 */
class ScanForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif: Notification = NotificationCompat.Builder(this, getString(R.string.notif_channel_id))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_scanning))
            .setOngoing(true)
            .setContentIntent(tap)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
        return START_STICKY
    }

    companion object {
        const val NOTIF_ID = 42
        fun start(ctx: Context) { ctx.startForegroundService(Intent(ctx, ScanForegroundService::class.java)) }
        fun stop(ctx: Context)  { ctx.stopService(Intent(ctx, ScanForegroundService::class.java)) }
    }
}
