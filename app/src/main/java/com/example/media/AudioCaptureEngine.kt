package com.example.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioCaptureEngine(private val context: Context) {

  companion object {
    const val SAMPLE_RATE = 44100
    const val CHANNELS = 2
    const val BITS_PER_SAMPLE = 16
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val mainHandler = Handler(Looper.getMainLooper())

  private var mediaProjection: MediaProjection? = null
  private var audioRecord: AudioRecord? = null
  private var captureJob: Job? = null

  private val _isCapturing = MutableStateFlow(false)
  val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

  private val _audioLevel = MutableStateFlow(0f)
  val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

  private val _statusNote = MutableStateFlow("Off — metadata & controls still active")
  val statusNote: StateFlow<String> = _statusNote.asStateFlow()

  private val _pcmChunks = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
  val pcmChunks: SharedFlow<ByteArray> = _pcmChunks.asSharedFlow()

  private val projectionCallback =
    object : MediaProjection.Callback() {
      override fun onStop() {
        stopCapture("Audio capture stopped by system")
      }
    }

  fun hasRecordAudioPermission(): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
      PackageManager.PERMISSION_GRANTED
  }

  @SuppressLint("MissingPermission")
  fun startCapture(projection: MediaProjection): Boolean {
    stopCapture()
    if (!hasRecordAudioPermission()) {
      _statusNote.value = "RECORD_AUDIO permission missing"
      return false
    }

    return try {
      mediaProjection = projection
      projection.registerCallback(projectionCallback, mainHandler)

      val config =
        AudioPlaybackCaptureConfiguration.Builder(projection)
          .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
          .addMatchingUsage(AudioAttributes.USAGE_GAME)
          .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
          .build()

      val audioFormat =
        AudioFormat.Builder()
          .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
          .setSampleRate(SAMPLE_RATE)
          .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
          .build()

      val minBufSize =
        AudioRecord.getMinBufferSize(
          SAMPLE_RATE,
          AudioFormat.CHANNEL_IN_STEREO,
          AudioFormat.ENCODING_PCM_16BIT,
        )
      val bufferSize = maxOf(minBufSize, 4096 * 2)

      val record =
        AudioRecord.Builder()
          .setAudioFormat(audioFormat)
          .setBufferSizeInBytes(bufferSize)
          .setAudioPlaybackCaptureConfig(config)
          .build()

      if (record.state != AudioRecord.STATE_INITIALIZED) {
        record.release()
        projection.stop()
        _statusNote.value = "AudioRecord failed to initialize"
        return false
      }

      audioRecord = record
      record.startRecording()
      _isCapturing.value = true
      _statusNote.value = "Streaming 44.1kHz Stereo PCM (DRM apps may output silence)"

      captureJob =
        scope.launch {
          val readSize = 4096
          val byteBuf = ByteArray(readSize)
          while (isActive && _isCapturing.value) {
            val bytesRead = record.read(byteBuf, 0, readSize)
            if (bytesRead > 0) {
              val packet = byteBuf.copyOf(bytesRead)
              _audioLevel.value = computeNormalizedRms(packet, bytesRead)
              _pcmChunks.tryEmit(packet)
            }
          }
        }
      true
    } catch (e: Exception) {
      stopCapture("Audio capture error: ${e.message ?: "unknown"}")
      false
    }
  }

  fun stopCapture(note: String = "Off — metadata & controls still active") {
    _isCapturing.value = false
    _audioLevel.value = 0f
    _statusNote.value = note
    captureJob?.cancel()
    captureJob = null

    try {
      audioRecord?.stop()
    } catch (_: Exception) {
    }
    try {
      audioRecord?.release()
    } catch (_: Exception) {
    }
    audioRecord = null

    try {
      mediaProjection?.unregisterCallback(projectionCallback)
      mediaProjection?.stop()
    } catch (_: Exception) {
    }
    mediaProjection = null
  }

  private fun computeNormalizedRms(pcmBytes: ByteArray, length: Int): Float {
    var sumSquares = 0.0
    var sampleCount = 0
    var i = 0
    while (i + 1 < length) {
      val low = pcmBytes[i].toInt() and 0xFF
      val high = pcmBytes[i + 1].toInt()
      val sample = (high shl 8) or low
      val norm = sample / 32768.0
      sumSquares += norm * norm
      sampleCount++
      i += 2
    }
    if (sampleCount == 0) return 0f
    val rms = sqrt(sumSquares / sampleCount).toFloat()
    return (rms * 3.2f).coerceIn(0f, 1f)
  }
}
