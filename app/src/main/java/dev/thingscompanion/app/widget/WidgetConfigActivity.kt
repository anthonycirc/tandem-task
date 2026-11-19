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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.width
import dev.thingscompanion.cloud.RgbColor
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                    var color by rememberSaveable { mutableStateOf(current.color) }
                    var customRgb by rememberSaveable { mutableIntStateOf(current.customRgb) }

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
                                Text(stringResource(R.string.widget_color), style = MaterialTheme.typography.labelLarge)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WidgetColor.entries.forEach { option ->
                                        FilterChip(
                                            selected = color == option,
                                            onClick = { color = option },
                                            label = { Text(stringResource(option.label)) },
                                            leadingIcon = option.tint(customRgb)?.let { swatch ->
                                                { Box(Modifier.size(14.dp).clip(RoundedCornerShape(7.dp)).background(swatch)) }
                                            },
                                        )
                                    }
                                }
                                if (color == WidgetColor.CUSTOM) CustomColorPicker(customRgb) { customRgb = it }
                                Text(stringResource(R.string.widget_opacity, opacity), style = MaterialTheme.typography.labelLarge)
                                Slider(
                                    value = opacity.toFloat(),
                                    onValueChange = { opacity = it.toInt() },
                                    valueRange = 0f..100f,
                                    steps = 9,
                                )
                                Preview(theme, opacity, color, customRgb)
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
                                onClick = { save(appWidgetId, ListWidget.Companion.Settings(listKey, theme, opacity, color, customRgb)) },
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
    private fun Preview(theme: WidgetTheme, opacity: Int, color: WidgetColor, customRgb: Int) {
        val dark = when (theme) {
            WidgetTheme.SYSTEM -> isSystemInDarkTheme()
            WidgetTheme.LIGHT -> false
            WidgetTheme.DARK -> true
        }
        val alpha = opacity / 100f
        val tint = color.tint(customRgb)
        val onTint = if (color == WidgetColor.CUSTOM && RgbColor.isLight(customRgb)) Color(0xFF1C1B1F) else Color.White
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF6B8E5A)),
        ) {
            Box(
                Modifier.fillMaxSize()
                    .background((tint ?: if (dark) Color(0xFF1F2024) else Color.White).copy(alpha = alpha))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    stringResource(R.string.list_today),
                    color = if (tint != null) onTint else if (dark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    /** Red, green and blue sliders and the colour as hex, which can be typed too. */
    @androidx.compose.runtime.Composable
    private fun CustomColorPicker(rgb: Int, onChange: (Int) -> Unit) {
        val (red, green, blue) = RgbColor.channels(rgb)
        // The typed text stays as typed; it is rewritten only when the sliders change the colour.
        var hex by rememberSaveable { mutableStateOf(RgbColor.format(rgb)) }
        LaunchedEffect(rgb) { if (RgbColor.parse(hex) != rgb) hex = RgbColor.format(rgb) }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                Triple(R.string.widget_red, red, Color(0xFFE5484D)),
                Triple(R.string.widget_green, green, Color(0xFF3C9D5D)),
                Triple(R.string.widget_blue, blue, Color(0xFF2F5BB7)),
            ).forEachIndexed { index, (label, value, track) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(label, value), Modifier.width(96.dp), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = value.toFloat(),
                        onValueChange = { v ->
                            val c = intArrayOf(red, green, blue).also { it[index] = v.toInt() }
                            onChange(RgbColor.of(c[0], c[1], c[2]))
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = track, activeTrackColor = track),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            OutlinedTextField(
                value = hex,
                onValueChange = { text ->
                    hex = text
                    RgbColor.parse(text)?.let(onChange)
                },
                label = { Text(stringResource(R.string.widget_hex)) },
                isError = RgbColor.parse(hex) == null,
                singleLine = true,
                leadingIcon = { Box(Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF000000.toInt() or rgb))) },
                modifier = Modifier.fillMaxWidth(),
            )
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
