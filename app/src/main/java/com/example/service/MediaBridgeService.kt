package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.WaveBridgeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MediaBridgeService : Service() {

  companion object {
    const val ACTION_START_SERVER = "com.example.action.START_SERVER"
    const val ACTION_STOP_SERVER = "com.example.action.STOP_SERVER"
    const val ACTION_START_AUDIO = "com.example.action.START_AUDIO"
    const val ACTION_STOP_AUDIO = "com.example.action.STOP_AUDIO"

    const val EXTRA_PORT = "extra_port"
    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_RESULT_DATA = "extra_result_data"

    private const val CHANNEL_ID = "wavebridge_server_channel"
    private const val NOTIFICATION_ID = 4107
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var stateObserverJob: Job? = null
  private var wakeLock: PowerManager.WakeLock? = null
  private var wifiLock: WifiManager.WifiLock? = null

  private val appContainer by lazy {
    (applicationContext as WaveBridgeApplication).container
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action ?: ACTION_START_SERVER
    val port = intent?.getIntExtra(EXTRA_PORT, appContainer.bridgeServer.currentPort.value)
      ?: appContainer.bridgeServer.currentPort.value

    when (action) {
      ACTION_START_SERVER -> {
        promoteToForeground(includeMediaProjection = appContainer.audioEngine.isCapturing.value)
        acquireLocks()
        appContainer.mediaMonitor.startMonitoring()
        appContainer.bridgeServer.start(port)
        observeForNotificationUpdates()
      }

      ACTION_START_AUDIO -> {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
          ?: Activity.RESULT_CANCELED
        val resultData = intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)

        if (resultCode == Activity.RESULT_OK && resultData != null) {
          // Android 14-17 MANDATORY ORDER: startForeground with MEDIA_PROJECTION BEFORE getMediaProjection()
          promoteToForeground(includeMediaProjection = true)
          acquireLocks()
          appContainer.mediaMonitor.startMonitoring()
          if (!appContainer.bridgeServer.isRunning.value) {
            appContainer.bridgeServer.start(port)
          }
          val projMgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
          val projection = projMgr.getMediaProjection(resultCode, resultData)
          val started = projection != null && appContainer.audioEngine.startCapture(projection)
          if (!started) {
            promoteToForeground(includeMediaProjection = false)
          }
          observeForNotificationUpdates()
        } else {
          promoteToForeground(includeMediaProjection = false)
        }
      }

      ACTION_STOP_AUDIO -> {
        appContainer.audioEngine.stopCapture()
        if (appContainer.bridgeServer.isRunning.value) {
          promoteToForeground(includeMediaProjection = false)
        }
      }

      ACTION_STOP_SERVER -> {
        shutdownAll()
      }
    }

    return START_STICKY
  }

  private fun promoteToForeground(includeMediaProjection: Boolean) {
    val types =
      if (includeMediaProjection) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
          ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
          ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
      } else {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
          ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
      }

    val notification = buildNotification()
    startForeground(NOTIFICATION_ID, notification, types)
  }

  private fun observeForNotificationUpdates() {
    if (stateObserverJob?.isActive == true) return
    stateObserverJob =
      serviceScope.launch {
        launch {
          appContainer.mediaMonitor.activeSession.collect { updateNotification() }
        }
        launch {
          appContainer.audioEngine.isCapturing.collect { updateNotification() }
        }
        launch {
          appContainer.bridgeServer.connectedClients.collect { updateNotification() }
        }
      }
  }

  private fun updateNotification() {
    if (!appContainer.bridgeServer.isRunning.value && !appContainer.audioEngine.isCapturing.value) {
      return
    }
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIFICATION_ID, buildNotification())
  }

  private fun buildNotification(): Notification {
    val openIntent =
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val openPending =
      PendingIntent.getActivity(
        this,
        0,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val stopIntent =
      Intent(this, MediaBridgeService::class.java).apply { action = ACTION_STOP_SERVER }
    val stopPending =
      PendingIntent.getService(
        this,
        1,
        stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val port = appContainer.bridgeServer.currentPort.value
    val clients = appContainer.bridgeServer.connectedClients.value
    val audioOn = appContainer.audioEngine.isCapturing.value
    val track = appContainer.mediaMonitor.activeSession.value

    val title =
      if (audioOn) {
        "WaveBridge Live Audio · Port $port"
      } else {
        "WaveBridge Active · Port $port"
      }

    val detail =
      buildString {
        if (track != null) {
          append("${track.title} — ${track.artist}")
        } else {
          append("Listening for media sessions")
        }
        append(" · $clients PC${if (clients == 1) "" else "s"} connected")
      }

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(title)
      .setContentText(detail)
      .setContentIntent(openPending)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Bridge", stopPending)
      .build()
  }

  private fun acquireLocks() {
    if (wakeLock == null) {
      val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
      wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WaveBridge::ServerWakeLock")
    }
    if (wakeLock?.isHeld == false) {
      wakeLock?.acquire(12 * 60 * 60 * 1000L)
    }

    if (wifiLock == null) {
      val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
      wifiLock =
        wm.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "WaveBridge::WifiLock")
    }
    if (wifiLock?.isHeld == false) {
      wifiLock?.acquire()
    }
  }

  private fun releaseLocks() {
    try {
      if (wakeLock?.isHeld == true) wakeLock?.release()
    } catch (_: Exception) {
    }
    wakeLock = null
    try {
      if (wifiLock?.isHeld == true) wifiLock?.release()
    } catch (_: Exception) {
    }
    wifiLock = null
  }

  private fun shutdownAll() {
    stateObserverJob?.cancel()
    stateObserverJob = null
    appContainer.audioEngine.stopCapture()
    appContainer.bridgeServer.stop()
    releaseLocks()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun createNotificationChannel() {
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel =
      NotificationChannel(
        CHANNEL_ID,
        getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = getString(R.string.notification_channel_desc)
        setShowBadge(false)
      }
    manager.createNotificationChannel(channel)
  }

  override fun onDestroy() {
    appContainer.audioEngine.stopCapture()
    appContainer.bridgeServer.stop()
    releaseLocks()
    serviceScope.cancel()
    super.onDestroy()
  }
}
