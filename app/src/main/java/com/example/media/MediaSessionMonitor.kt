package com.example.media

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.media.session.MediaController.PlaybackInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import com.example.data.ScrobbleRepository
import com.example.data.SettingsRepository
import com.example.model.MediaSessionInfo
import com.example.model.PlaybackStatus
import com.example.service.MediaNotificationListenerService
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MediaSessionMonitor(
  private val context: Context,
  private val scrobbleRepository: ScrobbleRepository,
  private val settingsRepository: SettingsRepository,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val mainHandler = Handler(Looper.getMainLooper())
  private val mediaSessionManager =
    context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
  private val audioManager =
    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  private val listenerComponent by lazy {
    ComponentName(context, MediaNotificationListenerService::class.java)
  }

  private val controllers = ConcurrentHashMap<String, MediaController>()
  private val callbacks = ConcurrentHashMap<String, MediaController.Callback>()
  private val artworkJpegCache = ConcurrentHashMap<String, Pair<String, ByteArray>>()
  private val artworkBitmapCache = ConcurrentHashMap<String, Bitmap>()

  private val _sessions = MutableStateFlow<List<MediaSessionInfo>>(emptyList())
  val sessions: StateFlow<List<MediaSessionInfo>> = _sessions.asStateFlow()

  private val _activeSessionId = MutableStateFlow<String?>(null)
  val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

  private val _activeSession = MutableStateFlow<MediaSessionInfo?>(null)
  val activeSession: StateFlow<MediaSessionInfo?> = _activeSession.asStateFlow()

  private val _activeArtwork = MutableStateFlow<Bitmap?>(null)
  val activeArtwork: StateFlow<Bitmap?> = _activeArtwork.asStateFlow()

  private val _stateChangedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
  val stateChangedEvents: SharedFlow<Unit> = _stateChangedEvents.asSharedFlow()

  private var preferredSessionId: String? = null
  private var sessionListenerRegistered = false
  private var tickerJob: Job? = null

  // Scrobble tracking
  private var currentTrackKey: String? = null
  private var trackPlayAccumulatedMs: Long = 0L
  private var lastTickWallMs: Long = 0L
  private var scrobbledCurrentTrack = false

  private val activeSessionsChangedListener =
    MediaSessionManager.OnActiveSessionsChangedListener { newControllers ->
      updateControllers(newControllers ?: emptyList())
    }

  fun isNotificationAccessGranted(): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context)
      .contains(context.packageName)
  }

  fun startMonitoring() {
    if (!isNotificationAccessGranted()) {
      clearAllSessions()
      return
    }
    try {
      if (!sessionListenerRegistered) {
        mediaSessionManager.addOnActiveSessionsChangedListener(
          activeSessionsChangedListener,
          listenerComponent,
          mainHandler,
        )
        sessionListenerRegistered = true
      }
      val active = mediaSessionManager.getActiveSessions(listenerComponent)
      updateControllers(active)
      startTickerIfNeeded()
    } catch (_: SecurityException) {
      sessionListenerRegistered = false
      clearAllSessions()
    }
  }

  fun stopMonitoring() {
    if (sessionListenerRegistered) {
      try {
        mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
      } catch (_: Exception) {
      }
      sessionListenerRegistered = false
    }
    tickerJob?.cancel()
    tickerJob = null
    clearAllSessions()
  }

  fun refreshNow() {
    if (isNotificationAccessGranted()) {
      startMonitoring()
    } else {
      clearAllSessions()
    }
  }

  fun getArtworkBytes(sessionId: String): ByteArray? = artworkJpegCache[sessionId]?.second

  fun selectSession(sessionId: String) {
    if (controllers.containsKey(sessionId)) {
      preferredSessionId = sessionId
      publishState(emitEvent = true)
    }
  }

  fun play() {
    getTargetController()?.transportControls?.play()
  }

  fun pause() {
    getTargetController()?.transportControls?.pause()
  }

  fun togglePlayPause() {
    val target = getTargetController() ?: return
    val status = mapPlaybackStatus(target.playbackState?.state)
    if (status == PlaybackStatus.PLAYING || status == PlaybackStatus.BUFFERING) {
      target.transportControls.pause()
    } else {
      target.transportControls.play()
    }
  }

  fun skipToNext() {
    getTargetController()?.transportControls?.skipToNext()
  }

  fun skipToPrevious() {
    getTargetController()?.transportControls?.skipToPrevious()
  }

  fun seekTo(positionMs: Long) {
    getTargetController()?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    mainHandler.postDelayed({ publishState(emitEvent = true) }, 120)
  }

  fun setVolume(volume: Int) {
    val target = getTargetController()
    val info = target?.playbackInfo
    if (target != null && info != null && info.volumeControl != android.media.VolumeProvider.VOLUME_CONTROL_FIXED) {
      val clamped = volume.coerceIn(0, info.maxVolume.coerceAtLeast(1))
      target.setVolumeTo(clamped, 0)
    } else {
      val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
      val clamped = volume.coerceIn(0, max)
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
    }
    publishState(emitEvent = true)
  }

  private fun getTargetController(): MediaController? {
    val id = _activeSessionId.value
    return if (id != null) controllers[id] else controllers.values.firstOrNull()
  }

  private fun startTickerIfNeeded() {
    if (tickerJob?.isActive == true) return
    lastTickWallMs = SystemClock.elapsedRealtime()
    tickerJob =
      scope.launch {
        while (isActive) {
          delay(1000L)
          val now = SystemClock.elapsedRealtime()
          val delta = (now - lastTickWallMs).coerceIn(0L, 3000L)
          lastTickWallMs = now

          val active = _activeSession.value
          if (active != null && active.status == PlaybackStatus.PLAYING) {
            evaluateScrobble(active, delta)
            publishState(emitEvent = true)
          }
        }
      }
  }

  private suspend fun evaluateScrobble(session: MediaSessionInfo, deltaMs: Long) {
    if (session.title.isBlank()) return
    val key = "${session.packageName}|${session.title}|${session.artist}"
    if (key != currentTrackKey) {
      currentTrackKey = key
      trackPlayAccumulatedMs = 0L
      scrobbledCurrentTrack = false
    } else {
      trackPlayAccumulatedMs += deltaMs
    }

    val delayThresholdMs = settingsRepository.getCurrentSettings().scrobbleDelaySec * 1000L
    if (!scrobbledCurrentTrack && trackPlayAccumulatedMs >= delayThresholdMs) {
      scrobbledCurrentTrack = true
      val recorded =
        scrobbleRepository.recordScrobbleIfNew(
          title = session.title,
          artist = session.artist,
          album = session.album,
          appName = session.appName,
          packageName = session.packageName,
          durationMs = session.durationMs,
        )
      if (recorded) {
        _stateChangedEvents.tryEmit(Unit)
      }
    }
  }

  private fun updateControllers(newList: List<MediaController>) {
    val newIds = newList.map { controllerId(it) }.toSet()

    // Unregister removed controllers
    val removedIds = controllers.keys.filter { it !in newIds }
    for (id in removedIds) {
      val oldController = controllers.remove(id)
      val oldCallback = callbacks.remove(id)
      if (oldController != null && oldCallback != null) {
        try {
          oldController.unregisterCallback(oldCallback)
        } catch (_: Exception) {
        }
      }
      artworkJpegCache.remove(id)
      artworkBitmapCache.remove(id)
    }

    // Register new controllers
    for (controller in newList) {
      val id = controllerId(controller)
      if (!controllers.containsKey(id)) {
        val callback =
          object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
              publishState(emitEvent = true)
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
              cacheArtworkForController(id, metadata)
              publishState(emitEvent = true)
            }

            override fun onAudioInfoChanged(info: PlaybackInfo) {
              publishState(emitEvent = true)
            }

            override fun onSessionDestroyed() {
              controllers.remove(id)
              callbacks.remove(id)
              artworkJpegCache.remove(id)
              artworkBitmapCache.remove(id)
              publishState(emitEvent = true)
            }
          }
        controllers[id] = controller
        callbacks[id] = callback
        controller.registerCallback(callback, mainHandler)
        cacheArtworkForController(id, controller.metadata)
      }
    }

    publishState(emitEvent = true)
  }

  private fun clearAllSessions() {
    for ((id, controller) in controllers) {
      val cb = callbacks[id]
      if (cb != null) {
        try {
          controller.unregisterCallback(cb)
        } catch (_: Exception) {
        }
      }
    }
    controllers.clear()
    callbacks.clear()
    artworkJpegCache.clear()
    artworkBitmapCache.clear()
    _sessions.value = emptyList()
    _activeSessionId.value = null
    _activeSession.value = null
    _activeArtwork.value = null
    _stateChangedEvents.tryEmit(Unit)
  }

  private fun publishState(emitEvent: Boolean) {
    val built =
      controllers.entries
        .map { (id, controller) -> buildSessionInfo(id, controller) }
        .sortedWith(
          compareByDescending<MediaSessionInfo> { it.status == PlaybackStatus.PLAYING }
            .thenByDescending { it.status == PlaybackStatus.BUFFERING }
            .thenByDescending { it.status == PlaybackStatus.PAUSED }
            .thenBy { it.appName }
        )

    _sessions.value = built

    val chosen =
      built.firstOrNull { it.sessionId == preferredSessionId }
        ?: built.firstOrNull { it.status == PlaybackStatus.PLAYING }
        ?: built.firstOrNull { it.status == PlaybackStatus.BUFFERING }
        ?: built.firstOrNull()

    _activeSessionId.value = chosen?.sessionId
    _activeSession.value = chosen
    _activeArtwork.value = chosen?.sessionId?.let { artworkBitmapCache[it] }

    if (emitEvent) {
      _stateChangedEvents.tryEmit(Unit)
    }
  }

  private fun buildSessionInfo(id: String, controller: MediaController): MediaSessionInfo {
    val meta = controller.metadata
    val state = controller.playbackState
    val pkg = controller.packageName
    val appName = resolveAppName(pkg)

    val title =
      meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
        ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        ?: ""
    val artist =
      meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        ?: ""
    val album =
      meta?.getString(MediaMetadata.METADATA_KEY_ALBUM)
        ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)
        ?: ""
    val durationMs = (meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L)

    val status = mapPlaybackStatus(state?.state)
    val speed = (state?.playbackSpeed ?: 1f).takeIf { it > 0f } ?: 1f
    val rawPos = (state?.position ?: 0L).coerceAtLeast(0L)
    val computedPos =
      if (status == PlaybackStatus.PLAYING && state != null && state.lastPositionUpdateTime > 0L) {
        val elapsed = (SystemClock.elapsedRealtime() - state.lastPositionUpdateTime).coerceAtLeast(0L)
        val pos = rawPos + (elapsed * speed).toLong()
        if (durationMs > 0L) pos.coerceIn(0L, durationMs) else pos
      } else {
        if (durationMs > 0L) rawPos.coerceIn(0L, durationMs) else rawPos
      }

    val playbackInfo = controller.playbackInfo
    val (vol, maxVol) =
      if (playbackInfo != null && playbackInfo.volumeControl != android.media.VolumeProvider.VOLUME_CONTROL_FIXED && playbackInfo.maxVolume > 0) {
        playbackInfo.currentVolume to playbackInfo.maxVolume
      } else {
        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) to
          audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
      }

    val artEntry = artworkJpegCache[id]
    return MediaSessionInfo(
      sessionId = id,
      packageName = pkg,
      appName = appName,
      title = title.ifBlank { "No Track Metadata" },
      artist = artist.ifBlank { appName },
      album = album,
      durationMs = durationMs,
      positionMs = computedPos,
      speed = speed,
      status = status,
      volume = vol,
      maxVolume = maxVol,
      hasArtwork = artEntry != null,
      artworkHash = artEntry?.first ?: "",
      artworkBase64 = null,
    )
  }

  private fun cacheArtworkForController(id: String, metadata: MediaMetadata?) {
    if (metadata == null) {
      artworkJpegCache.remove(id)
      artworkBitmapCache.remove(id)
      return
    }
    val rawBitmap =
      metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

    if (rawBitmap == null) {
      artworkJpegCache.remove(id)
      artworkBitmapCache.remove(id)
      return
    }

    val scaled = scaleBitmap(rawBitmap, 384)
    val baos = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 84, baos)
    val bytes = baos.toByteArray()
    val hash = sha256Short(bytes)
    artworkJpegCache[id] = hash to bytes
    artworkBitmapCache[id] = scaled
  }

  private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDim && h <= maxDim) return bitmap
    val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
    val nw = (w * ratio).toInt().coerceAtLeast(1)
    val nh = (h * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
  }

  private fun sha256Short(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.take(6).joinToString("") { "%02x".format(it) }
  }

  private fun controllerId(controller: MediaController): String {
    return "${controller.packageName}_${controller.sessionToken.hashCode().toUInt()}"
  }

  private fun resolveAppName(packageName: String): String {
    return try {
      val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
      context.packageManager.getApplicationLabel(appInfo).toString()
    } catch (_: PackageManager.NameNotFoundException) {
      packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
  }

  private fun mapPlaybackStatus(state: Int?): PlaybackStatus =
    when (state) {
      PlaybackState.STATE_PLAYING -> PlaybackStatus.PLAYING
      PlaybackState.STATE_PAUSED -> PlaybackStatus.PAUSED
      PlaybackState.STATE_BUFFERING, PlaybackState.STATE_CONNECTING -> PlaybackStatus.BUFFERING
      PlaybackState.STATE_STOPPED, PlaybackState.STATE_ERROR -> PlaybackStatus.STOPPED
      else -> PlaybackStatus.NONE
    }
}
