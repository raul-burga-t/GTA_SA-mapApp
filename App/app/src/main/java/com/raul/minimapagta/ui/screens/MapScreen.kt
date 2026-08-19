package com.raul.minimapagta.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
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

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val mapboxToken = stringResource(id = R.string.mapbox_access_token)
    val sharedPref = context.getSharedPreferences("GTA_Radar_Prefs", Context.MODE_PRIVATE)

    var hasLocationPermission by remember { mutableStateOf(false) }
    var isCameraRotating by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }

    // ESTADOS PARA EL FORMULARIO (CU-03)
    var isPointFormOpen by remember { mutableStateOf(false) }
    var pendingPoint by remember { mutableStateOf<Point?>(null) }
    var pointName by remember { mutableStateOf("") }

    // Lista de tus 63 íconos
    val iconosDisponibles = remember {
        listOf(
            R.drawable.icon_2, // Tu ícono actual de prueba
            R.drawable.radar_waypoint,
            R.drawable.icon_0,
            R.drawable.icon_5,
            R.drawable.icon_35,
        )
    }
    // Estado para guardar el ID numérico del ícono seleccionado (por defecto el primero)
    var selectedIconRes by remember { mutableStateOf(iconosDisponibles.firstOrNull() ?: 0) }

    var destinationPoint by remember { mutableStateOf<Point?>(null) }
    var routeCoordinates by remember { mutableStateOf<List<Point>>(emptyList()) }

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

    LaunchedEffect(destinationPoint) {
        destinationPoint?.let { dest ->
            if (hasLocationPermission) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                lastLocation?.let { loc ->
                    val origin = Point.fromLngLat(loc.longitude, loc.latitude)
                    val distance = TurfMeasurement.distance(origin, dest, TurfConstants.UNIT_METERS)

                    if (distance < 15.0) {
                        destinationPoint = null
                        routeCoordinates = emptyList()
                        sharedPref.edit().remove("dest_lat").remove("dest_lng").apply()
                    } else {
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
                                currentRoute?.geometry()?.let { geometry ->
                                    routeCoordinates = PolylineUtils.decode(geometry, 6)
                                }
                            }
                            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {}
                        })
                    }
                }
            }
        } ?: run {
            routeCoordinates = emptyList()
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

        // CAPA 1: EL MAPA
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            style = { MapStyle(style = "mapbox://styles/raul2005/cmsnqjsyh01a401qo4io098fm") }
        ) {
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

            if (routeCoordinates.isNotEmpty()) {
                PolylineAnnotation(points = routeCoordinates) {
                    lineColor = Color.Red
                    lineWidth = 6.0
                }
            }

            destinationPoint?.let { point ->
                PointAnnotation(point = point) {
                    iconImage = IconImage("marcador_destino")
                    iconSize = 0.5
                    iconOpacity = 1.0
                }
            }
        }

        // CAPA 2: MIRA CENTRAL
        Image(
            painter = painterResource(id = R.drawable.mira),
            contentDescription = "Mira central",
            modifier = Modifier.align(Alignment.Center).size(48.dp)
        )

        // CAPA 3: BOTONES INFERIORES IZQUIERDOS
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Button(onClick = {
                viewportState.cameraState?.center?.let { center ->
                    pendingPoint = center
                    // Al abrir, seleccionamos por defecto el primer ícono de la lista
                    selectedIconRes = iconosDisponibles.firstOrNull() ?: 0
                    isPointFormOpen = true
                }
            }) {
                Text("Agregar Punto de Interés")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                viewportState.cameraState?.center?.let { center ->
                    destinationPoint = center
                    sharedPref.edit()
                        .putFloat("dest_lat", center.latitude().toFloat())
                        .putFloat("dest_lng", center.longitude().toFloat())
                        .apply()
                }
            }) {
                Text("Marcar Destino")
            }
        }

        // CAPA 4: CONTROLES CÁMARA
        Button(
            onClick = { isCameraRotating = !isCameraRotating },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Text(if (isCameraRotating) "Fijar Cámara" else "Rotar Cámara")
        }

        // CAPA 5: BOTONES SUPERIORES DERECHOS
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 50.dp, end = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Button(onClick = { /* Form Misión */ }) { Text("Agregar Misión") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* Form Rutina */ }) { Text("Agregar Rutina") }

            if (destinationPoint != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    destinationPoint = null
                    routeCoordinates = emptyList()
                    sharedPref.edit().remove("dest_lat").remove("dest_lng").apply()
                }) { Text("Quitar Destino") }
            }
        }

        // CAPA 6: MENÚ LATERAL
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.6f)
                    .background(Color(0xE6000000))
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    MenuListItem(texto = "Revisar Misiones", iconId = R.drawable.ic_flecha_menu) {}
                    Spacer(modifier = Modifier.height(24.dp))
                    MenuListItem(texto = "Revisar Rutinas", iconId = R.drawable.ic_flecha_menu) {}
                }
            }
        }

        IconButton(
            onClick = { isMenuOpen = !isMenuOpen },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu_lineas),
                contentDescription = "Abrir menú",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // =========================================================================
        // FORMULARIO CU-03 CON GRILLA DE ÍCONOS
        // =========================================================================
        AnimatedVisibility(
            visible = isPointFormOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000))
                    .clickable(enabled = false) {}
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Nuevo Punto Relevante", color = Color.White, fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = pointName,
                        onValueChange = { pointName = it },
                        label = { Text("Nombre del lugar", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Selecciona un ícono:", color = Color.White, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))

                    // GRILLA DE ÍCONOS SCROLLEABLE
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 64.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp) // Limita la altura para que no tape los botones de abajo
                    ) {
                        items(iconosDisponibles) { iconRes ->
                            val isSelected = selectedIconRes == iconRes

                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x40FFFFFF) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedIconRes = iconRes }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (iconRes != 0) {
                                    Image(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                isPointFormOpen = false
                                pointName = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                // TODO: Guardar en Room usando pendingPoint, pointName y selectedIconRes
                                isPointFormOpen = false
                                pointName = ""
                            },
                            modifier = Modifier.weight(1f),
                            enabled = pointName.isNotBlank() && selectedIconRes != 0
                        ) {
                            Text("Aceptar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuListItem(texto: String, iconId: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = texto,
            color = Color.White,
            fontSize = 18.sp
        )
    }
}