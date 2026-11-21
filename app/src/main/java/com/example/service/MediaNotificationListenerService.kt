package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.WaveBridgeApplication

class MediaNotificationListenerService : NotificationListenerService() {

  override fun onListenerConnected() {
    super.onListenerConnected()
    val app = applicationContext as? WaveBridgeApplication ?: return
    app.container.mediaMonitor.startMonitoring()
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    val app = applicationContext as? WaveBridgeApplication ?: return
    app.container.mediaMonitor.refreshNow()
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    // MediaSessionManager callbacks handle active session updates directly.
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    // MediaSessionManager callbacks handle session teardown directly.
  }
}
