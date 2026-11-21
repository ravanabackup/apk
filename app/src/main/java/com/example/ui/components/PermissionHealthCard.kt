package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.PermissionHealthState
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.LiveEmerald

@Composable
fun PermissionHealthCard(
  health: PermissionHealthState,
  onOpenNotificationListenerSettings: () -> Unit,
  onOpenAppInfoSettings: () -> Unit,
  onRequestPostNotifications: () -> Unit,
  onRequestBatteryOptimization: () -> Unit,
  onRequestRecordAudio: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
          Text("Permissions & Health Check", style = MaterialTheme.typography.titleLarge)
          Text(
            "Android 15/16/17 background & media access readiness",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = RoundedCornerShape(999.dp),
          color =
            if (health.coreBridgeReady) {
              LiveEmerald.copy(alpha = 0.16f)
            } else {
              AmberWarn.copy(alpha = 0.16f)
            },
        ) {
          Text(
            text = if (health.coreBridgeReady) "READY" else "SETUP NEEDED",
            style = MaterialTheme.typography.labelSmall,
            color = if (health.coreBridgeReady) LiveEmerald else AmberWarn,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      // 1. Notification Access + Restricted Settings Hint
      HealthRow(
        icon = Icons.Rounded.NotificationsActive,
        title = "Notification Access",
        subtitle = "Required for MediaSessionManager to read & control Spotify, YouTube, etc.",
        granted = health.notificationAccessGranted,
        optional = false,
        actionLabel = if (health.notificationAccessGranted) "Review" else "Enable",
        testTag = "perm_notification_listener_button",
        onAction = onOpenNotificationListenerSettings,
      )

      if (!health.notificationAccessGranted) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = AmberWarn.copy(alpha = 0.11f),
          border = BorderStroke(1.dp, AmberWarn.copy(alpha = 0.35f)),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = AmberWarn,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text =
                  "Sideloaded APK? Android 15/16/17 blocks Notification Access by default. Tap Open App Info below -> ⋮ (three dots, top-right) -> Allow restricted settings, then enable Notification Access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
              onClick = onOpenAppInfoSettings,
              modifier = Modifier.height(48.dp).testTag("open_app_info_button"),
            ) {
              Icon(
                Icons.Rounded.SettingsApplications,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text("Open App Info (⋮ Allow Restricted)")
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // 2. Post Notifications
      HealthRow(
        icon = Icons.Rounded.NotificationsNone,
        title = "Post Notifications",
        subtitle = "Shows persistent Foreground Service status while screen is off.",
        granted = health.postNotificationsGranted,
        optional = false,
        actionLabel = if (health.postNotificationsGranted) "Granted" else "Allow",
        testTag = "perm_post_notifications_button",
        onAction = onRequestPostNotifications,
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // 3. Unrestricted Battery Usage
      HealthRow(
        icon = Icons.Rounded.BatteryChargingFull,
        title = "Unrestricted Battery Usage",
        subtitle = "Prevents Doze from suspending the Ktor server when screen is locked.",
        granted = health.batteryOptimizationIgnored,
        optional = false,
        actionLabel = if (health.batteryOptimizationIgnored) "Unrestricted" else "Optimize Off",
        testTag = "perm_battery_button",
        onAction = onRequestBatteryOptimization,
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // 4. Optional Microphone / Audio Playback Capture
      HealthRow(
        icon = Icons.Rounded.Mic,
        title = "Audio Capture (Optional)",
        subtitle = "Only required if you enable Live Audio Stream to your PC browser.",
        granted = health.recordAudioGranted,
        optional = true,
        actionLabel = if (health.recordAudioGranted) "Granted" else "Grant",
        testTag = "perm_record_audio_button",
        onAction = onRequestRecordAudio,
      )
    }
  }
}

@Composable
private fun HealthRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  granted: Boolean,
  optional: Boolean,
  actionLabel: String,
  testTag: String,
  onAction: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint =
        when {
          granted -> LiveEmerald
          optional -> MaterialTheme.colorScheme.secondary
          else -> AmberWarn
        },
      modifier = Modifier.size(22.dp),
    )

    Column(modifier = Modifier.weight(1f)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Icon(
          imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
          contentDescription = if (granted) "$title granted" else "$title needs attention",
          tint =
            when {
              granted -> LiveEmerald
              optional -> MaterialTheme.colorScheme.onSurfaceVariant
              else -> AmberWarn
            },
          modifier = Modifier.size(15.dp),
        )
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    FilledTonalButton(
      onClick = onAction,
      enabled = !granted || title == "Notification Access",
      modifier = Modifier.height(48.dp).testTag(testTag),
    ) {
      Text(actionLabel, style = MaterialTheme.typography.labelLarge)
    }
  }
}
