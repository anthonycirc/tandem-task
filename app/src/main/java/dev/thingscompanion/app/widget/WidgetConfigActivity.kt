package dev.thingscompanion.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dev.thingscompanion.app.R
import dev.thingscompanion.app.data.readWidgetSnapshot
import dev.thingscompanion.cloud.WidgetSnapshot
import kotlinx.coroutines.launch

/** Picks a widget's list, theme and background opacity, when it is placed or reconfigured. */
class WidgetConfigActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out leaves the widget unplaced (or unchanged when reconfiguring).
        setResult(RESULT_CANCELED, resultIntent(appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val snapshot = readWidgetSnapshot(this)
        val choices = BUILT_IN_LISTS.map { it to listLabel(this, snapshot, it) } +
            snapshot?.projects.orEmpty().map { "${WidgetSnapshot.PROJECT_PREFIX}${it.id}" to getString(R.string.widget_project, it.title) }

        lifecycleScope.launch {
            val current = ListWidget.settings(this@WidgetConfigActivity, appWidgetId)
            setContent {
                MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                    var listKey by rememberSaveable { mutableStateOf(current.listKey) }
                    var theme by rememberSaveable { mutableStateOf(current.theme) }
                    var opacity by rememberSaveable { mutableIntStateOf(current.opacity) }

                    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.widget_settings)) }) }) { padding ->
                        Column(Modifier.fillMaxSize().padding(padding)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.widget_theme), style = MaterialTheme.typography.labelLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WidgetTheme.entries.forEach { option ->
                                        FilterChip(
                                            selected = theme == option,
                                            onClick = { theme = option },
                                            label = { Text(stringResource(option.label)) },
                                        )
                                    }
                                }
                                Text(stringResource(R.string.widget_opacity, opacity), style = MaterialTheme.typography.labelLarge)
                                Slider(
                                    value = opacity.toFloat(),
                                    onValueChange = { opacity = it.toInt() },
                                    valueRange = 0f..100f,
                                    steps = 9,
                                )
                                Preview(theme, opacity)
                                Text(stringResource(R.string.widget_pick_list), style = MaterialTheme.typography.labelLarge)
                            }
                            LazyColumn(Modifier.weight(1f)) {
                                items(choices, key = { it.first }) { (key, label) ->
                                    ListItem(
                                        headlineContent = { Text(label) },
                                        leadingContent = { RadioButton(selected = listKey == key, onClick = { listKey = key }) },
                                        modifier = Modifier.clickable { listKey = key },
                                    )
                                    HorizontalDivider()
                                }
                            }
                            Button(
                                onClick = { save(appWidgetId, ListWidget.Companion.Settings(listKey, theme, opacity)) },
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            ) { Text(stringResource(R.string.save)) }
                        }
                    }
                }
            }
        }
    }

    /** A strip showing the chosen background over a checkerboard-like backdrop, so the opacity is visible. */
    @androidx.compose.runtime.Composable
    private fun Preview(theme: WidgetTheme, opacity: Int) {
        val dark = when (theme) {
            WidgetTheme.SYSTEM -> isSystemInDarkTheme()
            WidgetTheme.LIGHT -> false
            WidgetTheme.DARK -> true
        }
        val alpha = opacity / 100f
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF6B8E5A)),
        ) {
            Box(
                Modifier.fillMaxSize()
                    .background((if (dark) Color(0xFF1F2024) else Color.White).copy(alpha = alpha))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    stringResource(R.string.list_today),
                    color = if (dark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    private fun save(appWidgetId: Int, settings: ListWidget.Companion.Settings) {
        lifecycleScope.launch {
            ListWidget.save(this@WidgetConfigActivity, appWidgetId, settings)
            setResult(RESULT_OK, resultIntent(appWidgetId))
            finish()
        }
    }

    private fun resultIntent(appWidgetId: Int) =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
