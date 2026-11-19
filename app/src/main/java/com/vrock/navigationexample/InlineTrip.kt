package com.vrock.navigationexample

import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.vrock.navigation.core.LocationProvider
import com.vrock.navigation.core.NavigationCoordinate
import com.vrock.navigation.core.NavigationLocation
import com.vrock.navigation.core.NavigationRoute
import com.vrock.navigation.core.RouteRequest
import com.vrock.navigation.location.AndroidLocationProvider
import com.vrock.navigation.sdk.NavigationState
import com.vrock.navigation.sdk.TripProgress
import com.vrock.navigation.ui.NavigationScreen
import com.vrock.navigation.ui.view.NavigationLaunchConfig
import com.vrock.navigation.ui.view.NavigationRoutePoints
import com.vrock.navigation.ui.view.NavigationView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
internal fun InlineTrip(
    app: ExampleApplication,
    screen: TripScreen,
    config: NavigationLaunchConfig,
    points: NavigationRoutePoints,
    initialRoute: NavigationRoute?,
    onEnd: () -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    var route by remember(attempt) { mutableStateOf<NavigationRoute?>(null) }
    var progress by remember(attempt) { mutableStateOf<TripProgress?>(null) }
    var failed by remember(attempt) { mutableStateOf(false) }
    val locations = remember(attempt) { Channel<NavigationLocation>(Channel.BUFFERED) }

    val scope = rememberCoroutineScope()
    val simulator = remember(locations, scope) {
        TapLocationSimulator(scope) { locations.trySend(it).isSuccess }
    }

    LaunchedEffect(locations) {
        try {
            val locationProvider = if (config.simulateByTap) {
                LocationProvider { locations.receiveAsFlow() }
            } else {
                AndroidLocationProvider(app)
            }
            val navigation = app.createNavigation(config, locationProvider)
            val request = RouteRequest(points.origin, points.destination, points.waypoints)
            val readyRoute = initialRoute ?: run {
                navigation.requestRoute(request)
                (navigation.state.value as? NavigationState.RouteReady)?.route
            }
            if (readyRoute == null) {
                failed = true
                return@LaunchedEffect
            }
            route = readyRoute
            navigation.trackTrip(readyRoute, request).collect { progress = it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        } finally {
            simulator.reset()
            locations.cancel()
        }
    }

    val uiConfig = config.appearance.uiConfig
    BackHandler { if (uiConfig.allowBackToEndTrip) onEnd() }
    val activeRoute = route
    val modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
    if (failed || activeRoute == null) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (failed) {
                Text(stringResource(
                    if (activeRoute != null && !config.simulateByTap) R.string.gps_unavailable else R.string.trip_failed,
                ))
                Button(onClick = { attempt++ }) { Text(stringResource(R.string.retry_trip)) }
            } else {
                CircularProgressIndicator()
                Text(stringResource(R.string.building_route))
            }
            Button(onClick = onEnd) { Text(stringResource(R.string.back_to_demo)) }
        }
        return
    }

    val onMapTap: ((NavigationCoordinate) -> Unit)? =
        if (config.simulateByTap) simulator::tap else null
    val hint = if (config.simulateByTap) stringResource(R.string.simulation_hint) else null
    val colors = config.appearance.colors
    val mapFactory = remember(config.provider, config.mapTheme) { config.tripMapFactory() }
    Column(modifier) {
        if (!uiConfig.showEndTripButton) {
            Button(onClick = onEnd, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.stop_from_app))
            }
        }
        val tripModifier = Modifier.weight(1f).fillMaxWidth()
        if (screen == TripScreen.XML) {
            AndroidView(
                modifier = tripModifier,
                factory = { context ->
                    val view = LayoutInflater.from(context)
                        .inflate(R.layout.example_navigation, null) as NavigationView
                    view.apply { manageSystemBars = true }
                },
                update = { view ->
                    view.render(
                        styleUrl = config.styleUrl,
                        route = activeRoute,
                        trip = progress,
                        onEndTrip = onEnd,
                        onMapTap = onMapTap,
                        waitingMessage = hint,
                        colors = colors,
                        uiConfig = uiConfig,
                        destinationMarker = config.appearance.destinationMarker,
                        mapFactory = mapFactory,
                    )
                },
            )
        } else {
            NavigationScreen(
                modifier = tripModifier,
                styleUrl = config.styleUrl,
                route = activeRoute,
                trip = progress,
                onEndTrip = onEnd,
                onMapTap = onMapTap,
                waitingMessage = hint,
                colors = colors,
                uiConfig = uiConfig,
                destinationMarker = config.appearance.destinationMarker,
                mapFactory = mapFactory,
            )
        }
    }
}
