package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlaybackStatus {
  PLAYING,
  PAUSED,
  BUFFERING,
  STOPPED,
  NONE,
}

@Serializable
data class MediaSessionInfo(
  val sessionId: String,
  val packageName: String,
  val appName: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationMs: Long,
  val positionMs: Long,
  val speed: Float,
  val status: PlaybackStatus,
  val volume: Int,
  val maxVolume: Int,
  val hasArtwork: Boolean,
  val artworkHash: String,
  val artworkBase64: String? = null,
)

@Serializable
data class ScrobbleDto(
  val id: Long,
  val title: String,
  val artist: String,
  val album: String,
  val appName: String,
  val packageName: String,
  val durationMs: Long,
  val playedAtMs: Long,
)

@Serializable
data class BridgeStatePayload(
  val type: String = "state",
  val serverRunning: Boolean,
  val port: Int,
  val liveAudioActive: Boolean,
  val liveAudioNote: String,
  val activeSessionId: String?,
  val activeSession: MediaSessionInfo?,
  val sessions: List<MediaSessionInfo>,
  val recentScrobbles: List<ScrobbleDto>,
  val connectedClients: Int,
  val serverTimestampMs: Long = System.currentTimeMillis(),
)

@Serializable
data class ClientCommand(
  val action: String,
  val positionMs: Long? = null,
  val volume: Int? = null,
  val sessionId: String? = null,
)

@Serializable
data class AudioConfigMessage(
  val type: String = "audioConfig",
  val sampleRate: Int = 44100,
  val channels: Int = 2,
  val bitsPerSample: Int = 16,
  val active: Boolean,
  val note: String = "",
)

data class NetworkUrlInfo(
  val url: String,
  val ipAddress: String,
  val interfaceName: String,
  val label: String,
  val isTailscale: Boolean,
  val isWifi: Boolean,
)

data class PermissionHealthState(
  val notificationAccessGranted: Boolean = false,
  val postNotificationsGranted: Boolean = false,
  val batteryOptimizationIgnored: Boolean = false,
  val recordAudioGranted: Boolean = false,
  val mediaProjectionActive: Boolean = false,
) {
  val coreBridgeReady: Boolean
    get() = notificationAccessGranted
}
