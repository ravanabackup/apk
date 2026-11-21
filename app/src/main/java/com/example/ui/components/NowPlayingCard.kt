package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MediaSessionInfo
import com.example.model.PlaybackStatus
import com.example.ui.theme.LiveEmerald

@Composable
fun NowPlayingCard(
  activeSession: MediaSessionInfo?,
  sessions: List<MediaSessionInfo>,
  artwork: Bitmap?,
  onSelectSession: (String) -> Unit,
  onTogglePlayPause: () -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onSeekTo: (Long) -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Live Media Monitor", style = MaterialTheme.typography.titleLarge)
        if (activeSession != null) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color =
              if (activeSession.status == PlaybackStatus.PLAYING) {
                LiveEmerald.copy(alpha = 0.16f)
              } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
              },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint =
                  if (activeSession.status == PlaybackStatus.PLAYING) {
                    LiveEmerald
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = "${activeSession.appName} · ${activeSession.status.name}",
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (activeSession.status == PlaybackStatus.PLAYING) {
                    LiveEmerald
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
              )
            }
          }
        }
      }

      if (activeSession == null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              Icons.Rounded.MusicNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
            Column {
              Text("No active media session", style = MaterialTheme.typography.titleSmall)
              Text(
                "Grant Notification Access and play Spotify, YouTube Music, or any player.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(
            modifier =
              Modifier.size(74.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
          ) {
            if (artwork != null) {
              Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = "Album artwork for ${activeSession.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            } else {
              Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = activeSession.title,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = activeSession.artist,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (activeSession.album.isNotBlank()) {
              Text(
                text = activeSession.album,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        // Progress Slider
        val duration = activeSession.durationMs.coerceAtLeast(1L)
        var dragging by remember { mutableStateOf(false) }
        var dragPos by remember { mutableFloatStateOf(0f) }
        val displayPos =
          if (dragging) dragPos.toLong() else activeSession.positionMs.coerceIn(0L, duration)

        Column {
          Slider(
            value = displayPos.toFloat(),
            onValueChange = {
              dragging = true
              dragPos = it
            },
            onValueChangeFinished = {
              dragging = false
              onSeekTo(dragPos.toLong())
            },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth().testTag("media_seek_slider"),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(formatDuration(displayPos), style = MaterialTheme.typography.labelSmall)
            Text(
              formatDuration(activeSession.durationMs),
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }

        // Transport Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(48.dp).testTag("media_prev_button"),
          ) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous track")
          }
          Spacer(Modifier.width(16.dp))
          FilledIconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(56.dp).clip(CircleShape).testTag("media_play_pause_button"),
            colors =
              IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
          ) {
            Icon(
              imageVector =
                if (activeSession.status == PlaybackStatus.PLAYING) {
                  Icons.Rounded.Pause
                } else {
                  Icons.Rounded.PlayArrow
                },
              contentDescription = "Play or pause",
              modifier = Modifier.size(28.dp),
            )
          }
          Spacer(Modifier.width(16.dp))
          IconButton(
            onClick = onNext,
            modifier = Modifier.size(48.dp).testTag("media_next_button"),
          ) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next track")
          }
        }

        if (sessions.size > 1) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            sessions.forEach { session ->
              FilterChip(
                selected = session.sessionId == activeSession.sessionId,
                onClick = { onSelectSession(session.sessionId) },
                label = { Text(session.appName) },
                modifier = Modifier.testTag("session_chip_${session.packageName}"),
              )
            }
          }
        }
      }
    }
  }
}

fun formatDuration(ms: Long): String {
  if (ms <= 0L) return "0:00"
  val totalSec = ms / 1000L
  val min = totalSec / 60L
  val sec = totalSec % 60L
  return "%d:%02d".format(min, sec)
}
