package com.raul.minimapagta.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.raul.minimapagta.data.model.PuntoDestacadoEntity
import com.raul.minimapagta.data.model.PuntoEntity

@Dao
interface PuntoDao {

    // Inserta el punto base y devuelve el ID generado automáticamente
    @Insert
    suspend fun insertarPunto(punto: PuntoEntity): Long

    // Inserta los detalles del punto destacado usando el ID anterior
    @Insert
    suspend fun insertarPuntoDestacado(puntoDestacado: PuntoDestacadoEntity)

    // Función principal que usará la App: Guarda ambas tablas de un solo golpe
    @Transaction
    suspend fun guardarPuntoRelevante(punto: PuntoEntity, nombre: String, descripcion: String) {
        // 1. Guarda la coordenada
        val idPuntoGenerado = insertarPunto(punto)

        // 2. Guarda el texto asociándolo a la coordenada
        val destacado = PuntoDestacadoEntity(
            idPunto = idPuntoGenerado.toInt(),
            nombre = nombre,
            descripcion = descripcion
        )
        insertarPuntoDestacado(destacado)
    }

    // Obtener todos los puntos para dibujarlos en el mapa al cargar
    @Query("SELECT * FROM punto")
    suspend fun obtenerTodosLosPuntos(): List<PuntoEntity>
}