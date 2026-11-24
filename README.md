# VRock Navigation Example

A standalone Android app that consumes VRock Navigation SDK 0.1.6 as Maven packages. It uses a Compose launcher and four SDK navigation integrations for the same demo route.

## Quick start

Install Android SDK Platform 37.2 and use JDK 25 for the Gradle daemon. The project includes the Gradle wrapper. Gradle downloads the SDK from the public VRock Maven repository on GitHub Pages. Downloads are anonymous.

Clone the example, open it in Android Studio, sync Gradle and run the **debug** variant. To build from the command line:

```sh
git clone https://github.com/BboyV-Rock/vrock-navigation-example-android.git
cd vrock-navigation-example-android
./gradlew :app:assembleDebug
```

Use the debug build to evaluate the SDK without activation. See [Build and test a signed release](#build-and-test-a-signed-release) for release setup.

To test a local SDK checkout, publish it with `./gradlew publishAllPublicationsToLocalSdkRepository` in the SDK directory, then build this example with an explicit Maven repository path:

```sh
./gradlew -PvrockSdkRepository=../vrock-navigation-sdk-android/build/maven-sdk :app:assembleDebug
```

For the first run, try the same route with the SDK's ready-made navigation screen:

1. Run the **debug** app on an Android device or emulator with internet access.
2. Select **OSRM** under **Route provider** and **NavigationActivity** under **Trip screen**.
3. Tap **Buffalo demo** to load the start and destination, then tap **Tap to drive**.
4. When the navigation map opens, tap forward along the route to update your simulated position. Tap **End trip** to return to the planner.

This flow works without a Google API key, GPS permission, or a VRock activation key.

## Choose a trip screen

| Mode | Use it when | Who manages the trip? |
| --- | --- | --- |
| **NavigationActivity** | You want the SDK to open a full navigation screen. | The SDK calculates the route, requests GPS permission when needed, and tracks progress. |
| **NavigationFragment** | You want the ready-made SDK screen inside your own Activity and layout. | The SDK calculates the route, requests GPS permission when needed, and tracks progress. Your host handles the fragment's end result. |
| **Compose** | You want to render `NavigationScreen` inside your own Compose flow. | Your app requests GPS permission when needed, calls the SDK to calculate the route, collects `TripProgress`, and passes it to the UI. |
| **XML View** | You want to inflate `NavigationView` from an XML layout. | Your app handles the same trip flow as Compose and renders updates with `NavigationView.render()`. |

For Compose and XML, the host can update colors and visibility during a trip. For the ready-made Activity and Fragment, pass the appearance when opening the screen. See [Customize colors and visible controls](#customize-colors-and-visible-controls) for examples.

## Plan your own route

Open **Navigation colors** to edit the palette with a visual color picker and save it across app launches. All four modes use that palette.

Select **OSRM** (with MapLibre) or **Google** (Google Routes and Google Maps). The planner opens in Buffalo, New York. Tap the map to add points. The first point is the start, the last is the destination, and any points between them become waypoints in order. **Undo** removes the last point, **Clear** removes them all, and **Buffalo demo** loads the original sample points.

Use **Build route** to preview the route. Select **GPS** to follow the device position. Allow precise location, turn on GPS and choose a start near your current position. Select **Tap to drive** to simulate position by tapping along the route. Both modes work with all four trip screens. Routing and map tiles need internet access. The public OSRM endpoint is for evaluation, and the debug app runs without a VRock activation key.

The example runs in OSRM mode by default. To try Google, copy [app/secrets.properties.example](app/secrets.properties.example) to `app/secrets.properties`, set `GOOGLE_API_KEY`, and rebuild. Git ignores the local file. Google start buttons stay disabled until a key is configured. Enable Maps SDK for Android and Routes API for the key. The Android app ID is `com.vrock.navigationexample`.

## Try the changes in SDK 0.1.5

1. Open **Map and trip controls**, select **Dark**, then save. The planner map changes theme. Start any of the four trip screens to see the themed navigation UI. Choose **Auto** to try a schedule using the device clock. The default dark hours are 19:00–07:00.
2. Open **Navigation colors** to set an optional screen background. Select **Theme default** in the color picker to restore the SDK default. Night mode changes the default map controls, and an explicitly customized button palette takes precedence.
3. Turn off **Allow panning and show recenter** and start a trip. The navigation camera stays in follow mode.
4. Turn on **Stop from app**. The SDK's **End trip** button is hidden and Back is disabled during active navigation. In **NavigationFragment**, tap **Stop from app** to call the SDK controller. The standalone **NavigationActivity** demonstrates the same API with a 20-second timer. Compose and XML use an app button to stop their own trip.

## Troubleshoot a trip

| What you see | What to check |
| --- | --- |
| **GPS** and **Tap to drive** are disabled | Add at least two points on the map or tap **Buffalo demo**. If Google is selected, configure its key or switch to OSRM. |
| The route does not build | Check the device's internet connection and try **Build route** in the planner to check route calculation separately. For Google, check that Routes API is enabled for the configured key. |
| Google does not load the map | Set `GOOGLE_API_KEY` in `app/secrets.properties`, enable Maps SDK for Android and rebuild the app. |
| The app shows **Navigation license unavailable or invalid** | Check the activation key, application ID and SHA-256 fingerprint of the signing certificate. Rebuild and reinstall the signed release APK. |
| GPS asks for permission or waits for a signal | Grant **Precise** location access and turn on location services. On an emulator, send a location near the chosen route from its location controls. |
| Tap to drive waits for input | Once the trip screen opens, tap along the route to update the simulated position. |
| **End trip** is hidden by your UI configuration | Keep a Back handler or offer a host button to end the trip. In the sample, Compose and XML return to the planner on Back, and the hosted screens end their trip on Back by default. If **Stop from app** is enabled, use the app button or wait for the standalone Activity timer. |

## Use the SDK in code

### Choose the Maven modules

The [Gradle version catalog](gradle/libs.versions.toml) defines the Maven coordinates and keeps the SDK modules on a shared `vrockSdk` version. Copy the relevant catalog entries into your app and configure the Maven repository described above. The example uses these aliases in `app/build.gradle.kts`.

```kotlin
dependencies {
    implementation(libs.vrock.navigation.ui) // Compose
    implementation(libs.vrock.navigation.ui.view) // XML, Activity, Fragment
    implementation(libs.vrock.navigation.routing) // OSRM
    implementation(libs.vrock.navigation.routing.google) // Google Routes
    implementation(libs.vrock.navigation.map.google) // Google trip map
    implementation(libs.vrock.navigation.location) // GPS
    implementation(libs.google.maps) // Google route planner MapView
}
```

### Configure routing and launch options

[ExampleApplication.kt](app/src/main/java/com/vrock/navigationexample/ExampleApplication.kt) implements `NavigationEngineProvider` for the SDK's Activity and Fragment. Register it as `android:name=".ExampleApplication"` and declare `android.permission.INTERNET` in the [manifest](app/src/main/AndroidManifest.xml).

The following method belongs inside `ExampleApplication`. The application defines `osrmRouting` with `OsrmRoutingProvider`, `googleRouting` with `GoogleRoutingProvider(BuildConfig.GOOGLE_API_KEY)`, and `license` with `ServerNavigationLicense(applicationContext, activationKey = BuildConfig.VROCK_ACTIVATION_KEY)`. For Google, Gradle reads the key from the properties file described above and passes it to both `BuildConfig` for Routes and the manifest's `com.google.android.geo.API_KEY` entry for Maps.

```kotlin
private fun googleOptions(config: NavigationLaunchConfig) = GoogleRouteOptions(
    includeTraffic = config.considerTraffic,
    languageCode = resources.configuration.locales[0].toLanguageTag(),
)

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
```

The OSRM planner uses the SDK's MapLibre composable. The Google planner uses a Google `MapView` with the same `onPointSelected` callback. [RoutePlannerScreen.kt](app/src/main/java/com/vrock/navigationexample/RoutePlannerScreen.kt) wires both to the shared point list.

```kotlin
NavigationMap(
    styleUrl = styleUrl,
    modifier = Modifier.fillMaxSize(),
    points = selectedPoints,
    route = previewRoute?.geometry.orEmpty(),
    initialCenter = NavigationCoordinate(42.8864, -78.8784),
    onPointSelected = { point -> selectedPoints = selectedPoints + point },
)
```

Collect coordinates from the planner map and build the launch config once there are at least two selected points. Set `simulateByTap` to `false` to use GPS.

```kotlin
val points = NavigationRoutePoints(
    origin = selectedPoints.first(),
    destination = selectedPoints.last(),
    waypoints = selectedPoints.drop(1).dropLast(1),
)
val config = NavigationLaunchConfig(
    provider = NavigationProvider.OSRM, // Use GOOGLE for Google Routes and Google Maps
    simulateByTap = true,
)
```

### Customize colors and visible controls

Create a `NavigationColors` object and a `NavigationUiConfig`. Colors are Android ARGB integers in `0xAARRGGBB` format. `FF` in the first byte means fully opaque. Omitted fields keep the SDK defaults, and each visibility option defaults to `true`.

```kotlin
import com.vrock.navigation.ui.NavigationColors
import com.vrock.navigation.ui.NavigationUiConfig
import com.vrock.navigation.ui.TripPanelColors
import com.vrock.navigation.ui.view.NavigationAppearance

val appearance = NavigationAppearance(
    colors = NavigationColors(
        route = 0xFF00A896.toInt(),
        position = 0xFF007D65.toInt(),
        maneuverBackground = 0xFF173E36.toInt(),
        maneuverText = 0xFFFFFFFF.toInt(),
        tripPanel = TripPanelColors(
            background = 0xFF173E36.toInt(),
            primaryText = 0xFFFFFFFF.toInt(),
        ),
    ),
    uiConfig = NavigationUiConfig(
        showSpeedometer = false,
        showSpeedLimit = false,
        showRemainingTime = true,
        showRemainingDistance = true,
        showMuteButton = false,
    ),
)
val styledConfig = config.copy(appearance = appearance)
```

| UI area | Visibility options |
| --- | --- |
| Maneuver card | `showManeuverCard`, `showManeuverIcon`, `showStreetName`, `showDistanceToManeuver`, `showProviderInstructions`, `showLaneGuidance` |
| Speed | `showSpeedometer`, `showSpeedLimit` |
| Trip information | `showRemainingTime`, `showRemainingDistance`, `showTrafficStatus` |
| Buttons | `showMuteButton`, `showMapModeButton`, `showRecenterButton`, `showEndTripButton` |
| Back behavior | `allowBackToEndTrip` |

Set a flag to `false` to hide its UI element. For example, hiding the mute button removes the control while voice guidance still follows the trip's mute state. Lane guidance and traffic indicators depend on what data is available. If you hide **End trip**, provide another exit action, such as the app's Back handler or a host button that calls `endTrip()`.

**Activity and Fragment.** Pass `styledConfig` to `NavigationActivity.createIntent(this, styledConfig, points)` or `NavigationFragment.newInstance(styledConfig, points)`. The SDK reads the appearance from the launch arguments. Choose the appearance before opening the screen, or start a new hosted trip to apply changes.

**Compose and XML.** Pass both `appearance.colors` and `appearance.uiConfig` to `NavigationScreen` or `NavigationView.render()`. The examples below use `config.appearance`, so pass `styledConfig` as `config`. Both rendering calls in [InlineTrip.kt](app/src/main/java/com/vrock/navigationexample/InlineTrip.kt) forward these options.

For live updates in your own Compose or XML host, keep the appearance in state and copy the fields you want to change. Pass `currentAppearance.colors` and `currentAppearance.uiConfig` to the renderer from that state. Compose re-renders the screen, and an XML view wrapped in `AndroidView` receives the new values in its `update` block.

```kotlin
var currentAppearance by remember { mutableStateOf(appearance) }

// In a button or settings change callback:
currentAppearance = currentAppearance.copy(
    colors = currentAppearance.colors.copy(route = 0xFF7C4DFF.toInt()),
    uiConfig = currentAppearance.uiConfig.copy(
        showSpeedometer = !currentAppearance.uiConfig.showSpeedometer,
    ),
)
```

The sample's [PaletteSettings.kt](app/src/main/java/com/vrock/navigationexample/PaletteSettings.kt) shows how to convert HSV picker values into ARGB colors and save the palette across restarts with `SharedPreferences`.

### Configure light, dark and scheduled map themes

Pass a `MapThemeConfig` to the managed launch config. The schedule uses the device time zone and supports a dark period that crosses midnight.

```kotlin
val mapTheme = MapThemeConfig(
    mode = MapThemeMode.AUTO,
    schedule = MapThemeSchedule(
        darkStartsAt = LocalTime.of(20, 0),
        darkEndsAt = LocalTime.of(6, 0),
    ),
)
val themedConfig = config.copy(mapTheme = mapTheme)
```

For managed Activity and Fragment trips, the SDK applies `config.mapTheme` to the factory returned by `NavigationEngineProvider`. For Compose and XML, pass a themed map factory directly. [ExampleMapStyles.kt](app/src/main/java/com/vrock/navigationexample/ExampleMapStyles.kt) shows both providers.

```kotlin
val mapLibreFactory = MapLibreTripMapFactory(
    theme = mapTheme,
    styles = MapLibreStyles(
        dark = MapLibreStyle.Uri("https://tiles.openfreemap.org/styles/dark"),
    ),
)
val googleFactory = GoogleTripMapFactory(
    theme = mapTheme,
    styles = GoogleMapStyles(darkStyleResId = R.raw.google_map_dark),
)
```

MapLibre also accepts `MapLibreStyle.RawResource(R.raw.your_style)` or an `asset://` URI. For the planner map, pass the same theme and styles to `NavigationMap`, or use `GoogleMapThemeController` with your `GoogleMap` and close the controller when the map leaves the screen. See [RoutePlannerScreen.kt](app/src/main/java/com/vrock/navigationexample/RoutePlannerScreen.kt) and [GoogleRouteMap.kt](app/src/main/java/com/vrock/navigationexample/GoogleRouteMap.kt).

The navigation screen and its system bars can follow the theme. Set `NavigationColors.screenBackground` to override the screen background. The SDK's standalone Activity and Compose screen manage their system bars. For a full-screen Fragment or XML host, set `manageSystemBars = true` on `NavigationFragment` or `NavigationView`.

### Stop a trip from the host app

For a managed Activity or Fragment, configure the SDK controls and call `NavigationController.stopNavigation()` from a host button or another event in the app.

```kotlin
val appControlled = config.copy(
    appearance = config.appearance.copy(
        uiConfig = config.appearance.uiConfig.copy(
            showEndTripButton = false,
            allowBackToEndTrip = false,
        ),
    ),
)

// Run from the host's stop action.
val accepted = NavigationController.stopNavigation()
```

The controller cancels route building or tracking on active managed screens in the current process. It closes the standalone Activity. A managed Fragment delivers the end result to the host and is removed if the host remains open. `accepted` is false when there is no new stop request to issue. For direct Compose and XML integrations, call the host's `onEnd` handler to cancel its trip coroutine and return to the planner.

### Launch the SDK's Activity or embed its Fragment

From an Activity, use the SDK's intent factory. In this mode the SDK calculates the route and manages simulated location updates.

```kotlin
startActivity(NavigationActivity.createIntent(this, config, points))
```

To embed the SDK UI, place a `FragmentContainerView` in your own `FragmentActivity` and add the fragment after setting the content view. The SDK manages the trip here as well. This excerpt assumes the container ID is `R.id.navigation_container`.

```kotlin
supportFragmentManager.setFragmentResultListener(
    NavigationFragment.managedEndResultKey,
    this,
) { _, _ -> finish() }

if (supportFragmentManager.findFragmentById(R.id.navigation_container) == null) {
    supportFragmentManager.beginTransaction()
        .add(R.id.navigation_container, NavigationFragment.newInstance(config, points).apply {
            manageSystemBars = true
        })
        .commitNow()
}
```

Call `endTrip()` on the embedded `NavigationFragment` to stop the trip from your host. See [EmbeddedNavigationActivity.kt](app/src/main/java/com/vrock/navigationexample/EmbeddedNavigationActivity.kt) for a full host with a close button and Back handling.

### Manage a trip in Compose or XML

#### Switch from the planner to the trip

[MainActivity.kt](app/src/main/java/com/vrock/navigationexample/MainActivity.kt) uses Compose state to decide what to display. The **Tap to drive** button passes `true` to the start handler, while **GPS** passes `false`. For GPS, the handler requests precise location if needed and waits for permission before setting the state.

```kotlin
var simulateTrip by rememberSaveable { mutableStateOf<Boolean?>(null) }

// In the Compose or XML start handler, once any required permission is granted:
simulateTrip = useTapSimulation
```

The assignment schedules recomposition. The click handler finishes, then Compose re-evaluates the display condition in the same Activity. This excerpt omits the settings branch.

```kotlin
val simulated = simulateTrip
when {
    simulated != null && routePoints != null -> InlineTrip(
        app, screen, config.copy(simulateByTap = simulated), routePoints, previewRoute,
    ) { simulateTrip = null }

    else -> RoutePlannerScreen(/* ... */)
}
```

- `null` shows the planner.
- `true` shows the trip with tap simulation.
- `false` shows the trip with GPS.

The condition checks for a non-null value, so both `true` and `false` open the trip. Once the route is ready, `InlineTrip` renders `NavigationScreen` for Compose or inflates `NavigationView` through `AndroidView` for XML. Both run inside `MainActivity`. Ending the trip sets the state back to `null` and brings the planner back.

#### Calculate and track the route

For direct rendering, the app calls the SDK to calculate the route and collects progress. The shared setup below runs inside a composable. A map tap becomes a `NavigationLocation`, which the simulator sends to the SDK. [InlineTrip.kt](app/src/main/java/com/vrock/navigationexample/InlineTrip.kt) includes loading, error handling and retry.

```kotlin
var route by remember { mutableStateOf<NavigationRoute?>(null) }
var progress by remember { mutableStateOf<TripProgress?>(null) }
val locations = remember { Channel<NavigationLocation>(Channel.BUFFERED) }

LaunchedEffect(locations) {
    try {
        val locationProvider = if (config.simulateByTap) {
            LocationProvider { locations.receiveAsFlow() }
        } else {
            AndroidLocationProvider(app)
        }
        val navigation = app.createNavigation(config, locationProvider)
        val request = RouteRequest(points.origin, points.destination, points.waypoints)
        navigation.requestRoute(request)
        val ready = navigation.state.value as? NavigationState.RouteReady
            ?: return@LaunchedEffect
        route = ready.route
        navigation.trackTrip(ready.route, request).collect { progress = it }
    } finally {
        locations.cancel()
    }
}

val simulatePosition: (NavigationCoordinate) -> Unit = { coordinate ->
    locations.trySend(NavigationLocation(coordinate, SystemClock.elapsedRealtime(), 5f, 11.176))
}
val onMapTap = simulatePosition.takeIf { config.simulateByTap }
val mapFactory = remember(config.provider, config.mapTheme) { config.tripMapFactory() }
val activeRoute = route ?: return
```

For GPS, declare coarse and fine location permissions in the manifest. The SDK's Activity and Fragment handle runtime permission. For Compose and XML, [MainActivity.kt](app/src/main/java/com/vrock/navigationexample/MainActivity.kt) requests both permissions before opening the trip and checks that precise location was granted.

Here `app` is the example's `ExampleApplication`. Its `createNavigation` method configures `VRockNavigation` with the selected routing provider and location source. The map factory selects MapLibre for OSRM or Google Maps for Google and includes the selected theme and styles. In both examples below, `onEnd` returns to the launcher, removing the composable and cancelling its trip coroutine.

#### Compose screen

```kotlin
NavigationScreen(
    styleUrl = config.styleUrl,
    route = activeRoute,
    trip = progress,
    onEndTrip = onEnd,
    onMapTap = onMapTap,
    colors = config.appearance.colors,
    uiConfig = config.appearance.uiConfig,
    mapFactory = mapFactory,
)
```

#### XML View

Declare the SDK view in `res/layout/example_navigation.xml`.

```xml
<com.vrock.navigation.ui.view.NavigationView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

When `screen == TripScreen.XML`, `InlineTrip` inflates this layout in `AndroidView.factory`. The `update` block passes the current route and progress to the SDK view.

```kotlin
AndroidView(
    factory = { context ->
        val view = LayoutInflater.from(context).inflate(R.layout.example_navigation, null) as NavigationView
        view.apply { manageSystemBars = true }
    },
    update = { view ->
        view.render(
            styleUrl = config.styleUrl,
            route = activeRoute,
            trip = progress,
            onEndTrip = onEnd,
            onMapTap = onMapTap,
            colors = config.appearance.colors,
            uiConfig = config.appearance.uiConfig,
            mapFactory = mapFactory,
        )
    },
)
```

### Open the Compose previews

Select the `debug` build variant in Android Studio. Open `RoutePlannerScreen.kt`, `PaletteSettings.kt` or [ComposeTripPreview.kt](app/src/debug/java/com/vrock/navigationexample/ComposeTripPreview.kt) and switch the editor to **Split** or **Design**. The trip preview uses a static placeholder for the map.

## Game radar, adaptive zoom and destination markers

Open **Map and trip controls** and choose **Game radar** for Google Maps or MapLibre. The SDK supplies the day and night styles and the cyan route palette. The route fades near the vehicle during navigation. The app saves the preset across restarts.

In the same settings, turn **Adaptive zoom** or **Show destination marker** on or off. Adaptive zoom follows vehicle speed while the camera follows the marker. To customize the finish marker in code, pass `DestinationMarkerStyle(backgroundColor = 0xFF5145B8.toInt(), sizeDp = 40)` to `NavigationAppearance.destinationMarker`. Activity and Fragment read it from the launch config. Compose and XML pass it to their renderers.

In **Tap to drive**, speed depends on the distance and time between taps. After two seconds without a tap, speed returns to zero. Tap forward along the route, then pause to see the adaptive camera move closer again.

## Build and test a signed release

`./gradlew :app:assembleRelease` builds an unsigned APK in `app/build/outputs/apk/release/`. The example uses standard R8 optimization. Keep `optimization { enable = true }` without a custom `packageScope`. If an APK built with the old scoped settings crashes at startup with `IllegalAccessError`, rebuild and reinstall it.

1. Choose the signing key you will use for the release APK.
2. When issuing the VRock activation key, use the application ID `com.vrock.navigationexample` and the SHA-256 fingerprint of the certificate that will sign the installed APK.
3. Create `app/vrock-license.properties` with the following entry. Git ignores this file.

   ```properties
   VROCK_ACTIVATION_KEY=your_activation_key
   ```

4. In Android Studio, use **Generate Signed Bundle / APK** to build the signed APK, then install it. Select **OSRM**, load **Buffalo demo**, then choose **Tap to drive**. The navigation screen should open with the route ready. An absent or invalid activation key allows the APK to build, but the SDK blocks release navigation. Debug builds run in demo mode.
