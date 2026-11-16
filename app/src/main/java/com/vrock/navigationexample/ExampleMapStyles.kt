package com.vrock.navigationexample

import com.vrock.navigation.map.MapLibreStyle
import com.vrock.navigation.map.MapLibreStyles
import com.vrock.navigation.map.MapLibreTripMapFactory
import com.vrock.navigation.map.google.GoogleMapStyles
import com.vrock.navigation.map.google.GoogleTripMapFactory
import com.vrock.navigation.ui.view.NavigationLaunchConfig
import com.vrock.navigation.ui.view.NavigationProvider

internal val exampleMapLibreStyles = MapLibreStyles(
    dark = MapLibreStyle.Uri("https://tiles.openfreemap.org/styles/dark"),
)

internal val exampleGoogleMapStyles = GoogleMapStyles(darkStyleResId = R.raw.google_map_dark)

internal fun NavigationLaunchConfig.tripMapFactory() = when (provider) {
    NavigationProvider.OSRM -> MapLibreTripMapFactory(mapTheme, exampleMapLibreStyles)
    NavigationProvider.GOOGLE -> GoogleTripMapFactory(mapTheme, exampleGoogleMapStyles)
}
