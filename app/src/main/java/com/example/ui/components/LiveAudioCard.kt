package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LiveEmerald

@Composable
fun LiveAudioCard(
  liveAudioActive: Boolean,
  audioLevel: Float,
  statusNote: String,
  onEnableLiveAudio: () -> Unit,
  onDisableLiveAudio: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    border =
      BorderStroke(
        1.dp,
        if (liveAudioActive) LiveEmerald.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Live Audio Broadcast", style = MaterialTheme.typography.titleLarge)
          Text(
            text = statusNote,
            style = MaterialTheme.typography.bodySmall,
            color =
              if (liveAudioActive) LiveEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.width(10.dp))
        Button(
          onClick = if (liveAudioActive) onDisableLiveAudio else onEnableLiveAudio,
          modifier = Modifier.height(48.dp).testTag("toggle_live_audio_button"),
          colors =
            ButtonDefaults.buttonColors(
              containerColor =
                if (liveAudioActive) {
                  MaterialTheme.colorScheme.errorContainer
                } else {
                  MaterialTheme.colorScheme.secondary
                },
              contentColor =
                if (liveAudioActive) {
                  MaterialTheme.colorScheme.onErrorContainer
                } else {
                  MaterialTheme.colorScheme.onSecondary
                },
            ),
        ) {
          Icon(
            imageVector = if (liveAudioActive) Icons.Rounded.Stop else Icons.Rounded.GraphicEq,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(if (liveAudioActive) "Stop Audio" else "Enable Live Audio Stream")
        }
      }

      if (liveAudioActive) {
        val barInactive = MaterialTheme.colorScheme.surfaceContainerHigh
        Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
          val bars = 28
          val gap = 4.dp.toPx()
          val barWidth = (size.width - gap * (bars - 1)) / bars
          val litCount = (audioLevel * bars).toInt().coerceIn(0, bars)
          for (i in 0 until bars) {
            val x = i * (barWidth + gap)
            val lit = i < litCount
            val color: Color =
              when {
                !lit -> barInactive
                i > bars * 0.8f -> AmberWarn
                i > bars * 0.55f -> ElectricCyan
                else -> LiveEmerald
              }
            val barH = if (lit) size.height * 0.85f else size.height * 0.35f
            drawRoundRect(
              color = color,
              topLeft = Offset(x, (size.height - barH) / 2f),
              size = Size(barWidth, barH),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
          }
        }
      } else {
        Text(
          text =
            "Optional: uses MediaProjection + AudioPlaybackCapture to stream PCM audio over WebSocket. Metadata & remote control keep working even if disabled or blocked by a DRM app.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
