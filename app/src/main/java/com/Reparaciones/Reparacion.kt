package com.Reparaciones

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.UUID

@Entity(tableName = "reparaciones")
data class Reparacion(
    @PrimaryKey
    @SerializedName("_id")
    val id: String = UUID.randomUUID().toString(),

    val nombre: String,
    val apellido: String,
    val fecha: String,
    val auto: String,
    val amortiguadores: String,

    val costoFinal: Double,
    val telefono: String,

    var esSincronizado: Boolean = false,
    var esEliminado: Boolean = false
) : Serializable