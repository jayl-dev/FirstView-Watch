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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun WearMapScreen(viewModel: MapViewModel = viewModel()) {
    val mapView = rememberMapViewWithLifecycle()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val stopMarkers = remember { mutableSetOf<Marker>() }
    val busMarkers = remember { mutableSetOf<Marker>() }
    val hasZoomAdjusted = remember { mutableStateOf(false) }
    val isPanning = remember { mutableStateOf(false) }

    // Observe the latest EtaResponse from the ViewModel.
    val etaResponse by viewModel.etaResponse.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showIconsJob: Job? = null



    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            googleMap = map
//            map.uiSettings.isZoomControlsEnabled = MyApplication.isRunningOnEmulator()

            map.setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    hasZoomAdjusted.value = true
                    isPanning.value = true
                    showIconsJob?.cancel()
                    map.uiSettings.isZoomControlsEnabled = true
                }
            }
            map.setOnCameraIdleListener {
                // Use a coroutine on the Main dispatcher to delay showing icons.
                showIconsJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(2000L) // 1 second delay after panning stops
                    map.uiSettings.isZoomControlsEnabled = false
                    isPanning.value = false
                }
            }


        }
        onDispose {
            googleMap?.setOnCameraMoveStartedListener(null)
            googleMap = null
        }
    }

//    LaunchedEffect(etaResponse) {
//        println("LaunchedEffect triggered: $etaResponse")
//    }
    LaunchedEffect(etaResponse) {

        Log.d("MAP", "mapping ${etaResponse?.result?.size} results")
        busMarkers.forEach { it.remove() }
        stopMarkers.forEach { it.remove() }
        busMarkers.clear()
        stopMarkers.clear()
        etaResponse?.result?.forEach { result ->
            val studentName = result.student?.first_name?:""

            Log.d("NAME", studentName)

            result.stop?.let { stop ->
                Log.d("STOP", stop.toString())

                val stopMarker = addStopMarkers(googleMap, stop, studentName)
                if(stopMarker != null){
                    stopMarkers.add(stopMarker)
                }
            }

            result.vehicle_location?.let { vehicleLocation ->
                val busMarker = addBusMarker(googleMap, vehicleLocation,
                    studentName,
                    result.route?:"",
                    MyApplication.applicationContext())
                Log.d("VEHICLE", vehicleLocation.toString())
                if(busMarker != null){
                    busMarkers.add(busMarker)
                }
            }
        }

        // If there are markers and the zoom has not been adjusted yet, adjust the camera.
        if (stopMarkers.isNotEmpty() && !hasZoomAdjusted.value) {
            adjustZoom(googleMap, stopMarkers)
            hasZoomAdjusted.value = true
        }
    }

    LaunchedEffect(mapView) {
        snapshotFlow { mapView.isShown }
            .distinctUntilChanged()  // Only react to changes.
            .collectLatest { isShown ->
                if (isShown) {
                    // While the map view is visible, poll every 10 seconds.
                    while (true) {
                        viewModel.refreshData()
                        delay(10_000L)
                    }
                }
            }
    }

    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
                .aspectRatio(1f) // For round watches
        )

        if(!isPanning.value){
            IconButton(
                onClick = {
                    // Launch the settings activity
                    val intent = Intent(context, SettingsActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(2.dp)
                    .size(40.dp) // Smaller icon button size (adjust as needed)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colors.primary
                )
            }

            IconButton(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.refreshData()
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(2.dp)
                    .size(40.dp) // Smaller icon button size (adjust as needed)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colors.primary
                )
            }
        }


        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

}


fun addStopMarkers(
    googleMap: GoogleMap?,
    stop: Stop,
    studentName: String
): Marker? {
// Compute hue from the student name.
    val hue = (abs(studentName.hashCode()) % 360).toFloat()
    val position = LatLng(stop.lat, stop.lng)

    return googleMap?.addMarker(
        MarkerOptions()
            .position(position)
            .icon(BitmapDescriptorFactory.defaultMarker(hue))
            .title(stop.name)
            .anchor(0.5f, 0.5f)
    )

}


fun adjustZoom(
    googleMap: GoogleMap?,
    markers: Set<Marker>,
    padding: Int = 100
) {
    if (markers.isNotEmpty()) {
        val builder = LatLngBounds.Builder()
        markers.forEach { marker ->
            builder.include(marker.position)
        }
        val bounds = builder.build()
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }
}

/**
 * Returns a tinted bus icon based on the given hue.
 * Assumes you have an ic_bus vector drawable in your resources.
 */
fun getTintedBusIcon(context: Context, drawableResId: Int, hue: Float): BitmapDescriptor {
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
    googleMap: GoogleMap?,
    vehicleLocation: VehicleLocation,
    studentName: String,
    route: String,
    context: Context
): Marker? {
    val hue = (abs(studentName.hashCode()) % 360).toFloat()

    val busIcon = getTintedBusIcon(context, R.drawable.bus_school, hue)
    val position = LatLng(vehicleLocation.lat, vehicleLocation.lng)

    return googleMap?.addMarker(
        MarkerOptions()
            .position(position)
            .icon(busIcon)
            .title("$studentName (R$route)")
            .rotation(vehicleLocation.bearing.toFloat())
            .anchor(0.5f, 0.5f)
    )
}
