package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.network.QrCodeHelper

@Composable
fun QrCodeDialog(
  url: String,
  label: String,
  onCopy: () -> Unit,
  onDismiss: () -> Unit,
) {
  val qrBitmap = remember(url) { QrCodeHelper.generateQrBitmap(url, 560) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("Scan from PC or Tablet", style = MaterialTheme.typography.titleLarge)
            Text(
              label,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_qr_dialog_button"),
          ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close QR Code dialog")
          }
        }

        Spacer(Modifier.height(16.dp))

        Box(
          modifier =
            Modifier.size(230.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color.White)
              .padding(14.dp),
          contentAlignment = Alignment.Center,
        ) {
          if (qrBitmap != null) {
            Image(
              bitmap = qrBitmap.asImageBitmap(),
              contentDescription = "QR code for $url",
              modifier = Modifier.size(200.dp),
            )
          } else {
            Text("QR Error", color = Color.Black)
          }
        }

        Spacer(Modifier.height(14.dp))

        Text(
          text = url,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        Button(
          onClick = onCopy,
          modifier = Modifier.fillMaxWidth().testTag("qr_copy_link_button"),
        ) {
          Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.size(8.dp))
          Text("Copy Dashboard URL")
        }
      }
    }
  }
}
