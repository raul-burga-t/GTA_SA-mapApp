package com.raul.minimapagta.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.raul.minimapagta.R
import com.mapbox.maps.plugin.PuckBearing

@Composable
fun MapScreen() {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Si el usuario acepta, el MapEffect de abajo mostrará el ícono automáticamente
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val viewportState = MapViewportState().apply {
        setCameraOptions {
            zoom(14.0)
            center(Point.fromLngLat(-79.1231, -8.0784))
            pitch(0.0)
        }
    }

    MapboxMap(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
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

                // 1. Activamos la rotación del ícono
                puckBearingEnabled = true

                // 2. Elegimos cómo queremos que rote
                // COURSE: Rota calculando tu trayectoria 
                puckBearing = PuckBearing.COURSE
            }
        }
    }
}