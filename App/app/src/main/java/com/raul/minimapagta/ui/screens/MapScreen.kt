package com.raul.minimapagta.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.raul.minimapagta.R

@Composable
fun MapScreen() {
    val context = LocalContext.current

    var hasLocationPermission by remember { mutableStateOf(false) }
    var isCameraRotating by remember { mutableStateOf(false) }

    val viewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(16.0)
            pitch(0.0) // Vista inicial cenital
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(hasLocationPermission, isCameraRotating) {
        if (hasLocationPermission) {
            val bearingMode = if (isCameraRotating) {
                FollowPuckViewportStateBearing.SyncWithLocationPuck
            } else {
                FollowPuckViewportStateBearing.Constant(0.0)
            }

            viewportState.transitionToFollowPuckState(
                followPuckViewportStateOptions = FollowPuckViewportStateOptions.Builder()
                    .bearing(bearingMode)
                    .pitch(0.0) // <-- AQUÍ FIJAMOS LA INCLINACIÓN A 0 PARA MANTENER EL 2D
                    .zoom(16.0) // Mantenemos el nivel de zoom constante
                    .build()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {

        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            style = {
                MapStyle(style = "mapbox://styles/raul2005/cmsnqjsyh01a401qo4io098fm")
            }
        ) {
            MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                    enabled = true
                    locationPuck = LocationPuck2D(
                        bearingImage = ImageHolder.from(R.drawable.icon_2),
                        scaleExpression = "3.0"
                    )
                    puckBearingEnabled = true
                    puckBearing = PuckBearing.COURSE
                }
            }
        }

        Button(
            onClick = { isCameraRotating = !isCameraRotating },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text(if (isCameraRotating) "Fijar Cámara" else "Rotar Cámara")
        }
    }
}