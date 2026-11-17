package dev.openbehindyou.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import dev.openbehindyou.R

/** Small entry point shared by Compose and notification actions. */
object GuardController {
    private const val TAG = "GuardController"

    fun start(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            GuardRuntime.markStopped(R.string.error_camera_permission)
            return
        }

        GuardRuntime.markStarting()
        val intent = Intent(context, GuardService::class.java).setAction(GuardService.ACTION_START)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to start camera foreground service", t)
            GuardRuntime.markStopped(R.string.error_foreground_service)
        }
    }

    fun stop(context: Context) {
        GuardRuntime.markStopped()
        try {
            context.stopService(Intent(context, GuardService::class.java))
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to stop guard service", t)
        }
        GuardNotifications.cancelAll(context)
    }
}
