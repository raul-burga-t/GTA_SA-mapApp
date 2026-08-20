package com.raul.minimapagta.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.raul.minimapagta.data.model.PuntoConDetalles
import com.raul.minimapagta.data.model.PuntoDestacadoEntity
import com.raul.minimapagta.data.model.PuntoEntity
import kotlin.jvm.JvmSuppressWildcards

@Dao
@JvmSuppressWildcards
interface PuntoDao {

    @Insert
    suspend fun insertarPunto(punto: PuntoEntity): Long

    @Insert
    suspend fun insertarPuntoDestacado(puntoDestacado: PuntoDestacadoEntity): Long

    @Query("""
        SELECT p.id, p.latitud, p.longitud, p.icono_sprite AS iconoSprite, pd.nombre, pd.descripcion 
        FROM punto p 
        INNER JOIN punto_destacado pd ON p.id = pd.id_punto
    """)
    suspend fun obtenerPuntosConDetalles(): List<PuntoConDetalles>

    // ¡CORRECCIÓN! Ahora retornan Int en lugar de nada (Unit/V)
    @Query("DELETE FROM punto WHERE id = :idPunto")
    suspend fun borrarPuntoBase(idPunto: Int): Int

    @Query("DELETE FROM punto_destacado WHERE id_punto = :idPunto")
    suspend fun borrarPuntoDestacado(idPunto: Int): Int

    @Query("UPDATE punto SET icono_sprite = :icono WHERE id = :idPunto")
    suspend fun actualizarIcono(idPunto: Int, icono: String): Int

    @Query("UPDATE punto_destacado SET nombre = :nombre WHERE id_punto = :idPunto")
    suspend fun actualizarNombre(idPunto: Int, nombre: String): Int
}

// ---------------------------------------------------------
// FUNCIONES DE EXTENSIÓN PARA ENCAPSULAR LA LÓGICA
// ---------------------------------------------------------

suspend fun PuntoDao.guardarPuntoRelevante(punto: PuntoEntity, nombre: String, descripcion: String) {
    val idPuntoGenerado = insertarPunto(punto)
    val destacado = PuntoDestacadoEntity(
        idPunto = idPuntoGenerado.toInt(),
        nombre = nombre,
        descripcion = descripcion
    )
    insertarPuntoDestacado(destacado)
}

suspend fun PuntoDao.eliminarPuntoRelevante(idPunto: Int) {
    borrarPuntoDestacado(idPunto)
    borrarPuntoBase(idPunto)
}

suspend fun PuntoDao.modificarPuntoRelevante(idPunto: Int, nombre: String, icono: String) {
    actualizarIcono(idPunto, icono)
    actualizarNombre(idPunto, nombre)
}