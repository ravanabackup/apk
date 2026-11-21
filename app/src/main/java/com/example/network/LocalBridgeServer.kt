package com.example.network

import com.example.data.ScrobbleRepository
import com.example.media.AudioCaptureEngine
import com.example.media.MediaSessionMonitor
import com.example.model.AudioConfigMessage
import com.example.model.BridgeStatePayload
import com.example.model.ClientCommand
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import java.util.Collections
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalBridgeServer(
  private val mediaMonitor: MediaSessionMonitor,
  private val audioEngine: AudioCaptureEngine,
  private val scrobbleRepository: ScrobbleRepository,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
  private var broadcastJob: Job? = null
  private var audioJob: Job? = null

  private val controlClients =
    Collections.synchronizedSet(mutableSetOf<DefaultWebSocketServerSession>())
  private val audioClients =
    Collections.synchronizedSet(mutableSetOf<DefaultWebSocketServerSession>())

  private val _isRunning = MutableStateFlow(false)
  val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

  private val _currentPort = MutableStateFlow(8765)
  val currentPort: StateFlow<Int> = _currentPort.asStateFlow()

  private val _connectedClients = MutableStateFlow(0)
  val connectedClients: StateFlow<Int> = _connectedClients.asStateFlow()

  private val _lastError = MutableStateFlow<String?>(null)
  val lastError: StateFlow<String?> = _lastError.asStateFlow()

  fun start(port: Int) {
    if (_isRunning.value && _currentPort.value == port) return
    stop()
    _lastError.value = null
    _currentPort.value = port

    try {
      val engine =
        embeddedServer(CIO, host = "0.0.0.0", port = port) {
          install(CORS) {
            anyHost()
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowHeader(HttpHeaders.ContentType)
          }
          install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 30.seconds
            maxFrameSize = Long.MAX_VALUE
          }

          routing {
            get("/") {
              call.respondText(DashboardHtml.render(), ContentType.Text.Html)
            }

            get("/api/state") {
              val payload = buildStatePayload()
              call.respondText(json.encodeToString(payload), ContentType.Application.Json)
            }

            get("/api/artwork/{sessionId}") {
              val sessionId = call.parameters["sessionId"] ?: ""
              val bytes = mediaMonitor.getArtworkBytes(sessionId)
              if (bytes != null) {
                call.respondBytes(bytes, ContentType.Image.JPEG)
              } else {
                call.respondText("No artwork", status = HttpStatusCode.NotFound)
              }
            }

            get("/api/history") {
              val recent = scrobbleRepository.getRecentSnapshot(100)
              call.respondText(json.encodeToString(recent), ContentType.Application.Json)
            }

            get("/api/history/export") {
              val format = call.request.queryParameters["format"]?.lowercase() ?: "json"
              if (format == "csv") {
                val csv = scrobbleRepository.exportAsCsv()
                call.response.header(
                  HttpHeaders.ContentDisposition,
                  ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName,
                    "wavebridge-scrobbles.csv",
                  ).toString(),
                )
                call.respondText(csv, ContentType.Text.CSV)
              } else {
                val exportJson = scrobbleRepository.exportAsJson()
                call.response.header(
                  HttpHeaders.ContentDisposition,
                  ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName,
                    "wavebridge-scrobbles.json",
                  ).toString(),
                )
                call.respondText(exportJson, ContentType.Application.Json)
              }
            }

            webSocket("/ws/control") {
              controlClients.add(this)
              updateClientCount()
              try {
                val initial = json.encodeToString(buildStatePayload())
                send(Frame.Text(initial))

                for (frame in incoming) {
                  if (frame is Frame.Text) {
                    handleClientCommand(frame.readText())
                  }
                }
              } finally {
                controlClients.remove(this)
                updateClientCount()
              }
            }

            webSocket("/ws/audio") {
              audioClients.add(this)
              try {
                val config =
                  AudioConfigMessage(
                    sampleRate = AudioCaptureEngine.SAMPLE_RATE,
                    channels = AudioCaptureEngine.CHANNELS,
                    bitsPerSample = AudioCaptureEngine.BITS_PER_SAMPLE,
                    active = audioEngine.isCapturing.value,
                    note = audioEngine.statusNote.value,
                  )
                send(Frame.Text(json.encodeToString(config)))
                for (frame in incoming) {
                  // Keep connection alive until client disconnects
                }
              } finally {
                audioClients.remove(this)
              }
            }
          }
        }

      engine.start(wait = false)
      server = engine
      _isRunning.value = true
      startBroadcastLoops()
    } catch (e: Exception) {
      _lastError.value = e.message ?: "Failed to bind port $port"
      _isRunning.value = false
    }
  }

  fun stop() {
    broadcastJob?.cancel()
    broadcastJob = null
    audioJob?.cancel()
    audioJob = null
    controlClients.clear()
    audioClients.clear()
    _connectedClients.value = 0

    try {
      server?.stop(300, 800)
    } catch (_: Exception) {
    }
    server = null
    _isRunning.value = false
  }

  private fun startBroadcastLoops() {
    broadcastJob?.cancel()
    broadcastJob =
      scope.launch {
        launch {
          mediaMonitor.stateChangedEvents.collect {
            broadcastStateSnapshot()
          }
        }
        launch {
          audioEngine.isCapturing.collect {
            broadcastStateSnapshot()
          }
        }
      }

    audioJob?.cancel()
    audioJob =
      scope.launch {
        audioEngine.pcmChunks.collect { chunk ->
          val clientsSnapshot = audioClients.toList()
          for (session in clientsSnapshot) {
            try {
              session.send(Frame.Binary(true, chunk))
            } catch (_: Exception) {
              audioClients.remove(session)
            }
          }
        }
      }
  }

  private suspend fun broadcastStateSnapshot() {
    val clientsSnapshot = controlClients.toList()
    if (clientsSnapshot.isEmpty()) return
    val text = json.encodeToString(buildStatePayload())
    for (session in clientsSnapshot) {
      try {
        session.send(Frame.Text(text))
      } catch (_: Exception) {
        controlClients.remove(session)
        updateClientCount()
      }
    }
  }

  private fun handleClientCommand(raw: String) {
    val cmd =
      try {
        json.decodeFromString<ClientCommand>(raw)
      } catch (_: Exception) {
        return
      }
    when (cmd.action) {
      "play" -> mediaMonitor.play()
      "pause" -> mediaMonitor.pause()
      "toggle" -> mediaMonitor.togglePlayPause()
      "next" -> mediaMonitor.skipToNext()
      "previous" -> mediaMonitor.skipToPrevious()
      "seek" -> cmd.positionMs?.let { mediaMonitor.seekTo(it) }
      "volume" -> cmd.volume?.let { mediaMonitor.setVolume(it) }
      "selectSession" -> cmd.sessionId?.let { mediaMonitor.selectSession(it) }
    }
  }

  private suspend fun buildStatePayload(): BridgeStatePayload {
    return BridgeStatePayload(
      serverRunning = _isRunning.value,
      port = _currentPort.value,
      liveAudioActive = audioEngine.isCapturing.value,
      liveAudioNote = audioEngine.statusNote.value,
      activeSessionId = mediaMonitor.activeSessionId.value,
      activeSession = mediaMonitor.activeSession.value,
      sessions = mediaMonitor.sessions.value,
      recentScrobbles = scrobbleRepository.getRecentSnapshot(40),
      connectedClients = _connectedClients.value,
    )
  }

  private fun updateClientCount() {
    _connectedClients.value = controlClients.size
  }
}
