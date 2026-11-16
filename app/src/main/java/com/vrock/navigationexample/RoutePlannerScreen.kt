package com.vrock.navigationexample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vrock.navigation.core.NavigationCoordinate
import com.vrock.navigation.core.NavigationRoute
import com.vrock.navigation.map.NavigationMap
import com.vrock.navigation.ui.view.NavigationLaunchConfig
import com.vrock.navigation.ui.view.NavigationProvider
import com.vrock.navigationexample.ui.theme.VRockNavigationExampleTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RoutePlannerScreen(
    screen: TripScreen,
    config: NavigationLaunchConfig,
    points: List<NavigationCoordinate>,
    route: NavigationRoute?,
    buildingRoute: Boolean,
    routeFailed: Boolean,
    permissionDenied: Boolean,
    onScreenSelected: (TripScreen) -> Unit,
    onProviderSelected: (NavigationProvider) -> Unit,
    onPointsChanged: (List<NavigationCoordinate>) -> Unit,
    onBuild: () -> Unit,
    onSettings: () -> Unit,
    onTripSettings: () -> Unit,
    onStart: (simulateByTap: Boolean) -> Unit,
) {
    val missingGoogleKey = config.provider == NavigationProvider.GOOGLE && BuildConfig.GOOGLE_API_KEY.isBlank()
    val canStart = points.size >= 2 && !missingGoogleKey && !buildingRoute
    val geometry = route?.geometry.orEmpty()
    val colors = config.appearance.colors
    val selectPoint: (NavigationCoordinate) -> Unit = { point -> onPointsChanged(points + point) }

    Scaffold { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val map: @Composable (Modifier) -> Unit = { modifier ->
                when {
                    LocalInspectionMode.current || missingGoogleKey -> Surface(
                        modifier,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
                            Text(stringResource(
                                if (missingGoogleKey) R.string.google_key_required else R.string.demo_route,
                            ))
                        }
                    }
                    config.provider == NavigationProvider.GOOGLE -> GoogleRouteMap(
                        modifier, points, geometry, colors.route, demoPoints.origin, config.mapTheme, selectPoint,
                    )
                    else -> NavigationMap(
                        styleUrl = config.styleUrl,
                        modifier = modifier,
                        points = points,
                        route = geometry,
                        onPointSelected = selectPoint,
                        initialCenter = demoPoints.origin,
                        routeColor = colors.route,
                        theme = config.mapTheme,
                        styles = exampleMapLibreStyles,
                    )
                }
            }
            val controls: @Composable (Modifier) -> Unit = { modifier ->
                Column(
                    modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val hint = when {
                        points.isEmpty() -> stringResource(R.string.select_start)
                        points.size == 1 -> stringResource(R.string.select_destination)
                        else -> pluralStringResource(R.plurals.route_points, points.size, points.size)
                    }
                    Text(hint, style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onStart(false) }, enabled = canStart) {
                            Text(stringResource(R.string.start_gps))
                        }
                        Button(onClick = { onStart(true) }, enabled = canStart) {
                            Text(stringResource(R.string.start_demo))
                        }
                        OutlinedButton(onClick = onBuild, enabled = canStart) {
                            Text(stringResource(R.string.build_route))
                        }
                    }
                    if (buildingRoute) Text(stringResource(R.string.building_route))
                    if (routeFailed) Text(stringResource(R.string.preview_route_failed), color = MaterialTheme.colorScheme.error)
                    if (permissionDenied) Text(stringResource(R.string.precise_location_required), color = MaterialTheme.colorScheme.error)
                    FlowRow {
                        TextButton(
                            onClick = { onPointsChanged(listOf(demoPoints.origin, demoPoints.destination)) },
                            enabled = !missingGoogleKey,
                        ) { Text(stringResource(R.string.use_demo_points)) }
                        TextButton(onClick = { onPointsChanged(points.dropLast(1)) }, enabled = points.isNotEmpty()) {
                            Text(stringResource(R.string.undo_point))
                        }
                        TextButton(onClick = { onPointsChanged(emptyList()) }, enabled = points.isNotEmpty()) {
                            Text(stringResource(R.string.clear_points))
                        }
                    }
                    Text(stringResource(R.string.route_provider), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NavigationProvider.entries.forEach { option ->
                            val label = when (option) {
                                NavigationProvider.OSRM -> R.string.provider_osrm
                                NavigationProvider.GOOGLE -> R.string.provider_google
                            }
                            FilterChip(
                                selected = config.provider == option,
                                onClick = { onProviderSelected(option) },
                                label = { Text(stringResource(label)) },
                            )
                        }
                    }
                    Text(stringResource(R.string.trip_screen), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TripScreen.entries.forEach { option ->
                            FilterChip(
                                selected = screen == option,
                                onClick = { onScreenSelected(option) },
                                label = { Text(stringResource(option.label)) },
                            )
                        }
                    }
                    OutlinedButton(onClick = onTripSettings) { Text(stringResource(R.string.trip_settings)) }
                    OutlinedButton(onClick = onSettings) { Text(stringResource(R.string.palette_settings)) }
                }
            }
            if (maxWidth > maxHeight) {
                Row(Modifier.fillMaxSize()) {
                    map(Modifier.weight(1f).fillMaxSize())
                    controls(Modifier.weight(1f).fillMaxSize())
                }
            } else {
                val controlsHeight = maxHeight * 0.48f
                Column(Modifier.fillMaxSize()) {
                    map(Modifier.weight(1f).fillMaxWidth())
                    controls(Modifier.fillMaxWidth().heightIn(max = controlsHeight))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun RoutePlannerPreview() {
    VRockNavigationExampleTheme {
        RoutePlannerScreen(
            screen = TripScreen.COMPOSE,
            config = NavigationLaunchConfig(),
            points = listOf(demoPoints.origin, demoPoints.destination),
            route = null,
            buildingRoute = false,
            routeFailed = false,
            permissionDenied = false,
            onScreenSelected = {},
            onProviderSelected = {},
            onPointsChanged = {},
            onBuild = {},
            onSettings = {},
            onTripSettings = {},
            onStart = {},
        )
    }
}
