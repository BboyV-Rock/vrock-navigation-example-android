package com.vrock.navigationexample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vrock.navigation.core.NavigationCoordinate
import com.vrock.navigation.core.NavigationRoute
import com.vrock.navigation.core.RouteRequest
import com.vrock.navigation.map.MapStylePreset
import com.vrock.navigation.map.MapThemeConfig
import com.vrock.navigation.routing.OsrmRoutingException
import com.vrock.navigation.ui.NavigationColors
import com.vrock.navigation.sdk.NavigationState
import com.vrock.navigation.ui.NavigationUiConfig
import com.vrock.navigation.ui.view.NavigationActivity
import com.vrock.navigation.ui.view.NavigationAppearance
import com.vrock.navigation.ui.view.NavigationController
import com.vrock.navigation.ui.view.NavigationLaunchConfig
import com.vrock.navigation.ui.view.NavigationProvider
import com.vrock.navigation.ui.view.NavigationRoutePoints
import com.vrock.navigationexample.ui.theme.VRockNavigationExampleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Duration.Companion.milliseconds

internal val demoPoints = NavigationRoutePoints(
    origin = NavigationCoordinate(42.8864, -78.8784),
    destination = NavigationCoordinate(42.8970, -78.8690),
)

internal enum class TripScreen(val label: Int) {
    COMPOSE(R.string.host_compose),
    XML(R.string.host_xml),
    ACTIVITY(R.string.host_activity),
    FRAGMENT(R.string.host_fragment),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val palette = getSharedPreferences("navigation_palette", MODE_PRIVATE)
        val app = application as ExampleApplication
        setContent {
            VRockNavigationExampleTheme {
                var screen by rememberSaveable { mutableStateOf(TripScreen.ACTIVITY) }
                var provider by rememberSaveable { mutableStateOf(NavigationProvider.OSRM) }
                var points by rememberSaveable { mutableStateOf(arrayListOf<NavigationCoordinate>()) }
                var showTripSettings by rememberSaveable { mutableStateOf(false) }
                var mapTheme by rememberSaveable { mutableStateOf(MapThemeConfig(preset = MapStylePreset.entries.firstOrNull {
                    it.name == palette.getString("map_preset", palette.getString("google_map_preset", null))
                } ?: MapStylePreset.STANDARD)) }
                var appStopsTrip by rememberSaveable { mutableStateOf(false) }
                var adaptiveZoom by rememberSaveable { mutableStateOf(true) }
                var showDestination by rememberSaveable { mutableStateOf(true) }
                var recenterEnabled by rememberSaveable { mutableStateOf(true) }
                var stopHostedTrip by rememberSaveable { mutableStateOf(false) }
                val hostedTrip = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { stopHostedTrip = false }
                LaunchedEffect(stopHostedTrip) {
                    if (stopHostedTrip) {
                        delay(20_000.milliseconds)
                        NavigationController.stopNavigation()
                        stopHostedTrip = false
                    }
                }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var simulateTrip by rememberSaveable { mutableStateOf<Boolean?>(null) }
                var colors by remember { mutableStateOf(readPalette(palette)) }
                var permissionDenied by rememberSaveable { mutableStateOf(false) }
                var buildingRoute by remember(points, provider) { mutableStateOf(false) }
                var previewRoute by remember(points, provider) { mutableStateOf<NavigationRoute?>(null) }
                var routeError by remember(points, provider) { mutableStateOf<String?>(null) }

                val config = NavigationLaunchConfig(
                    provider = provider,
                    appearance = NavigationAppearance(
                        colors = if (mapTheme.preset == MapStylePreset.GAME_RADAR) {
                            val radar = NavigationColors.gameRadar()
                            colors.copy(route = radar.route, position = radar.position, positionOutline = radar.positionOutline)
                        } else colors,
                        uiConfig = NavigationUiConfig(
                            adaptiveZoomEnabled = adaptiveZoom,
                            showDestinationMarker = showDestination,
                            showRecenterButton = recenterEnabled,
                            showEndTripButton = !appStopsTrip,
                            allowBackToEndTrip = !appStopsTrip,
                        ),
                    ),
                    mapTheme = mapTheme,
                )
                val routePoints = points.takeIf { it.size >= 2 }?.let {
                    NavigationRoutePoints(it.first(), it.last(), ArrayList(it.subList(1, it.lastIndex)))
                }
                val locationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { grants ->
                    permissionDenied = grants[Manifest.permission.ACCESS_FINE_LOCATION] != true
                    if (!permissionDenied) simulateTrip = false
                }

                LaunchedEffect(points, provider, buildingRoute) {
                    if (!buildingRoute || routePoints == null) return@LaunchedEffect
                    val navigation = app.createNavigation(config) { emptyFlow() }
                    navigation.requestRoute(RouteRequest(routePoints.origin, routePoints.destination, routePoints.waypoints))
                    previewRoute = (navigation.state.value as? NavigationState.RouteReady)?.route
                    val cause = (navigation.state.value as? NavigationState.Error)?.cause
                    routeError = if (previewRoute != null) null else when (cause) {
                        is OsrmRoutingException -> cause.message ?: getString(R.string.preview_route_failed)
                        is java.net.UnknownHostException -> getString(R.string.preview_route_dns)
                        is java.net.SocketTimeoutException -> getString(R.string.preview_route_timeout)
                        else -> getString(R.string.preview_route_failed)
                    }
                    buildingRoute = false
                }

                val simulated = simulateTrip
                when {
                    showTripSettings -> {
                        BackHandler { showTripSettings = false }
                        TripSettings(
                            mapTheme, appStopsTrip, recenterEnabled, adaptiveZoom, showDestination,
                            onCancel = { showTripSettings = false },
                        ) { theme, stopFromApp, recenter, zoom, destination ->
                            palette.edit().putString("map_preset", theme.preset.name).apply()
                            adaptiveZoom = zoom
                            showDestination = destination
                            mapTheme = theme
                            appStopsTrip = stopFromApp
                            recenterEnabled = recenter
                            showTripSettings = false
                        }
                    }
                    showSettings -> {
                        BackHandler { showSettings = false }
                        PaletteSettings(colors, onCancel = { showSettings = false }) {
                            savePalette(palette, it)
                            colors = it
                            showSettings = false
                        }
                    }
                    simulated != null && routePoints != null -> InlineTrip(
                        app, screen, config.copy(simulateByTap = simulated), routePoints, previewRoute,
                    ) { simulateTrip = null }
                    else -> RoutePlannerScreen(
                        screen = screen,
                        config = config,
                        points = points,
                        route = previewRoute,
                        buildingRoute = buildingRoute,
                        routeError = routeError,
                        permissionDenied = permissionDenied,
                        onScreenSelected = { screen = it },
                        onProviderSelected = { provider = it },
                        onPointsChanged = { points = ArrayList(it) },
                        onBuild = {
                            routeError = null
                            buildingRoute = true
                        },
                        onSettings = { showSettings = true },
                        onTripSettings = { showTripSettings = true },
                        onStart = start@{ useTapSimulation ->
                            val tripPoints = routePoints ?: return@start
                            permissionDenied = false
                            val launch = config.copy(simulateByTap = useTapSimulation)
                            when (screen) {
                                TripScreen.ACTIVITY -> {
                                    hostedTrip.launch(NavigationActivity.createIntent(this, launch, tripPoints))
                                    stopHostedTrip = appStopsTrip
                                }
                                TripScreen.FRAGMENT -> startActivity(EmbeddedNavigationActivity.createIntent(this, launch, tripPoints))
                                TripScreen.COMPOSE, TripScreen.XML -> {
                                    if (useTapSimulation || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                                        PackageManager.PERMISSION_GRANTED
                                    ) {
                                        simulateTrip = useTapSimulation
                                    } else {
                                        locationPermission.launch(arrayOf(
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                        ))
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
