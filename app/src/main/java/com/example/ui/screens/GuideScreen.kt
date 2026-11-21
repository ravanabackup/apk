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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.BridgeSettings
import kotlin.math.roundToInt

@Composable
fun GuideScreen(
  settings: BridgeSettings,
  onSavePort: (Int) -> Unit,
  onToggleAutoStart: (Boolean) -> Unit,
  onUpdateScrobbleDelay: (Int) -> Unit,
) {
  var portInput by remember(settings.serverPort) { mutableStateOf(settings.serverPort.toString()) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item(key = "server_settings") {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text("Server & Scrobble Settings", style = MaterialTheme.typography.titleLarge)

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = portInput,
              onValueChange = { input ->
                portInput = input.filter { it.isDigit() }.take(5)
              },
              label = { Text("HTTP / WebSocket Port") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("port_input_field"),
            )
            Spacer(Modifier.width(12.dp))
            Button(
              onClick = {
                val parsed = portInput.toIntOrNull() ?: 8765
                onSavePort(parsed.coerceIn(1024, 65535))
              },
              modifier = Modifier.height(52.dp).testTag("apply_port_button"),
            ) {
              Text("Apply")
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Auto-start Bridge on launch", style = MaterialTheme.typography.titleSmall)
              Text(
                "Starts Ktor server automatically when WaveBridge opens",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = settings.autoStartServer,
              onCheckedChange = onToggleAutoStart,
              modifier = Modifier.testTag("autostart_switch"),
            )
          }

          Column {
            Text(
              "Scrobble threshold: ${settings.scrobbleDelaySec}s of playback",
              style = MaterialTheme.typography.titleSmall,
            )
            Slider(
              value = settings.scrobbleDelaySec.toFloat(),
              onValueChange = { onUpdateScrobbleDelay(it.roundToInt()) },
              valueRange = 2f..20f,
              steps = 17,
              modifier = Modifier.testTag("scrobble_delay_slider"),
            )
          }
        }
      }
    }

    item(key = "pc_guide") {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text("PC Connection & Android 16/17 Guide", style = MaterialTheme.typography.titleLarge)
          GuideStep(
            number = "1",
            title = "Unlock Notification Access (Sideloaded APKs)",
            body =
              "If Android 15/16/17 grays out Notification Access under Restricted Settings, open App Info -> tap ⋮ in the top-right -> Allow restricted settings, then enable WaveBridge in Notification Access.",
          )
          GuideStep(
            number = "2",
            title = "Start the Bridge Server",
            body =
              "Tap Start on the Bridge tab. WaveBridge binds Ktor CIO to 0.0.0.0:${settings.serverPort} with WakeLock and WifiLock so it stays reachable even with the screen off.",
          )
          GuideStep(
            number = "3",
            title = "Open on PC (Wi-Fi LAN or Tailscale)",
            body =
              "On your PC browser, visit the Wi-Fi LAN link (192.168.x.x:${settings.serverPort}) or Tailscale IP (100.x.y.z:${settings.serverPort}) or scan the QR code.",
          )
          GuideStep(
            number = "4",
            title = "Optional Live Audio Streaming",
            body =
              "Tap Enable Live Audio Stream on the Bridge tab to grant MediaProjection capture, then click 'Listen Live in Browser' on your PC. Note: DRM-protected apps may output silence while metadata & remote controls remain fully active.",
          )
        }
      }
    }
  }
}

@Composable
private fun GuideStep(number: String, title: String, body: String) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = "$number. $title",
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = body,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
