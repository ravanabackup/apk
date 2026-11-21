package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.NetworkUrlInfo
import com.example.ui.theme.LiveEmerald

@Composable
fun ServerHeroCard(
  serverRunning: Boolean,
  port: Int,
  connectedClients: Int,
  reachableUrls: List<NetworkUrlInfo>,
  serverError: String?,
  onToggleServer: () -> Unit,
  onCopyUrl: (String) -> Unit,
  onShowQr: (NetworkUrlInfo) -> Unit,
) {
  val statusColor by
    animateColorAsState(
      targetValue =
        if (serverRunning) LiveEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
      label = "serverStatusColor",
    )

  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    border =
      BorderStroke(
        1.dp,
        if (serverRunning) {
          MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        } else {
          MaterialTheme.colorScheme.outline
        },
      ),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors =
                if (serverRunning) {
                  listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                    MaterialTheme.colorScheme.surfaceContainer,
                  )
                } else {
                  listOf(
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.surfaceContainerLow,
                  )
                }
            )
          )
          .padding(18.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(10.dp).clip(CircleShape).background(statusColor)
              )
              Text(
                text = if (serverRunning) "BRIDGE LIVE · 0.0.0.0:$port" else "BRIDGE OFFLINE",
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
              )
            }
            Spacer(Modifier.height(4.dp))
            Text(
              text =
                if (serverRunning) {
                  "Open any link below on your PC browser"
                } else {
                  "Start server to share playback with PC"
                },
              style = MaterialTheme.typography.titleMedium,
            )
          }

          Spacer(Modifier.width(12.dp))

          Button(
            onClick = onToggleServer,
            modifier = Modifier.height(48.dp).testTag("toggle_server_button"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor =
                  if (serverRunning) {
                    MaterialTheme.colorScheme.errorContainer
                  } else {
                    MaterialTheme.colorScheme.primary
                  },
                contentColor =
                  if (serverRunning) {
                    MaterialTheme.colorScheme.onErrorContainer
                  } else {
                    MaterialTheme.colorScheme.onPrimary
                  },
              ),
          ) {
            Icon(
              Icons.Rounded.PowerSettingsNew,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(if (serverRunning) "Stop" else "Start")
          }
        }

        if (serverError != null) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = "Server error: $serverError",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.fillMaxWidth().padding(10.dp),
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            Icons.Rounded.Computer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text =
              "$connectedClients PC browser${if (connectedClients == 1) "" else "s"} connected via WebSocket",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (reachableUrls.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
          ) {
            Text(
              text = "No active Wi-Fi or Tailscale IPv4 address detected. Connect to Wi-Fi, Hotspot, or Tailscale.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.fillMaxWidth().padding(12.dp),
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            reachableUrls.forEachIndexed { idx, info ->
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector =
                      when {
                        info.isTailscale -> Icons.Rounded.VpnLock
                        info.isWifi -> Icons.Rounded.Wifi
                        else -> Icons.Rounded.Lan
                      },
                    contentDescription = info.label,
                    tint =
                      if (info.isTailscale) {
                        MaterialTheme.colorScheme.secondary
                      } else {
                        MaterialTheme.colorScheme.primary
                      },
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "${info.label} (${info.interfaceName})",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = info.url,
                      style = MaterialTheme.typography.labelMedium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  IconButton(
                    onClick = { onCopyUrl(info.url) },
                    modifier = Modifier.testTag("copy_url_button_$idx"),
                  ) {
                    Icon(
                      Icons.Rounded.ContentCopy,
                      contentDescription = "Copy ${info.url}",
                      modifier = Modifier.size(20.dp),
                    )
                  }
                  IconButton(
                    onClick = { onShowQr(info) },
                    modifier = Modifier.testTag("qr_url_button_$idx"),
                  ) {
                    Icon(
                      Icons.Rounded.QrCode2,
                      contentDescription = "Show QR Code for ${info.url}",
                      modifier = Modifier.size(22.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
