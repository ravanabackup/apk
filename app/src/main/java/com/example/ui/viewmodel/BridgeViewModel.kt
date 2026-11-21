package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WaveBridgeApplication
import com.example.data.BridgeSettings
import com.example.model.MediaSessionInfo
import com.example.model.NetworkUrlInfo
import com.example.model.PermissionHealthState
import com.example.model.ScrobbleDto
import com.example.network.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class BridgeUiState(
  val serverRunning: Boolean = false,
  val serverPort: Int = 8765,
  val serverError: String? = null,
  val connectedClients: Int = 0,
  val reachableUrls: List<NetworkUrlInfo> = emptyList(),
  val activeSession: MediaSessionInfo? = null,
  val sessions: List<MediaSessionInfo> = emptyList(),
  val activeArtwork: Bitmap? = null,
  val liveAudioActive: Boolean = false,
  val audioLevel: Float = 0f,
  val audioStatusNote: String = "Off — metadata & controls still active",
  val scrobbles: List<ScrobbleDto> = emptyList(),
  val settings: BridgeSettings = BridgeSettings(),
  val permissions: PermissionHealthState = PermissionHealthState(),
)

class BridgeViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as WaveBridgeApplication
  private val container = app.container

  private val _permissions = MutableStateFlow(readPermissions())
  private val _reachableUrls = MutableStateFlow<List<NetworkUrlInfo>>(emptyList())

  private val serverSnapshotFlow =
    combine(
      container.bridgeServer.isRunning,
      container.bridgeServer.currentPort,
      container.bridgeServer.lastError,
      container.bridgeServer.connectedClients,
      _reachableUrls,
    ) { running, port, err, clients, urls ->
      ServerSnapshot(running, port, err, clients, urls)
    }

  private val mediaSnapshotFlow =
    combine(
      container.mediaMonitor.activeSession,
      container.mediaMonitor.sessions,
      container.mediaMonitor.activeArtwork,
    ) { active, all, art ->
      MediaSnapshot(active, all, art)
    }

  private val audioSnapshotFlow =
    combine(
      container.audioEngine.isCapturing,
      container.audioEngine.audioLevel,
      container.audioEngine.statusNote,
    ) { active, level, note ->
      AudioSnapshot(active, level, note)
    }

  val uiState: StateFlow<BridgeUiState> =
    combine(
      serverSnapshotFlow,
      mediaSnapshotFlow,
      audioSnapshotFlow,
      container.scrobbleRepository.recentScrobbles,
      container.settingsRepository.settingsFlow,
      _permissions,
    ) { values ->
      val server = values[0] as ServerSnapshot
      val media = values[1] as MediaSnapshot
      val audio = values[2] as AudioSnapshot
      @Suppress("UNCHECKED_CAST")
      val scrobbles = values[3] as List<ScrobbleDto>
      val settings = values[4] as BridgeSettings
      val perms = values[5] as PermissionHealthState

      BridgeUiState(
        serverRunning = server.running,
        serverPort = settings.serverPort,
        serverError = server.error,
        connectedClients = server.clients,
        reachableUrls = server.urls,
        activeSession = media.active,
        sessions = media.sessions,
        activeArtwork = media.artwork,
        liveAudioActive = audio.active,
        audioLevel = audio.level,
        audioStatusNote = audio.note,
        scrobbles = scrobbles,
        settings = settings,
        permissions = perms,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = BridgeUiState(permissions = readPermissions()),
    )

  init {
    refreshAll()
    viewModelScope.launch {
      val settings = container.settingsRepository.getCurrentSettings()
      if (settings.autoStartServer && !container.bridgeServer.isRunning.value) {
        container.startBridgeService(settings.serverPort)
      }
    }
    viewModelScope.launch {
      while (isActive) {
        val port = container.settingsRepository.getCurrentSettings().serverPort
        _reachableUrls.value = NetworkUtils.getReachableUrls(port)
        _permissions.value = readPermissions()
        delay(4000L)
      }
    }
  }

  fun refreshAll() {
    _permissions.value = readPermissions()
    viewModelScope.launch {
      val port = container.settingsRepository.getCurrentSettings().serverPort
      _reachableUrls.value = NetworkUtils.getReachableUrls(port)
      if (_permissions.value.notificationAccessGranted) {
        container.mediaMonitor.refreshNow()
      }
    }
  }

  fun toggleServer() {
    if (container.bridgeServer.isRunning.value) {
      container.stopBridgeService()
    } else {
      viewModelScope.launch {
        val port = container.settingsRepository.getCurrentSettings().serverPort
        container.startBridgeService(port)
      }
    }
  }

  fun startAudioCaptureWithProjection(resultCode: Int, data: Intent) {
    viewModelScope.launch {
      val port = container.settingsRepository.getCurrentSettings().serverPort
      container.startLiveAudioCapture(resultCode, data, port)
    }
  }

  fun stopAudioCapture() {
    container.stopLiveAudioCapture()
  }

  fun selectSession(sessionId: String) = container.mediaMonitor.selectSession(sessionId)

  fun togglePlayPause() = container.mediaMonitor.togglePlayPause()

  fun nextTrack() = container.mediaMonitor.skipToNext()

  fun previousTrack() = container.mediaMonitor.skipToPrevious()

  fun seekTo(positionMs: Long) = container.mediaMonitor.seekTo(positionMs)

  fun updatePort(newPort: Int) {
    viewModelScope.launch {
      container.settingsRepository.setServerPort(newPort)
      _reachableUrls.value = NetworkUtils.getReachableUrls(newPort.coerceIn(1024, 65535))
      if (container.bridgeServer.isRunning.value) {
        container.startBridgeService(newPort.coerceIn(1024, 65535))
      }
    }
  }

  fun updateAutoStart(enabled: Boolean) {
    viewModelScope.launch {
      container.settingsRepository.setAutoStartServer(enabled)
    }
  }

  fun updateScrobbleDelay(seconds: Int) {
    viewModelScope.launch {
      container.settingsRepository.setScrobbleDelaySec(seconds)
    }
  }

  fun clearScrobbles() {
    viewModelScope.launch {
      container.scrobbleRepository.clearHistory()
    }
  }

  suspend fun exportScrobbles(asCsv: Boolean): String {
    return if (asCsv) {
      container.scrobbleRepository.exportAsCsv()
    } else {
      container.scrobbleRepository.exportAsJson()
    }
  }

  private fun readPermissions(): PermissionHealthState {
    val notifListener = container.mediaMonitor.isNotificationAccessGranted()
    val postNotif =
      ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    val pm = app.getSystemService(PowerManager::class.java)
    val batteryIgnored = pm?.isIgnoringBatteryOptimizations(app.packageName) == true
    val recordAudio = container.audioEngine.hasRecordAudioPermission()

    return PermissionHealthState(
      notificationAccessGranted = notifListener,
      postNotificationsGranted = postNotif,
      batteryOptimizationIgnored = batteryIgnored,
      recordAudioGranted = recordAudio,
      mediaProjectionActive = container.audioEngine.isCapturing.value,
    )
  }

  private data class ServerSnapshot(
    val running: Boolean,
    val port: Int,
    val error: String?,
    val clients: Int,
    val urls: List<NetworkUrlInfo>,
  )

  private data class MediaSnapshot(
    val active: MediaSessionInfo?,
    val sessions: List<MediaSessionInfo>,
    val artwork: Bitmap?,
  )

  private data class AudioSnapshot(
    val active: Boolean,
    val level: Float,
    val note: String,
  )
}
