package com.vrock.navigationexample

import android.location.Location
import android.os.SystemClock
import com.vrock.navigation.core.NavigationCoordinate
import com.vrock.navigation.core.NavigationLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class TapLocationSimulator(
    private val scope: CoroutineScope,
    private val emit: (NavigationLocation) -> Boolean,
) {
    private var previous: NavigationCoordinate? = null
    private var lastTapTime = 0L
    private var stopJob: Job? = null

    fun tap(coordinate: NavigationCoordinate) {
        val time = SystemClock.elapsedRealtime()
        val speed = previous?.let { last ->
            val distance = FloatArray(1)
            Location.distanceBetween(last.latitude, last.longitude, coordinate.latitude, coordinate.longitude, distance)
            (distance[0] / ((time - lastTapTime).coerceAtLeast(250L) / 1000.0))
                .coerceIn(0.0, 110.0 / 3.6)
        } ?: 0.0
        if (!emit(NavigationLocation(coordinate, time, 5f, speed))) return
        previous = coordinate
        lastTapTime = time
        stopJob?.cancel()
        stopJob = scope.launch {
            delay(2_000.milliseconds)
            emit(NavigationLocation(coordinate, SystemClock.elapsedRealtime(), 5f, 0.0))
        }
    }

    fun reset() {
        stopJob?.cancel()
        previous = null
        lastTapTime = 0L
    }
}
