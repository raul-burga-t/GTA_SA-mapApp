package com.raul.minimapagta.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.raul.minimapagta.R

@Composable
fun MapScreen() {
    val context = LocalContext.current

    // 1. Instancia de SharedPreferences para la memoria temporal
    val sharedPref = context.getSharedPreferences("GTA_Radar_Prefs", Context.MODE_PRIVATE)

    var hasLocationPermission by remember { mutableStateOf(false) }
    var isCameraRotating by remember { mutableStateOf(false) }

    // 2. Estado para almacenar las coordenadas de tu destino
    var destinationPoint by remember { mutableStateOf<Point?>(null) }

    // 3. Al abrir la app, intentamos recuperar el último destino guardado
    LaunchedEffect(Unit) {
        val lat = sharedPref.getFloat("dest_lat", Float.NaN)
        val lng = sharedPref.getFloat("dest_lng", Float.NaN)
        if (!lat.isNaN() && !lng.isNaN()) {
            destinationPoint = Point.fromLngLat(lng.toDouble(), lat.toDouble())
        }
    }

    val viewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(16.0)
            pitch(0.0)
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
                    .pitch(0.0)
                    .zoom(16.0)
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

            // Volvemos a pasar "Unit" porque leemos la imagen directamente
            MapEffect(Unit) { mapView ->

                mapView.mapboxMap.getStyle { style ->

                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.radar_waypoint)
                    style.addImage("marcador_destino", bitmap)

                }

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

            // 4. Si existe un destino en memoria, dibujamos tu ícono rojo
            destinationPoint?.let { point ->
                PointAnnotation(
                    point = point
                ) {
                    iconImage = IconImage("marcador_destino")
                    iconSize = 0.4
                    iconOpacity = 1.0
                }
            }
        }

        Image(
            painter = painterResource(id = R.drawable.mira),
            contentDescription = "Mira central",
            modifier = Modifier.align(Alignment.Center)
                .size(48.dp)
        )

        Button(
            onClick = { isCameraRotating = !isCameraRotating },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Text(if (isCameraRotating) "Fijar Cámara" else "Rotar Cámara")
        }

        // 5. Botón para capturar el centro de la mira y guardar el destino
        Button(
            onClick = {
                // El ?. protege el código y el let asegura que no sea nulo
                viewportState.cameraState?.center?.let { center ->

                    destinationPoint = center

                    // Guardamos en memoria para que resista si se cierra la app
                    sharedPref.edit()
                        .putFloat("dest_lat", center.latitude().toFloat())
                        .putFloat("dest_lng", center.longitude().toFloat())
                        .apply()
                }
            },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Text("Marcar Destino")
        }
    }
}