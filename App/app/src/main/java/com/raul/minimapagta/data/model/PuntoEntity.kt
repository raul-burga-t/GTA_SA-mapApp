package com.raul.minimapagta.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. Tabla base para cualquier punto en el mapa
@Entity(tableName = "punto")
data class PuntoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val latitud: Double,
    val longitud: Double,

    @ColumnInfo(name = "icono_sprite")
    val iconoSprite: String
)

// 2. Tabla específica para los puntos destacados (Relacionada 1:1)
@Entity(tableName = "punto_destacado")
data class PuntoDestacadoEntity(
    // Este ID no es autogenerado, es la llave foránea que conecta con "punto"
    @PrimaryKey
    @ColumnInfo(name = "id_punto")
    val idPunto: Int,

    val nombre: String,
    val descripcion: String
)