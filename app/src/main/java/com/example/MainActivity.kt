package com.example

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.NetworkUrlInfo
import com.example.ui.components.QrCodeDialog
import com.example.ui.screens.BridgeScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.ScrobblesScreen
import com.example.ui.theme.WaveBridgeTheme
import com.example.ui.viewmodel.BridgeViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object BridgeRoute

@Serializable object ScrobblesRoute

@Serializable object GuideRoute

private data class NavDestinationItem(
  val route: Any,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

class MainActivity : ComponentActivity() {
  private val viewModel: BridgeViewModel by viewModels()
  private var launchProjectionAfterAudioPermission = false

  private val postNotificationLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) {
      viewModel.refreshAll()
    }

  private val mediaProjectionLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK && result.data != null) {
        viewModel.startAudioCaptureWithProjection(result.resultCode, result.data!!)
      }
    }

  private val recordAudioLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      viewModel.refreshAll()
      if (granted && launchProjectionAfterAudioPermission) {
        launchProjectionAfterAudioPermission = false
        launchMediaProjectionPrompt()
      } else {
        launchProjectionAfterAudioPermission = false
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      WaveBridgeTheme {
        WaveBridgeAppRoot(
          viewModel = viewModel,
          onCopyUrl = ::copyToClipboard,
          onShareExport = ::shareExportText,
          onOpenNotificationListenerSettings = ::openNotificationListenerSettings,
          onOpenAppInfoSettings = ::openAppInfoSettings,
          onRequestPostNotifications = {
            postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          },
          onRequestBatteryOptimization = ::requestIgnoreBatteryOptimizations,
          onRequestRecordAudio = {
            launchProjectionAfterAudioPermission = false
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
          },
          onEnableLiveAudio = ::handleEnableLiveAudio,
        )
      }
    }
  }

  private fun handleEnableLiveAudio() {
    val audioGranted =
      ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    if (!audioGranted) {
      launchProjectionAfterAudioPermission = true
      recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
    } else {
      launchMediaProjectionPrompt()
    }
  }

  private fun launchMediaProjectionPrompt() {
    val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    mediaProjectionLauncher.launch(mgr.createScreenCaptureIntent())
  }

  private fun openNotificationListenerSettings() {
    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
  }

  private fun openAppInfoSettings() {
    val intent =
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
      }
    startActivity(intent)
  }

  private fun requestIgnoreBatteryOptimizations() {
    val intent =
      Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:$packageName")
      }
    try {
      startActivity(intent)
    } catch (_: Exception) {
      startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
  }

  private fun copyToClipboard(text: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("WaveBridge URL", text))
  }

  private fun shareExportText(content: String, title: String) {
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, content)
      }
    startActivity(Intent.createChooser(sendIntent, title))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaveBridgeAppRoot(
  viewModel: BridgeViewModel,
  onCopyUrl: (String) -> Unit,
  onShareExport: (String, String) -> Unit,
  onOpenNotificationListenerSettings: () -> Unit,
  onOpenAppInfoSettings: () -> Unit,
  onRequestPostNotifications: () -> Unit,
  onRequestBatteryOptimization: () -> Unit,
  onRequestRecordAudio: () -> Unit,
  onEnableLiveAudio: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var qrUrlInfo by remember { mutableStateOf<NetworkUrlInfo?>(null) }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAll()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val navItems =
    listOf(
      NavDestinationItem(
        route = BridgeRoute,
        label = stringResource(R.string.nav_bridge),
        selectedIcon = Icons.Rounded.Podcasts,
        unselectedIcon = Icons.Outlined.Podcasts,
        testTag = "nav_bridge_tab",
      ),
      NavDestinationItem(
        route = ScrobblesRoute,
        label = stringResource(R.string.nav_scrobbles),
        selectedIcon = Icons.Rounded.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_scrobbles_tab",
      ),
      NavDestinationItem(
        route = GuideRoute,
        label = stringResource(R.string.nav_guide),
        selectedIcon = Icons.Rounded.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_guide_tab",
      ),
    )

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 700.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "WaveBridge",
              style = MaterialTheme.typography.headlineMedium,
            )
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background,
            ),
        )
      },
      bottomBar = {
        if (!useRail) {
          NavigationBar {
            navItems.forEach { item ->
              val selected =
                when (item.route) {
                  is BridgeRoute -> currentDestination?.hasRoute<BridgeRoute>() == true
                  is ScrobblesRoute -> currentDestination?.hasRoute<ScrobblesRoute>() == true
                  is GuideRoute -> currentDestination?.hasRoute<GuideRoute>() == true
                  else -> false
                }
              NavigationBarItem(
                selected = selected,
                onClick = {
                  navController.navigate(item.route) {
                    popUpTo(BridgeRoute) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useRail) {
          NavigationRail {
            navItems.forEach { item ->
              val selected =
                when (item.route) {
                  is BridgeRoute -> currentDestination?.hasRoute<BridgeRoute>() == true
                  is ScrobblesRoute -> currentDestination?.hasRoute<ScrobblesRoute>() == true
                  is GuideRoute -> currentDestination?.hasRoute<GuideRoute>() == true
                  else -> false
                }
              NavigationRailItem(
                selected = selected,
                onClick = {
                  navController.navigate(item.route) {
                    popUpTo(BridgeRoute) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
          Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()) {
            NavHost(
              navController = navController,
              startDestination = BridgeRoute,
            ) {
              composable<BridgeRoute> {
                BridgeScreen(
                  state = uiState,
                  onToggleServer = viewModel::toggleServer,
                  onCopyUrl = { url ->
                    onCopyUrl(url)
                    scope.launch { snackbarHostState.showSnackbar("Copied $url") }
                  },
                  onShowQr = { info -> qrUrlInfo = info },
                  onSelectSession = viewModel::selectSession,
                  onTogglePlayPause = viewModel::togglePlayPause,
                  onPrevious = viewModel::previousTrack,
                  onNext = viewModel::nextTrack,
                  onSeekTo = viewModel::seekTo,
                  onEnableLiveAudio = onEnableLiveAudio,
                  onDisableLiveAudio = viewModel::stopAudioCapture,
                  onOpenNotificationListenerSettings = onOpenNotificationListenerSettings,
                  onOpenAppInfoSettings = onOpenAppInfoSettings,
                  onRequestPostNotifications = onRequestPostNotifications,
                  onRequestBatteryOptimization = onRequestBatteryOptimization,
                  onRequestRecordAudio = onRequestRecordAudio,
                )
              }

              composable<ScrobblesRoute> {
                ScrobblesScreen(
                  scrobbles = uiState.scrobbles,
                  onExportJson = {
                    scope.launch {
                      val data = viewModel.exportScrobbles(asCsv = false)
                      onShareExport(data, "WaveBridge Scrobbles (JSON)")
                    }
                  },
                  onExportCsv = {
                    scope.launch {
                      val data = viewModel.exportScrobbles(asCsv = true)
                      onShareExport(data, "WaveBridge Scrobbles (CSV)")
                    }
                  },
                  onClearHistory = {
                    viewModel.clearScrobbles()
                    scope.launch { snackbarHostState.showSnackbar("Scrobble history cleared") }
                  },
                )
              }

              composable<GuideRoute> {
                GuideScreen(
                  settings = uiState.settings,
                  onSavePort = { newPort ->
                    viewModel.updatePort(newPort)
                    scope.launch { snackbarHostState.showSnackbar("Server port set to $newPort") }
                  },
                  onToggleAutoStart = viewModel::updateAutoStart,
                  onUpdateScrobbleDelay = viewModel::updateScrobbleDelay,
                )
              }
            }
          }
        }
      }
    }
  }

  qrUrlInfo?.let { info ->
    QrCodeDialog(
      url = info.url,
      label = "${info.label} (${info.interfaceName})",
      onCopy = {
        onCopyUrl(info.url)
        scope.launch { snackbarHostState.showSnackbar("Copied ${info.url}") }
      },
      onDismiss = { qrUrlInfo = null },
    )
  }
}
