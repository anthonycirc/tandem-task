package dev.thingscompanion.app

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thingscompanion.app.ui.ThemeMode
import dev.thingscompanion.app.ui.ThingsApp
import dev.thingscompanion.app.ui.ThingsViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ThingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleLaunch(intent)
        requestNotificationPermission()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val dark = when (state.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                ThingsApp(viewModel)
            }
        }
    }

    /** Back in the app: send what waits and pull, in the background. */
    override fun onRestart() {
        super.onRestart()
        viewModel.refresh()
    }

    /** Leaving the app: changes still queued are handed to WorkManager. */
    override fun onStop() {
        super.onStop()
        viewModel.onAppStopped()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunch(intent)
    }

    /** Reminders are posted as notifications, which Android 13+ asks the user to allow once. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Widget taps: open the Inbox form, or a to-do's detail. */
    private fun handleLaunch(intent: Intent?) {
        intent ?: return
        // Demo mode for screenshots, from adb (am start ... --ez demo true) on a development build only:
        // another app can't switch a published build into it.
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (debuggable && intent.getBooleanExtra(EXTRA_DEMO, false)) viewModel.enterDemo()
        viewModel.handleLaunch(
            openAdd = intent.getBooleanExtra(EXTRA_OPEN_ADD, false),
            taskId = intent.getStringExtra(EXTRA_TASK_ID),
        )
    }

    companion object {
        const val EXTRA_OPEN_ADD = "open_add"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_DEMO = "demo"
    }
}
