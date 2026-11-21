package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ScrobbleDto
import com.example.ui.components.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScrobblesScreen(
  scrobbles: List<ScrobbleDto>,
  onExportJson: () -> Unit,
  onExportCsv: () -> Unit,
  onClearHistory: () -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val filtered =
    remember(scrobbles, query) {
      if (query.isBlank()) {
        scrobbles
      } else {
        scrobbles.filter {
          it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.appName.contains(query, ignoreCase = true)
        }
      }
    }

  val timeFmt = remember { SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()) }

  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Spacer(Modifier.height(4.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text("Scrobble Log", style = MaterialTheme.typography.headlineSmall)
        Text(
          "${scrobbles.size} tracks captured locally",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(
          onClick = onExportJson,
          enabled = scrobbles.isNotEmpty(),
          modifier = Modifier.height(48.dp).testTag("export_json_button"),
        ) {
          Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("JSON")
        }
        FilledTonalButton(
          onClick = onExportCsv,
          enabled = scrobbles.isNotEmpty(),
          modifier = Modifier.height(48.dp).testTag("export_csv_button"),
        ) {
          Text("CSV")
        }
        IconButton(
          onClick = onClearHistory,
          enabled = scrobbles.isNotEmpty(),
          modifier = Modifier.size(48.dp).testTag("clear_scrobbles_button"),
        ) {
          Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear scrobble history")
        }
      }
    }

    OutlinedTextField(
      value = query,
      onValueChange = { query = it },
      placeholder = { Text("Search title, artist, or app...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      singleLine = true,
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier.fillMaxWidth().testTag("scrobble_search_input"),
    )

    if (filtered.isEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
          )
          Text(
            text = if (scrobbles.isEmpty()) "No scrobbles yet" else "No matching tracks",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Tracks are logged automatically after playing for a few seconds.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(filtered, key = { it.id }) { item ->
          Card(
            shape = MaterialTheme.shapes.medium,
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text =
                    if (item.album.isNotBlank()) "${item.artist} · ${item.album}" else item.artist,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                  Text(
                    text = item.appName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                  text =
                    "${timeFmt.format(Date(item.playedAtMs))} · ${formatDuration(item.durationMs)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}
