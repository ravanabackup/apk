package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.ScrobbleRepository
import com.example.data.SettingsRepository
import com.example.data.WaveBridgeDatabase
import com.example.media.AudioCaptureEngine
import com.example.media.MediaSessionMonitor
import com.example.network.LocalBridgeServer
import com.example.service.MediaBridgeService

class AppContainer(context: Context) {
  private val appContext = context.applicationContext

  val database: WaveBridgeDatabase = WaveBridgeDatabase.getInstance(appContext)
  val scrobbleRepository = ScrobbleRepository(database.scrobbleDao())
  val settingsRepository = SettingsRepository(appContext)
  val mediaMonitor = MediaSessionMonitor(appContext, scrobbleRepository, settingsRepository)
  val audioEngine = AudioCaptureEngine(appContext)
  val bridgeServer = LocalBridgeServer(mediaMonitor, audioEngine, scrobbleRepository)

  fun startBridgeService(port: Int) {
    val intent =
      Intent(appContext, MediaBridgeService::class.java).apply {
        action = MediaBridgeService.ACTION_START_SERVER
        putExtra(MediaBridgeService.EXTRA_PORT, port)
      }
    ContextCompat.startForegroundService(appContext, intent)
  }

  fun stopBridgeService() {
    val intent =
      Intent(appContext, MediaBridgeService::class.java).apply {
        action = MediaBridgeService.ACTION_STOP_SERVER
      }
    appContext.startService(intent)
  }

  fun startLiveAudioCapture(resultCode: Int, data: Intent, port: Int) {
    val intent =
      Intent(appContext, MediaBridgeService::class.java).apply {
        action = MediaBridgeService.ACTION_START_AUDIO
        putExtra(MediaBridgeService.EXTRA_RESULT_CODE, resultCode)
        putExtra(MediaBridgeService.EXTRA_RESULT_DATA, data)
        putExtra(MediaBridgeService.EXTRA_PORT, port)
      }
    ContextCompat.startForegroundService(appContext, intent)
  }

  fun stopLiveAudioCapture() {
    val intent =
      Intent(appContext, MediaBridgeService::class.java).apply {
        action = MediaBridgeService.ACTION_STOP_AUDIO
      }
    appContext.startService(intent)
  }
}

class WaveBridgeApplication : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}
