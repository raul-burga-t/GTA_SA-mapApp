package com.raul.minimapagta.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle // <-- Esta importación es nueva y crucial

@Composable
fun MapScreen() {
    val viewportState = MapViewportState().apply {
        setCameraOptions {
            zoom(14.0)
            // Coordenadas iniciales en Huanchaco
            center(Point.fromLngLat(-79.1231, -8.0784))
            pitch(0.0)
        }
    }

    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapViewportState = viewportState,
        style = {
            MapStyle(style = "mapbox://styles/raul2005/cmsnqjsyh01a401qo4io098fm")
        }
    )
}