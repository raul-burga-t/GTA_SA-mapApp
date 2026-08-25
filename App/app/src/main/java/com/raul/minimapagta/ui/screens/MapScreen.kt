package com.raul.minimapagta.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.widget.Toast
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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.raul.minimapagta.R
import com.raul.minimapagta.data.local.AppDatabase
import com.raul.minimapagta.data.local.eliminarPuntoRelevante
import com.raul.minimapagta.data.local.guardarPuntoRelevante
import com.raul.minimapagta.data.local.modificarPuntoRelevante
import com.raul.minimapagta.data.model.PuntoConDetalles
import com.raul.minimapagta.data.model.PuntoEntity
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import android.view.Gravity
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.compass.compass

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val mapboxToken = stringResource(id = R.string.mapbox_access_token)
    val sharedPref = context.getSharedPreferences("GTA_Radar_Prefs", Context.MODE_PRIVATE)

    val dao = remember { AppDatabase.getDatabase(context).puntoDao() }
    val scope = rememberCoroutineScope()

    // AHORA USA LA NUEVA CLASE HÍBRIDA (PuntoConDetalles)
    var savedPoints by remember { mutableStateOf<List<PuntoConDetalles>>(emptyList()) }

    var hasLocationPermission by remember { mutableStateOf(false) }
    var isCameraRotating by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }

    // ESTADOS DEL PANEL DE ADMINISTRACIÓN
    var isPointsAdminOpen by remember { mutableStateOf(false) }
    var pointToDelete by remember { mutableStateOf<PuntoConDetalles?>(null) }

    // ESTADOS DEL FORMULARIO (Creación y Edición)
    var isPointFormOpen by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editingPointId by remember { mutableStateOf<Int?>(null) }

    var pendingPoint by remember { mutableStateOf<Point?>(null) }
    var pointName by remember { mutableStateOf("") }

    val iconosDisponibles = remember {
        (5..63).mapNotNull { i ->
            val nombreIcono = "icon_$i"
            val resId = context.resources.getIdentifier(nombreIcono, "drawable", context.packageName)
            if (resId != 0) Pair(nombreIcono, resId) else null
        }
    }
    var selectedIcon by remember { mutableStateOf(iconosDisponibles.firstOrNull()) }

    var destinationPoint by remember { mutableStateOf<Point?>(null) }
    var routeCoordinates by remember { mutableStateOf<List<Point>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Obtenemos los puntos con nombre e ícono al arrancar
        savedPoints = dao.obtenerPuntosConDetalles()

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
            style = { MapStyle(style = "mapbox://styles/raul2005/cmsnqjsyh01a401qo4io098fm") },

        ) {
            // CAPA 0: Carga de imágenes/sprites en el estilo (no dibuja nada por sí sola)
            MapEffect(Unit) { mapView ->
                mapView.mapboxMap.getStyle { style ->
                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.radar_waypoint)
                    style.addImage("marcador_destino", bitmap)

                    iconosDisponibles.forEach { (nombre, idRecurso) ->
                        val iconBmp = BitmapFactory.decodeResource(context.resources, idRecurso)
                        style.addImage(nombre, iconBmp)
                    }
                }
            }


            // CAPA 1: RUTA
            if (routeCoordinates.isNotEmpty()) {
                PolylineAnnotation(points = routeCoordinates) {
                    lineColor = Color.Red
                    lineWidth = 6.0
                }
            }
            // Opacidad dinámica de los puntos guardados según el zoom de la cámara
            val currentZoom = viewportState.cameraState?.zoom ?: 16.0
            val zoomVisibleDesde = 7.5   // debajo de este zoom, los puntos ya casi no se ven
            val zoomVisibleHasta = 10.0   // en este zoom o más cercano, se ven al 100%

            val savedPointsOpacity = when {
                currentZoom >= zoomVisibleHasta -> 1.0
                currentZoom <= zoomVisibleDesde -> 0.0
                else -> (currentZoom - zoomVisibleDesde) / (zoomVisibleHasta - zoomVisibleDesde)
            }
            // CAPA 2: puntos-layer
            savedPoints.forEach { punto ->
                PointAnnotation(
                    point = Point.fromLngLat(punto.longitud, punto.latitud)
                ) {
                    iconImage = IconImage(punto.iconoSprite)
                    iconSize = 0.5
                    iconOpacity = savedPointsOpacity
                }
            }

            // CAPA 3: destino-layer
            destinationPoint?.let { point ->
                PointAnnotation(point = point) {
                    iconImage = IconImage("marcador_destino")
                    iconSize = 0.5
                    iconOpacity = 1.0
                }
            }

            // CAPA 4: LocationPuck — se activa AL FINAL para quedar arriba de todo lo demás
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
            MapEffect(savedPoints, destinationPoint, routeCoordinates) { mapView ->
                mapView.mapboxMap.getStyle { style ->
                    style.moveStyleLayer("mapbox-location-indicator-layer", null)
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
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Button(onClick = {
                viewportState.cameraState?.center?.let { center ->
                    pendingPoint = center
                    pointName = ""
                    selectedIcon = iconosDisponibles.firstOrNull()
                    isEditing = false
                    editingPointId = null
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
        /*Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 50.dp, end = 16.dp),
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
        }*/

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
                    Spacer(modifier = Modifier.height(24.dp))
                    // NUEVO BOTÓN DE ADMINISTRACIÓN
                    MenuListItem(texto = "Revisar puntos de interés", iconId = R.drawable.ic_flecha_menu) {
                        isMenuOpen = false
                        isPointsAdminOpen = true
                    }
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
        // CAPA 7: BARRA DE BÚSQUEDA MÚLTIPLE
        // =========================================================================
        var searchQuery by remember { mutableStateOf("") }
        var isSearchActive by remember { mutableStateOf(false) }

        if (!isMenuOpen) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 50.dp, start = 64.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                // 1. La caja de texto del buscador (Opacidad 85%)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        isSearchActive = it.isNotEmpty()
                    },
                    placeholder = { Text("Buscar ...", color = Color.Gray) },
                    leadingIcon = { Icon(painterResource(id = android.R.drawable.ic_menu_search), contentDescription = "Buscar", tint = Color.DarkGray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; isSearchActive = false }) {
                                Text("✕", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.80f), RoundedCornerShape(8.dp)), // <--- OPACIDAD AJUSTABLE
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                // 2. Resultados desplegables (Opacidad 90%)
                AnimatedVisibility(visible = isSearchActive) {
                    val resultadosLocales = savedPoints.filter {
                        it.nombre.contains(searchQuery, ignoreCase = true)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .background(Color.White.copy(alpha = 0.90f), RoundedCornerShape(8.dp)) // <--- OPACIDAD DEL DESPLEGABLE
                            .heightIn(max = 250.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text("Tus Puntos Guardados", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                            }

                            if (resultadosLocales.isEmpty()) {
                                item { Text("No se encontraron resultados", color = Color.DarkGray, modifier = Modifier.padding(8.dp)) }
                            } else {
                                items(resultadosLocales) { punto ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewportState.cameraState?.let {
                                                    viewportState.setCameraOptions {
                                                        center(Point.fromLngLat(punto.longitud, punto.latitud))
                                                        zoom(18.0)
                                                    }
                                                }
                                                searchQuery = ""
                                                isSearchActive = false
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // BUSCAMOS EL ÍCONO CORRESPONDIENTE (icon_5 hasta icon_63)
                                        val iconoRecurso = iconosDisponibles.find { it.first == punto.iconoSprite }?.second
                                        if (iconoRecurso != null) {
                                            Image(
                                                painter = painterResource(id = iconoRecurso),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(28.dp))
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(punto.nombre, color = Color.Black, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // PANTALLA: ADMINISTRAR PUNTOS DE INTERÉS
        // =========================================================================
        AnimatedVisibility(
            visible = isPointsAdminOpen,
            enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000))
                    .clickable(enabled = false) {}
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                    Text("Puntos de Interés", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Encabezado de la tabla
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nombre", color = Color.LightGray, modifier = Modifier.weight(2f))
                        Text("Ícono", color = Color.LightGray, modifier = Modifier.weight(1f))
                        Text("Acciones", color = Color.LightGray, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Lista de puntos (Tabla)
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(savedPoints) { punto ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(punto.nombre, color = Color.White, modifier = Modifier.weight(2f))

                                val iconoRecurso = iconosDisponibles.find { it.first == punto.iconoSprite }?.second
                                if (iconoRecurso != null) {
                                    Image(
                                        painter = painterResource(id = iconoRecurso),
                                        contentDescription = null,
                                        modifier = Modifier.weight(1f).size(32.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    // BOTÓN MODIFICAR
                                    IconButton(onClick = {
                                        pointName = punto.nombre
                                        selectedIcon = iconosDisponibles.find { it.first == punto.iconoSprite }
                                        editingPointId = punto.id
                                        isEditing = true
                                        isPointsAdminOpen = false
                                        isPointFormOpen = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Green)
                                    }
                                    // BOTÓN ELIMINAR
                                    IconButton(onClick = { pointToDelete = punto }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { isPointsAdminOpen = false }) { Text("Cerrar Panel") }
                }
            }
        }

        // =========================================================================
        // FORMULARIO: AGREGAR / EDITAR PUNTO
        // =========================================================================
        AnimatedVisibility(
            visible = isPointFormOpen,
            enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()
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
                    Text(
                        text = if (isEditing) "Modificar Punto" else "Nuevo Punto Relevante",
                        color = Color.White, fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = pointName,
                        onValueChange = { pointName = it },
                        label = { Text("Nombre del lugar", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White, unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Selecciona un ícono:", color = Color.White, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 64.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)
                    ) {
                        items(iconosDisponibles) { iconPair ->
                            val isSelected = selectedIcon?.first == iconPair.first
                            Box(
                                modifier = Modifier
                                    .padding(4.dp).size(64.dp).clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x40FFFFFF) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedIcon = iconPair }.padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = iconPair.second),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                isPointFormOpen = false
                                if (isEditing) isPointsAdminOpen = true // Si estaba editando, regresa a la tabla
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancelar") }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    if (isEditing && editingPointId != null) {
                                        // FLUJO ACTUALIZAR
                                        dao.modificarPuntoRelevante(editingPointId!!, pointName, selectedIcon?.first ?: "icon_5")
                                        isPointFormOpen = false
                                        isPointsAdminOpen = true // Regresa a la tabla
                                    } else {
                                        // FLUJO GUARDAR (Con regla de 5 metros)
                                        pendingPoint?.let { nuevoPunto ->
                                            val isTooClose = savedPoints.any { existente ->
                                                val pt = Point.fromLngLat(existente.longitud, existente.latitud)
                                                TurfMeasurement.distance(nuevoPunto, pt, TurfConstants.UNIT_METERS) < 5.0
                                            }

                                            if (isTooClose) {
                                                Toast.makeText(context, "Muy cerca de otro punto (Mínimo 5m)", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val entidad = PuntoEntity(
                                                    latitud = nuevoPunto.latitude(), longitud = nuevoPunto.longitude(),
                                                    iconoSprite = selectedIcon?.first ?: "icon_5"
                                                )
                                                dao.guardarPuntoRelevante(entidad, pointName, "Sin descripción")
                                                isPointFormOpen = false
                                            }
                                        }
                                    }
                                    // Recarga la lista en ambos casos
                                    savedPoints = dao.obtenerPuntosConDetalles()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = pointName.isNotBlank() && selectedIcon != null
                        ) { Text("Aceptar") }
                    }
                }
            }
        }

        // =========================================================================
        // CUADRO DE ADVERTENCIA PARA ELIMINAR
        // =========================================================================
        AnimatedVisibility(
            visible = pointToDelete != null,
            enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.background(Color(0xFF222222), RoundedCornerShape(16.dp)).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("¿Eliminar '${pointToDelete?.nombre}'?", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Esta acción no se puede deshacer.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row {
                        Button(onClick = { pointToDelete = null }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            onClick = {
                                scope.launch {
                                    pointToDelete?.let { dao.eliminarPuntoRelevante(it.id) }
                                    savedPoints = dao.obtenerPuntosConDetalles()
                                    pointToDelete = null
                                }
                            }
                        ) { Text("Eliminar") }
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
        Icon(painter = painterResource(id = iconId), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = texto, color = Color.White, fontSize = 18.sp)
    }
}