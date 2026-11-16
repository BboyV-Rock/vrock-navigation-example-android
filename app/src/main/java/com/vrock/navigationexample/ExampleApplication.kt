package com.vrock.navigationexample

import android.app.Application
import com.vrock.navigation.core.LocationProvider
import com.vrock.navigation.license.ServerNavigationLicense
import com.vrock.navigation.routing.OsrmRoutingProvider
import com.vrock.navigation.routing.google.GoogleRouteOptions
import com.vrock.navigation.routing.google.GoogleRoutingProvider
import com.vrock.navigation.sdk.VRockNavigation
import com.vrock.navigation.ui.view.NavigationEngine
import com.vrock.navigation.ui.view.NavigationEngineProvider
import com.vrock.navigation.ui.view.NavigationLaunchConfig
import com.vrock.navigation.ui.view.NavigationProvider
import okhttp3.OkHttpClient

class ExampleApplication : Application(), NavigationEngineProvider {
    private val license by lazy { ServerNavigationLicense(applicationContext) }

    private val osrmRouting by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "VRockNavigationExample/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
        OsrmRoutingProvider("https://routing.openstreetmap.de/routed-car/", client)
    }

    private val googleRouting by lazy { GoogleRoutingProvider(BuildConfig.GOOGLE_API_KEY) }

    internal fun createNavigation(
        config: NavigationLaunchConfig,
        locationProvider: LocationProvider,
    ): VRockNavigation<*> = when (config.provider) {
        NavigationProvider.OSRM -> VRockNavigation(
            routingProvider = osrmRouting,
            locationProvider = locationProvider,
            navigationAccess = license,
        )
        NavigationProvider.GOOGLE -> VRockNavigation(
            routingProvider = googleRouting,
            defaultOptions = googleOptions(config),
            locationProvider = locationProvider,
            navigationAccess = license,
        )
    }

    override fun createNavigationEngine(
        config: NavigationLaunchConfig,
        locationProvider: LocationProvider,
    ): NavigationEngine = when (config.provider) {
        NavigationProvider.OSRM -> NavigationEngine.create(
            routingProvider = osrmRouting,
            locationProvider = locationProvider,
            navigationAccess = license,
            mapFactory = config.tripMapFactory(),
        )
        NavigationProvider.GOOGLE -> NavigationEngine.create(
            routingProvider = googleRouting,
            options = googleOptions(config),
            locationProvider = locationProvider,
            navigationAccess = license,
            mapFactory = config.tripMapFactory(),
        )
    }

    private fun googleOptions(config: NavigationLaunchConfig) = GoogleRouteOptions(
        includeTraffic = config.considerTraffic,
        languageCode = resources.configuration.locales[0].toLanguageTag(),
    )
}
