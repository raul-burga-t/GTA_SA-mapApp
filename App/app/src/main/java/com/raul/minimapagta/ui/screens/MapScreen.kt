package com.raul.minimapagta.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.LocationManager
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.raul.minimapagta.R
import com.mapbox.geojson.utils.PolylineUtils


/**
 * Pantalla principal del minimapa estilo GTA San Andreas.
 * Maneja la ubicación del usuario, la renderización del mapa personalizado,
 * la creación de waypoints (destinos) y el cálculo de la ruta más corta en tiempo real.
 */
@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current

    // Extracción segura del token de Mapbox para solicitudes a la API
    val mapboxToken = stringResource(id = R.string.mapbox_access_token)

    // =========================================================================
    // 1. GESTIÓN DE ESTADOS (STATE MANAGEMENT)
    // =========================================================================

    // Caché local para persistir el waypoint temporal en caso de que la app se cierre
    val sharedPref = context.getSharedPreferences("GTA_Radar_Prefs", Context.MODE_PRIVATE)

    // Estados de permisos y cámara
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isCameraRotating by remember { mutableStateOf(false) }

    // Estados de enrutamiento (CU-02: Poner punto en el mapa)
    var destinationPoint by remember { mutableStateOf<Point?>(null) }
    var routeCoordinates by remember { mutableStateOf<List<Point>>(emptyList()) }

    // Configuración inicial de la cámara del mapa (vista cenital 2D)
    val viewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(16.0)
            pitch(0.0) // 0.0 fuerza la vista plana 2D clásica
        }
    }

    // Manejador de solicitud de permisos de ubicación
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    // =========================================================================
    // 2. EFECTOS SECUNDARIOS Y LÓGICA DE NEGOCIO (LAUNCHED EFFECTS)
    // =========================================================================

    // Inicialización: Verificar permisos y cargar waypoint guardado en caché
    LaunchedEffect(Unit) {
        val lat = sharedPref.getFloat("dest_lat", Float.NaN)
        val lng = sharedPref.getFloat("dest_lng", Float.NaN)
        if (!lat.isNaN() && !lng.isNaN()) {
            destinationPoint = Point.fromLngLat(lng.toDouble(), lat.toDouble())
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Motor de Enrutamiento: Se ejecuta cada vez que el usuario marca o cambia un destino
    LaunchedEffect(destinationPoint) {
        destinationPoint?.let { dest ->
            if (hasLocationPermission) {
                // Obtener la ubicación actual del dispositivo
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                lastLocation?.let { loc ->
                    val origin = Point.fromLngLat(loc.longitude, loc.latitude)

                    // Validar la distancia geométrica entre el jugador y el destino
                    val distance = TurfMeasurement.distance(origin, dest, TurfConstants.UNIT_METERS)

                    // Excepción de llegada: Si el jugador está a menos de 15m, el destino se borra automáticamente
                    if (distance < 15.0) {
                        destinationPoint = null
                        routeCoordinates = emptyList()
                        sharedPref.edit().remove("dest_lat").remove("dest_lng").apply()
                    } else {
                        // Solicitar la ruta más rápida mediante la API Directions de Mapbox
                        val client = MapboxDirections.builder()
                            .accessToken(mapboxToken)
                            .origin(origin)
                            .destination(dest)
                            .overview(DirectionsCriteria.OVERVIEW_FULL)
                            .profile(DirectionsCriteria.PROFILE_DRIVING)
                            .build()

                        client.enqueueCall(object : Callback<DirectionsResponse> {
                            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                                val currentRoute = response.body()?.routes()?.firstOrNull()
                                // Transformar la respuesta GeoJSON en una lista de puntos para dibujar la línea
                                currentRoute?.geometry()?.let { geometry ->
                                    routeCoordinates = com.mapbox.geojson.utils.PolylineUtils.decode(geometry, 6)
                                }
                            }
                            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                                // Pendiente: Manejo de errores de red o zonas inaccesibles
                            }
                        })
                    }
                }
            }
        } ?: run {
            // Limpiar la ruta visual si no hay destino establecido
            routeCoordinates = emptyList()
        }
    }

    // Comportamiento de la cámara: Seguir al usuario con o sin rotación según la brújula
    LaunchedEffect(hasLocationPermission, isCameraRotating) {
        if (hasLocationPermission) {
            val bearingMode = if (isCameraRotating) {
                FollowPuckViewportStateBearing.SyncWithLocationPuck
            } else {
                FollowPuckViewportStateBearing.Constant(0.0) // Norte fijo
            }

            viewportState.transitionToFollowPuckState(
                followPuckViewportStateOptions = FollowPuckViewportStateOptions.Builder()
                    .bearing(bearingMode)
                    .pitch(0.0) // Mantiene la estética cenital 2D durante el movimiento
                    .zoom(16.0)
                    .build()
            )
        }
    }

    // =========================================================================
    // 3. RENDERIZADO DE LA INTERFAZ DE USUARIO (UI)
    // =========================================================================

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {

        // Contenedor principal del Mapa
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            style = {
                // Aplicación del estilo visual predefinido en Mapbox Studio
                MapStyle(style = "mapbox://styles/raul2005/cmsnqjsyh01a401qo4io098fm")
            }
        ) {
            // Carga de recursos asíncronos y configuración del ícono del jugador (Puck)
            MapEffect(Unit) { mapView ->
                mapView.mapboxMap.getStyle { style ->
                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.radar_waypoint)
                    style.addImage("marcador_destino", bitmap)
                }

                mapView.location.updateSettings {
                    enabled = true
                    locationPuck = LocationPuck2D(
                        bearingImage = ImageHolder.from(R.drawable.icon_2), // Sprite del jugador
                        scaleExpression = "3.0"
                    )
                    puckBearingEnabled = true
                    puckBearing = PuckBearing.COURSE
                }
            }

            // Capa 1: Dibujar la línea de la ruta
            if (routeCoordinates.isNotEmpty()) {
                PolylineAnnotation(
                    points = routeCoordinates
                ) {
                    lineColor = androidx.compose.ui.graphics.Color.Red
                    lineWidth = 6.0
                }
            }

            // Capa 2: Dibujar el marcador de destino
            destinationPoint?.let { point ->
                PointAnnotation(
                    point = point
                ) {
                    iconImage = IconImage("marcador_destino") // Usando la imagen cargada en el MapEffect
                    iconSize = 0.5
                    iconOpacity = 1.0
                }
            }
        }
        // Control para quitar el Waypoint (Superior Derecha)
        if (destinationPoint != null) {
            Button(
                onClick = {
                    destinationPoint = null
                    routeCoordinates = emptyList()
                    sharedPref.edit().remove("dest_lat").remove("dest_lng").apply()
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Text("Quitar Destino")
            }
        }

        // Interfaz Superpuesta: Mira central fija para seleccionar ubicaciones
        Image(
            painter = painterResource(id = R.drawable.mira),
            contentDescription = "Mira central",
            modifier = Modifier.align(Alignment.Center).size(48.dp)
        )

        // Controles de Cámara (Inferior Derecha)
        Button(
            onClick = { isCameraRotating = !isCameraRotating },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Text(if (isCameraRotating) "Fijar Cámara" else "Rotar Cámara")
        }

        // Control para crear el Waypoint (Inferior Izquierda)
        Button(
            onClick = {
                // Captura las coordenadas exactas a las que apunta la mira central
                viewportState.cameraState?.center?.let { center ->
                    destinationPoint = center

                    // Persiste las coordenadas en SharedPreferences
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