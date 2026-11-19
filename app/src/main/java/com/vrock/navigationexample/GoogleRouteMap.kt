package com.vrock.navigationexample

import android.content.ComponentCallbacks
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.vrock.navigation.core.NavigationCoordinate
import com.vrock.navigation.map.MapThemeConfig
import com.vrock.navigation.map.google.GoogleMapThemeController
import com.vrock.navigation.map.google.GoogleMapStyles

@Composable
internal fun GoogleRouteMap(
    modifier: Modifier,
    points: List<NavigationCoordinate>,
    route: List<NavigationCoordinate>,
    routeColor: Int,
    initialCenter: NavigationCoordinate,
    theme: MapThemeConfig,
    styles: GoogleMapStyles,
    onPointSelected: (NavigationCoordinate) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember(context, lifecycle) { MapView(context) }
    val pointSelected by rememberUpdatedState(onPointSelected)
    var map by remember(mapView) { mutableStateOf<GoogleMap?>(null) }

    DisposableEffect(mapView, lifecycle) {
        var destroyed = false
        var started = false
        var resumed = false
        mapView.onCreate(null)
        val callbacks = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() {
                if (!destroyed) mapView.onLowMemory()
            }
        }
        context.applicationContext.registerComponentCallbacks(callbacks)
        fun release() {
            if (destroyed) return
            destroyed = true
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
            context.applicationContext.unregisterComponentCallbacks(callbacks)
        }
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                mapView.onStart()
                started = true
            }

            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
                resumed = true
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
                resumed = false
            }

            override fun onStop(owner: LifecycleOwner) {
                mapView.onStop()
                started = false
            }

            override fun onDestroy(owner: LifecycleOwner) = release()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState != Lifecycle.State.DESTROYED) {
            mapView.getMapAsync { ready ->
                if (!destroyed) {
                    ready.moveCamera(CameraUpdateFactory.newLatLngZoom(initialCenter.toLatLng(), 13f))
                    ready.setOnMapClickListener { pointSelected(NavigationCoordinate(it.latitude, it.longitude)) }
                    map = ready
                }
            }
        }
        onDispose {
            lifecycle.removeObserver(observer)
            release()
        }
    }

    AndroidView(modifier = modifier, factory = { mapView })

    DisposableEffect(map, lifecycle, theme, styles) {
        val controller = map?.let { GoogleMapThemeController(context, lifecycle, it, theme, styles) }
        onDispose { controller?.close() }
    }

    LaunchedEffect(map, points, route, routeColor) {
        val ready = map ?: return@LaunchedEffect
        ready.clear()
        points.forEachIndexed { index, point ->
            ready.addMarker(MarkerOptions().position(point.toLatLng()).title((index + 1).toString()))
        }
        val line = route.map { it.toLatLng() }
        if (line.size >= 2) {
            ready.addPolyline(PolylineOptions().addAll(line).color(routeColor).width(12f))
        }
    }
    val cameraPoints = route.ifEmpty { points }
    DisposableEffect(map, mapView, cameraPoints) {
        var active = true
        val ready = map
        if (ready != null && cameraPoints.isNotEmpty()) {
            mapView.doOnLayout {
                if (active && lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    val coordinates = cameraPoints.map { it.toLatLng() }.distinct()
                    if (coordinates.size == 1) {
                        ready.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates.single(), 15f))
                    } else {
                        val bounds = LatLngBounds.builder().apply { coordinates.forEach { include(it) } }.build()
                        val padding = minOf(
                            (48 * context.resources.displayMetrics.density).toInt(),
                            minOf(mapView.width, mapView.height) / 4,
                        )
                        ready.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    }
                }
            }
        }
        onDispose { active = false }
    }
}

private fun NavigationCoordinate.toLatLng(): LatLng = LatLng(latitude, longitude)
