package com.hans.gesticar.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hans.gesticar.model.TareaEstado
import com.hans.gesticar.model.TareaOt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val taskDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

fun formatTaskDate(timestamp: Long?): String = timestamp?.let { taskDateFormatter.format(Date(it)) }.orEmpty()

fun parseTaskDate(text: String): Long? = text.takeIf { it.isNotBlank() }?.let {
    runCatching { taskDateFormatter.parse(it)?.time }.getOrNull()
}

fun defaultCreationDate(): String = formatTaskDate(System.currentTimeMillis())

class EditableTaskState(
    val id: String = UUID.randomUUID().toString(),
    descripcion: String = "",
    detalle: String = "",
    fechaCreacion: String = defaultCreationDate(),
    fechaInicio: String = "",
    fechaTermino: String = "",
    estado: TareaEstado = TareaEstado.CREADA
) {
    var descripcion by mutableStateOf(descripcion)
    var detalle by mutableStateOf(detalle)
    var fechaCreacion by mutableStateOf(fechaCreacion)
    var fechaInicio by mutableStateOf(fechaInicio)
    var fechaTermino by mutableStateOf(fechaTermino)
    var estado by mutableStateOf(estado)
    var estadoMenuExpanded by mutableStateOf(false)
}

fun EditableTaskState.toTareaOt(): TareaOt {
    val fechaCreacionEpoch = parseTaskDate(fechaCreacion) ?: System.currentTimeMillis()
    return TareaOt(
        id = id,
        titulo = descripcion.trim(),
        descripcion = detalle.takeIf(String::isNotBlank)?.trim(),
        fechaCreacion = fechaCreacionEpoch,
        fechaInicio = parseTaskDate(fechaInicio),
        fechaTermino = parseTaskDate(fechaTermino),
        estado = estado
    )
}

fun TareaOt.toEditableTaskState(): EditableTaskState = EditableTaskState(
    id = id,
    descripcion = titulo,
    detalle = descripcion.orEmpty(),
    fechaCreacion = formatTaskDate(fechaCreacion),
    fechaInicio = formatTaskDate(fechaInicio),
    fechaTermino = formatTaskDate(fechaTermino),
    estado = estado
)

fun TareaEstado.toReadableName(): String = when (this) {
    TareaEstado.CREADA -> "Creada"
    TareaEstado.INICIADA -> "Iniciada"
    TareaEstado.CANCELADA -> "Cancelada"
    TareaEstado.TERMINADA -> "Terminada"
    TareaEstado.TERMINADA_INCOMPLETA -> "Terminada incompleta"
}
