package com.raul.minimapagta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.raul.minimapagta.ui.screens.MapScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setContent es el puente que conecta la lógica base con tu interfaz visual
        setContent {
            // Llamamos a la función que renderiza tu mapa de Mapbox
            MapScreen()
        }
    }
}