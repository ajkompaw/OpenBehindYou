package dev.openbehindyou.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.Surface
import androidx.annotation.StringRes
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dev.openbehindyou.OpenBehindYouApplication
import dev.openbehindyou.R
import dev.openbehindyou.camera.CameraAnalyzer
import dev.openbehindyou.detector.DetectionConfig
import dev.openbehindyou.detector.DetectionReport
import dev.openbehindyou.detector.MlKitFaceDetector
import dev.openbehindyou.detector.SafetyLevel
import dev.openbehindyou.detector.Sensitivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Camera foreground service.
 *
 * Android 14+ requires the camera type and FOREGROUND_SERVICE_CAMERA. Starting
 * while the app is not visible is restricted by the platform, so the service is
 * only started from a visible user action and uses START_NOT_STICKY. Once
 * started in the foreground, the camera session remains valid while the user
 * switches apps.
 */
class GuardService : LifecycleService(), CameraAnalyzer.Listener {

    @Volatile
    private var activeConfig: DetectionConfig = Sensitivity.MEDIUM.toConfig()

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var analyzer: CameraAnalyzer? = null
    private var settingsJob: Job? = null
    private var displayListener: DisplayManager.DisplayListener? = null

    @Volatile
    private var running = false

    private var foregroundStarted = false
    private var keepErrorNotification = false

    @Volatile
    private var lastStatusKey: String? = null
    private var lastDetectionLevel = SafetyLevel.SAFE
    private var lastAlertNotificationAtMs = 0L

    private val cameraSelector: CameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
        .build()

    private val resolutionSelector: ResolutionSelector = ResolutionSelector.Builder()
        .setResolutionStrategy(
            ResolutionStrategy(
                Size(640, 480),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
            ),
        )
        .build()

    override fun onCreate() {
        super.onCreate()
        GuardNotifications.createChannels(this)
        GuardRuntime.markStarting()

        foregroundStarted = startInForeground(GuardRuntime.snapshot.value)
        if (!foregroundStarted) {
            keepErrorNotification = true
            GuardRuntime.markStopped(R.string.error_foreground_service)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!foregroundStarted) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_STOP -> stopGuardAndSelf()
            ACTION_START -> startGuard()
            else -> {
                // A sticky/system restart must not open the camera from the
                // background. Guard is always explicitly user-started.
                stopGuardAndSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun startGuard() {
        if (running) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            fail(R.string.error_camera_permission)
            return
        }

        running = true
        lastDetectionLevel = SafetyLevel.SAFE
        lastAlertNotificationAtMs = 0L
        keepErrorNotification = false
        GuardRuntime.markStarting()
        GuardNotifications.cancelAlert(this)
        observeSensitivity()
        bindCamera()
    }

    private fun observeSensitivity() {
        settingsJob?.cancel()
        settingsJob = lifecycleScope.launch {
            val repository = (application as OpenBehindYouApplication).settingsRepository
            repository.sensitivity.collect { sensitivity ->
                activeConfig = sensitivity.toConfig()
                analyzer?.updateConfig(activeConfig)
            }
        }
    }

    private fun bindCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener(
            {
                if (!running) return@addListener
                try {
                    val provider = providerFuture.get()
                    if (!provider.hasCamera(cameraSelector)) {
                        fail(R.string.error_no_front_camera)
                        return@addListener
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setResolutionSelector(resolutionSelector)
                        .build()
                    analysis.targetRotation = currentDisplayRotation()

                    val newAnalyzer = CameraAnalyzer(
                        detector = MlKitFaceDetector(),
                        initialConfig = activeConfig,
                        listener = this,
                    )
                    analysis.setAnalyzer(newAnalyzer.executor, newAnalyzer)

                    cameraProvider = provider
                    imageAnalysis = analysis
                    analyzer = newAnalyzer

                    provider.unbindAll()
                    val camera = provider.bindToLifecycle(
                        this@GuardService,
                        cameraSelector,
                        analysis,
                    )
                    camera.cameraInfo.cameraState.observe(this@GuardService) { state ->
                        if (running &&
                            state.type == CameraState.Type.CLOSED &&
                            state.error != null
                        ) {
                            Log.e(TAG, "Camera closed with an error: ${state.error}")
                            fail(R.string.error_camera_runtime)
                        }
                    }
                    registerDisplayListener()

                    GuardRuntime.markRunning()
                    refreshStatusNotification(force = true)
                } catch (t: Throwable) {
                    Log.e(TAG, "Unable to bind front camera", t)
                    fail(R.string.error_camera_start)
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun startInForeground(snapshot: GuardSnapshot): Boolean {
        return try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                GuardNotifications.STATUS_ID,
                GuardNotifications.buildStatus(this, snapshot),
                type,
            )
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to enter camera foreground mode", t)
            false
        }
    }

    override fun onDetection(report: DetectionReport) {
        if (!running) return

        val previousLevel = lastDetectionLevel
        GuardRuntime.updateDetection(report)

        if (report.level == SafetyLevel.ALERT) {
            val enteredAlert = previousLevel != SafetyLevel.ALERT
            val repeatDue = lastAlertNotificationAtMs == 0L ||
                report.timestampMs - lastAlertNotificationAtMs >= ALERT_REPEAT_INTERVAL_MS
            if (enteredAlert || repeatDue) {
                lastAlertNotificationAtMs = report.timestampMs
                GuardRuntime.noteAlert()
                vibrateAlert()
                GuardNotifications.notifyAlert(this, report.reason)
            }
        }

        lastDetectionLevel = report.level
        refreshStatusNotification(force = false)
    }

    override fun onAnalyzerError(error: Throwable) {
        if (!running) return
        Log.w(TAG, "Analyzer error", error)
        GuardRuntime.setError(R.string.error_camera_runtime)
        refreshStatusNotification(force = true)
    }

    private fun refreshStatusNotification(force: Boolean) {
        val snapshot = GuardRuntime.snapshot.value
        val key = buildString {
            append(snapshot.running)
            append('|').append(snapshot.starting)
            append('|').append(snapshot.level)
            append('|').append(snapshot.reason)
            append('|').append(snapshot.primaryPresent)
            append('|').append(snapshot.errorRes)
        }
        if (!force && key == lastStatusKey) return
        lastStatusKey = key
        GuardNotifications.notifyStatus(this, snapshot)
    }

    private fun registerDisplayListener() {
        val manager = getSystemService(DisplayManager::class.java) ?: return
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == Display.DEFAULT_DISPLAY) {
                    imageAnalysis?.targetRotation = currentDisplayRotation()
                }
            }
        }
        manager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        displayListener = listener
        imageAnalysis?.targetRotation = currentDisplayRotation()
    }

    private fun unregisterDisplayListener() {
        val listener = displayListener ?: return
        getSystemService(DisplayManager::class.java)?.unregisterDisplayListener(listener)
        displayListener = null
    }

    @Suppress("DEPRECATION")
    private fun currentDisplayRotation(): Int {
        val manager = getSystemService(DisplayManager::class.java)
        return manager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
    }

    private fun vibrateAlert() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 70L, 55L, 140L),
                    -1,
                ),
            )
        }
    }

    private fun fail(@StringRes errorRes: Int) {
        Log.e(TAG, "Guard failure: ${getString(errorRes)}")
        running = false
        settingsJob?.cancel()
        stopCamera()
        GuardRuntime.markStopped(errorRes)
        keepErrorNotification = true
        lastStatusKey = null
        GuardNotifications.cancelAlert(this)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        GuardNotifications.notifyStatus(this, GuardRuntime.snapshot.value)
        stopSelf()
    }

    private fun stopGuardAndSelf() {
        running = false
        settingsJob?.cancel()
        stopCamera()
        GuardRuntime.markStopped()
        keepErrorNotification = false
        lastStatusKey = null
        GuardNotifications.cancelAll(this)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopCamera() {
        unregisterDisplayListener()
        analyzer?.close()
        analyzer = null

        imageAnalysis?.clearAnalyzer()
        val provider = cameraProvider
        val analysis = imageAnalysis
        if (provider != null && analysis != null) {
            runCatching { provider.unbind(analysis) }
                .onFailure { Log.w(TAG, "Unable to unbind analysis", it) }
        } else {
            runCatching { provider?.unbindAll() }
                .onFailure { Log.w(TAG, "Unable to unbind camera", it) }
        }
        imageAnalysis = null
        cameraProvider = null
    }

    override fun onDestroy() {
        running = false
        settingsJob?.cancel()
        stopCamera()
        GuardRuntime.markStopped(GuardRuntime.snapshot.value.errorRes)
        if (!keepErrorNotification) {
            GuardNotifications.cancelAll(this)
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "GuardService"
        const val ACTION_START = "dev.openbehindyou.action.START_GUARD"
        const val ACTION_STOP = "dev.openbehindyou.action.STOP_GUARD"
        private const val ALERT_REPEAT_INTERVAL_MS = 15_000L
    }
}
