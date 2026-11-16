package com.vrock.navigationexample

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.vrock.navigation.core.NavigationCoordinate
import com.vrock.navigation.core.NavigationRoute
import com.vrock.navigation.map.TripMap
import com.vrock.navigation.map.TripMapFactory
import com.vrock.navigation.map.TripMapFrame
import com.vrock.navigation.ui.NavigationScreen
import com.vrock.navigationexample.ui.theme.VRockNavigationExampleTheme

private val previewMap = TripMapFactory { context, _, _ ->
    object : TripMap {
        override val view: View = TextView(context).apply {
            gravity = Gravity.CENTER
            setText(R.string.demo_route)
            setTextColor(0xFF28382C.toInt())
            setBackgroundColor(0xFFE7EBE4.toInt())
        }
        override val isFollowingPosition = true

        override fun render(
            frame: TripMapFrame,
            onMapTap: ((NavigationCoordinate) -> Unit)?,
            onCameraGesture: () -> Unit,
        ) = Unit

        override fun updateNavigationMode(use3D: Boolean) = Unit
        override fun recenter() = Unit
        override fun saveMapState() = Bundle()
        override fun release() = Unit
    }
}

@Preview(name = "Compose trip", widthDp = 393, heightDp = 851, showBackground = true)
@Composable
private fun ComposeTripPreview() {
    VRockNavigationExampleTheme {
        NavigationScreen(
            styleUrl = "",
            route = NavigationRoute(
                geometry = listOf(demoPoints.origin, demoPoints.destination),
                distanceMeters = 1800.0,
                durationSeconds = 240.0,
                maneuvers = emptyList(),
            ),
            trip = null,
            onEndTrip = {},
            waitingMessage = stringResource(R.string.simulation_hint),
            mapFactory = previewMap,
        )
    }
}
