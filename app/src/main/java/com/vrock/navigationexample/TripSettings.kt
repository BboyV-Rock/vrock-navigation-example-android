package com.vrock.navigationexample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vrock.navigation.map.MapThemeConfig
import com.vrock.navigation.map.MapThemeMode
import com.vrock.navigation.map.MapThemeSchedule
import java.time.LocalTime

@Composable
internal fun TripSettings(
    theme: MapThemeConfig,
    appStopsTrip: Boolean,
    recenterEnabled: Boolean,
    onCancel: () -> Unit,
    onSave: (MapThemeConfig, Boolean, Boolean) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(theme.mode) }
    var darkStartsAt by rememberSaveable { mutableStateOf(theme.schedule.darkStartsAt.toString()) }
    var darkEndsAt by rememberSaveable { mutableStateOf(theme.schedule.darkEndsAt.toString()) }
    var stopFromApp by rememberSaveable { mutableStateOf(appStopsTrip) }
    var recenter by rememberSaveable { mutableStateOf(recenterEnabled) }
    val start = runCatching { LocalTime.parse(darkStartsAt) }.getOrNull()
    val end = runCatching { LocalTime.parse(darkEndsAt) }.getOrNull()

    Scaffold { padding ->
        Column(
            Modifier.fillMaxWidth().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.trip_settings), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.map_theme), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MapThemeMode.entries.forEach { option ->
                    val label = when (option) {
                        MapThemeMode.LIGHT -> R.string.theme_light
                        MapThemeMode.DARK -> R.string.theme_dark
                        MapThemeMode.AUTO -> R.string.theme_auto
                    }
                    FilterChip(
                        selected = mode == option,
                        onClick = { mode = option },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            if (mode == MapThemeMode.AUTO) {
                Text(stringResource(R.string.theme_schedule_help))
                OutlinedTextField(
                    value = darkStartsAt,
                    onValueChange = { darkStartsAt = it },
                    label = { Text(stringResource(R.string.theme_dark_start)) },
                    singleLine = true,
                    isError = start == null,
                )
                OutlinedTextField(
                    value = darkEndsAt,
                    onValueChange = { darkEndsAt = it },
                    label = { Text(stringResource(R.string.theme_dark_end)) },
                    singleLine = true,
                    isError = end == null,
                )
            }
            TripSwitch(R.string.trip_recenter, recenter) { recenter = it }
            TripSwitch(R.string.stop_from_app, stopFromApp) { stopFromApp = it }
            if (stopFromApp) Text(stringResource(R.string.stop_from_app_help))
            Row {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel_palette)) }
                Button(
                    enabled = mode != MapThemeMode.AUTO || (start != null && end != null),
                    onClick = {
                        val schedule = if (start != null && end != null) MapThemeSchedule(start, end) else theme.schedule
                        onSave(MapThemeConfig(mode, schedule), stopFromApp, recenter)
                    },
                ) { Text(stringResource(R.string.save_palette)) }
            }
        }
    }
}

@Composable
private fun TripSwitch(label: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(label), Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
