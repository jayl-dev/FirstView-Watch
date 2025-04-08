package com.jlsoft.firstviewwatch.map// WearMapScreen.kt

import MapViewModel
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.jlsoft.firstviewwatch.MyApplication
import com.jlsoft.firstviewwatch.R
import com.jlsoft.firstviewwatch.api.Stop
import com.jlsoft.firstviewwatch.api.VehicleLocation
import com.jlsoft.firstviewwatch.settings.SettingsActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@Composable
fun WearMapScreen(viewModel: MapViewModel = MapViewModel()) {
    // Create and remember the MapView
    val mapView = rememberMapViewWithLifecycle()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    // Cache markers keyed by a unique identifier (e.g., stop.id).
    val stopMarkers = remember { mutableSetOf<Marker>() }
    val busMarkers = remember { mutableSetOf<Marker>() }
    val hasZoomAdjusted = remember { mutableStateOf(false) }


    // Observe the latest EtaResponse from the ViewModel.
    val etaResponse by viewModel.etaResponse.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()



    // Retrieve the GoogleMap instance only once.
    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            googleMap = map // Update the state
            map.uiSettings.isZoomControlsEnabled = MyApplication.isRunningOnEmulator()

            map.setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    hasZoomAdjusted.value = true
                }
            }
        }
        onDispose {
            googleMap?.setOnCameraMoveStartedListener(null)
            googleMap = null
        }
    }
    LaunchedEffect(Unit) {
        // Using snapshotFlow ensures that we create only one collector that watches both states.
        snapshotFlow { Pair(googleMap, etaResponse) }
            .collect { (map, etaResponse) ->
                if (map == null || etaResponse == null) return@collect

                busMarkers.forEach { it.remove() }
                stopMarkers.forEach { it.remove() }
                busMarkers.clear()
                stopMarkers.clear()
                etaResponse.result?.forEach { result ->
                    result.stop?.let { stop ->
                        val stopMarker = addStopMarkers(map, stop,
                            result.student?.first_name?:"")
                        if(stopMarker != null){
                            stopMarkers.add(stopMarker)
                        }
                    }
                    // Only process if the vehicle_location is present.
                    result.vehicle_location?.let { vehicleLocation ->
                        val busMarker = addBusMarker(map, vehicleLocation, result.student?.first_name?:"",
                            result.route?:"",
                            MyApplication.applicationContext())
                        if(busMarker != null){
                            busMarkers.add(busMarker)
                        }
                    }
                }

                // If there are markers and the zoom has not been adjusted yet, adjust the camera.
                if (stopMarkers.isNotEmpty() && !hasZoomAdjusted.value) {
                    adjustZoom(map, stopMarkers)
                    // Mark that the map's zoom has been adjusted.
                    hasZoomAdjusted.value = true
                }

            }
    }

    LaunchedEffect(mapView) {
        snapshotFlow { mapView.isShown }
            .distinctUntilChanged()  // Only react to changes.
            .collectLatest { isShown ->
                if (isShown) {
                    // While the map view is visible, poll every 10 seconds.
                    while (true) {
                        // Call your suspend function that fetches new API data.
                        viewModel.refreshData()
                        delay(10_000L)
                    }
                }
                // When isShown becomes false, collectLatest will cancel the polling block.
            }
    }

    val context = LocalContext.current

    // Compose UI: Box to overlay a loading indicator on top of the MapView.
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
                .aspectRatio(1f) // For round watches
        )

        // Small settings button using an IconButton with a settings icon.
        IconButton(
            onClick = {
                // Launch the settings activity
                val intent = Intent(context, SettingsActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(4.dp)
                .size(40.dp) // Smaller icon button size (adjust as needed)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colors.primary
            )
        }


        if (isLoading) {
            // Overlay a centered progress indicator.
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

}


fun addStopMarkers(
    googleMap: GoogleMap,
    stop: Stop,
    studentName: String
): Marker? {
// Compute hue from the student name.
    val hue = (abs(studentName.hashCode()) % 360).toFloat()

    Log.d("stop", studentName)

    // Create the LatLng position from vehicle location.
    val position = LatLng(stop.lat, stop.lng)

    // Add marker to the map with the specified rotation (bearing) and centered anchor.
    return googleMap.addMarker(
        MarkerOptions()
            .position(position)
            .icon(BitmapDescriptorFactory.defaultMarker(hue))
            .title(stop.name)
            .anchor(0.5f, 0.5f)
    )

}


fun adjustZoom(
    googleMap: GoogleMap,
    markers: Set<Marker>,
    padding: Int = 100
) {
    if (markers.isNotEmpty()) {
        val builder = LatLngBounds.Builder()
        markers.forEach { marker ->
            builder.include(marker.position)
        }
        val bounds = builder.build()
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }
}

/**
 * Returns a tinted bus icon based on the given hue.
 * Assumes you have an ic_bus vector drawable in your resources.
 */
fun getTintedBusIcon(context: Context, drawableResId: Int, hue: Float): BitmapDescriptor {
    // Retrieve the vector drawable
    val drawable = context.getDrawable(drawableResId)?.mutate()
        ?: throw IllegalArgumentException("Drawable resource not found")

    // Convert the hue (0-359) into an ARGB color using full saturation and brightness.
    val hsv = floatArrayOf(hue, 1f, 1f)
    val color = android.graphics.Color.HSVToColor(hsv)
    drawable.setTint(color)

    // Create a bitmap from the drawable
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 24
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 24
    drawable.setBounds(0, 0, width, height)
    val bitmap = createBitmap(width, height)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
fun addBusMarker(
    googleMap: GoogleMap,
    vehicleLocation: VehicleLocation,
    studentName: String,
    route: String,
    context: Context
): Marker? {
    // Compute hue from the student name.
    val hue = (abs(studentName.hashCode()) % 360).toFloat()

    // Use the helper function to get a tinted bus icon.
    val busIcon = getTintedBusIcon(context, R.drawable.bus_school, hue)

    // Create the LatLng position from vehicle location.
    val position = LatLng(vehicleLocation.lat, vehicleLocation.lng)

    // Add marker to the map with the specified rotation (bearing) and centered anchor.
    return googleMap.addMarker(
        MarkerOptions()
            .position(position)
            .icon(busIcon)
            .title("$studentName (R$route)")
            .rotation(vehicleLocation.bearing.toFloat())
            .anchor(0.5f, 0.5f)
    )
}
