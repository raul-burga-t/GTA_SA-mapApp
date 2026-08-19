package com.raul.minimapagta.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.raul.minimapagta.data.model.PuntoDestacadoEntity
import com.raul.minimapagta.data.model.PuntoEntity
import kotlin.jvm.JvmSuppressWildcards

@Dao
@JvmSuppressWildcards // <-- ESTO EVITA EL CHOQUE DE CONTINUATION ENTRE KOTLIN Y JAVA
interface PuntoDao {

    @Insert
    suspend fun insertarPunto(punto: PuntoEntity): Long

    @Insert
    suspend fun insertarPuntoDestacado(puntoDestacado: PuntoDestacadoEntity): Long

    @Query("SELECT * FROM punto")
    suspend fun obtenerTodosLosPuntos(): List<PuntoEntity>
}

/**
 * FUNCION DE EXTENSIÓN:
 * Mantenemos la lógica fuera del @Dao para que no choque con la máquina virtual de Java.
 */
suspend fun PuntoDao.guardarPuntoRelevante(punto: PuntoEntity, nombre: String, descripcion: String) {
    // 1. Guardamos la coordenada y obtenemos su ID
    val idPuntoGenerado = insertarPunto(punto)

    // 2. Guardamos la información
    val destacado = PuntoDestacadoEntity(
        idPunto = idPuntoGenerado.toInt(),
        nombre = nombre,
        descripcion = descripcion
    )

    insertarPuntoDestacado(destacado)
}