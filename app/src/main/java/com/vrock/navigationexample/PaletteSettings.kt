package com.vrock.navigationexample

import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vrock.navigation.ui.NavigationColors
import com.vrock.navigationexample.ui.theme.VRockNavigationExampleTheme

private enum class PaletteColor(
    val label: Int,
    val read: (NavigationColors) -> Int?,
    val write: (NavigationColors, Int?) -> NavigationColors,
    val optional: Boolean = false,
) {
    ROUTE(R.string.color_route, { it.route }, { colors, value -> colors.copy(route = requireNotNull(value)) }),
    POSITION(R.string.color_position, { it.position }, { colors, value -> colors.copy(position = requireNotNull(value)) }),
    MANEUVER_BACKGROUND(R.string.color_maneuver_background, { it.maneuverBackground },
        { colors, value -> colors.copy(maneuverBackground = requireNotNull(value)) }),
    MANEUVER_TEXT(R.string.color_maneuver_text, { it.maneuverText },
        { colors, value -> colors.copy(maneuverText = requireNotNull(value), maneuverIcon = requireNotNull(value)) }),
    MAP_BUTTON_BACKGROUND(R.string.color_map_button_background, { it.mapButtonBackground },
        { colors, value -> colors.copy(mapButtonBackground = requireNotNull(value)) }),
    MAP_BUTTON_CONTENT(R.string.color_map_button_content, { it.mapButtonContent },
        { colors, value -> colors.copy(mapButtonContent = requireNotNull(value)) }),
    TRIP_BACKGROUND(R.string.color_trip_background, { it.tripPanel.background },
        { colors, value -> colors.copy(tripPanel = colors.tripPanel.copy(background = requireNotNull(value))) }),
    TRIP_TEXT(R.string.color_trip_text, { it.tripPanel.primaryText },
        { colors, value -> colors.copy(tripPanel = colors.tripPanel.copy(primaryText = requireNotNull(value))) }),
    SCREEN_BACKGROUND(R.string.color_screen_background, { it.screenBackground },
        { colors, value -> colors.copy(screenBackground = value) }, optional = true),
}

internal fun readPalette(preferences: SharedPreferences): NavigationColors =
    PaletteColor.entries.fold(NavigationColors()) { colors, field ->
        if (preferences.contains(field.name)) field.write(colors, preferences.getInt(field.name, 0)) else colors
    }

internal fun savePalette(preferences: SharedPreferences, colors: NavigationColors) {
    val editor = preferences.edit()
    PaletteColor.entries.forEach { field ->
        val value = field.read(colors)
        if (value == null) editor.remove(field.name) else editor.putInt(field.name, value)
    }
    editor.apply()
}

private fun NavigationColors.colorValues() = ArrayList(PaletteColor.entries.map { it.read(this) })

@Composable
internal fun PaletteSettings(
    colors: NavigationColors,
    onCancel: () -> Unit,
    onSave: (NavigationColors) -> Unit,
) {
    var values by rememberSaveable { mutableStateOf(colors.colorValues()) }
    var editing by rememberSaveable { mutableStateOf<PaletteColor?>(null) }

    editing?.let { field ->
        ColorPickerDialog(
            title = stringResource(field.label),
            initialColor = values[field.ordinal],
            onDismiss = { editing = null },
            onSelect = { color ->
                values = ArrayList(values).apply { set(field.ordinal, color) }
                editing = null
            },
            useThemeDefault = field.optional,
        )
    }
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.palette_settings), style = MaterialTheme.typography.headlineSmall)
                Button(
                    onClick = {
                        val updated = PaletteColor.entries.foldIndexed(colors) { index, palette, field ->
                            field.write(palette, values[index])
                        }
                        onSave(updated)
                    },
                ) { Text(stringResource(R.string.save_palette)) }
            }
            Text(stringResource(R.string.palette_help))
            Row {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel_palette)) }
                TextButton(onClick = { values = NavigationColors().colorValues() }) {
                    Text(stringResource(R.string.reset_palette))
                }
            }
            PaletteColor.entries.forEachIndexed { index, field ->
                OutlinedButton(
                    onClick = { editing = field },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(field.label), modifier = Modifier.weight(1f))
                    val color = values[index]
                    if (color == null) {
                        Text(stringResource(R.string.palette_theme_default))
                    } else {
                        ColorSwatch(color, Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initialColor: Int?,
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit,
    useThemeDefault: Boolean,
) {
    val initialHsv = FloatArray(3).apply {
        AndroidColor.colorToHSV(initialColor ?: AndroidColor.WHITE, this)
    }
    var hue by rememberSaveable { mutableFloatStateOf(initialHsv[0]) }
    var saturation by rememberSaveable { mutableFloatStateOf(initialHsv[1]) }
    var brightness by rememberSaveable { mutableFloatStateOf(initialHsv[2]) }
    val color = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColorSwatch(color, Modifier.fillMaxWidth().height(48.dp))
                Text(stringResource(R.string.palette_hue))
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Text(stringResource(R.string.palette_saturation))
                Slider(value = saturation, onValueChange = { saturation = it })
                Text(stringResource(R.string.palette_brightness))
                Slider(value = brightness, onValueChange = { brightness = it })
                if (useThemeDefault) {
                    TextButton(onClick = { onSelect(null) }) {
                        Text(stringResource(R.string.palette_theme_default))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(color) }) {
                Text(stringResource(R.string.palette_apply_color))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_palette)) }
        },
    )
}

@Composable
private fun ColorSwatch(color: Int, modifier: Modifier) {
    Box(modifier.background(Color(color)).border(1.dp, MaterialTheme.colorScheme.outline))
}

@Preview(showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PaletteSettingsPreview() {
    VRockNavigationExampleTheme {
        PaletteSettings(NavigationColors(), onCancel = {}, onSave = {})
    }
}
