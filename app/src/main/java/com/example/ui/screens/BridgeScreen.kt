package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.NetworkUrlInfo
import com.example.ui.components.LiveAudioCard
import com.example.ui.components.NowPlayingCard
import com.example.ui.components.PermissionHealthCard
import com.example.ui.components.ServerHeroCard
import com.example.ui.viewmodel.BridgeUiState

@Composable
fun BridgeScreen(
  state: BridgeUiState,
  onToggleServer: () -> Unit,
  onCopyUrl: (String) -> Unit,
  onShowQr: (NetworkUrlInfo) -> Unit,
  onSelectSession: (String) -> Unit,
  onTogglePlayPause: () -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onEnableLiveAudio: () -> Unit,
  onDisableLiveAudio: () -> Unit,
  onOpenNotificationListenerSettings: () -> Unit,
  onOpenAppInfoSettings: () -> Unit,
  onRequestPostNotifications: () -> Unit,
  onRequestBatteryOptimization: () -> Unit,
  onRequestRecordAudio: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item(key = "hero") {
      ServerHeroCard(
        serverRunning = state.serverRunning,
        port = state.serverPort,
        connectedClients = state.connectedClients,
        reachableUrls = state.reachableUrls,
        serverError = state.serverError,
        onToggleServer = onToggleServer,
        onCopyUrl = onCopyUrl,
        onShowQr = onShowQr,
      )
    }

    item(key = "now_playing") {
      NowPlayingCard(
        activeSession = state.activeSession,
        sessions = state.sessions,
        artwork = state.activeArtwork,
        onSelectSession = onSelectSession,
        onTogglePlayPause = onTogglePlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onSeekTo = onSeekTo,
      )
    }

    item(key = "live_audio") {
      LiveAudioCard(
        liveAudioActive = state.liveAudioActive,
        audioLevel = state.audioLevel,
        statusNote = state.audioStatusNote,
        onEnableLiveAudio = onEnableLiveAudio,
        onDisableLiveAudio = onDisableLiveAudio,
      )
    }

    item(key = "permissions") {
      PermissionHealthCard(
        health = state.permissions,
        onOpenNotificationListenerSettings = onOpenNotificationListenerSettings,
        onOpenAppInfoSettings = onOpenAppInfoSettings,
        onRequestPostNotifications = onRequestPostNotifications,
        onRequestBatteryOptimization = onRequestBatteryOptimization,
        onRequestRecordAudio = onRequestRecordAudio,
      )
    }
  }
}
