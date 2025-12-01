package com.hans.gesticar.network.dto

import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtDetalle
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.Presupuesto
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.SintomaOt
import com.hans.gesticar.model.TareaEstado
import com.hans.gesticar.model.TareaOt
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import com.hans.gesticar.util.normalizeRut
import java.util.UUID

// --- Auth ---

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String?,
    val usuario: UsuarioDto?
)

// --- Usuarios / Personas ---

data class UsuarioDto(
    val id: String?,
    val nombre: String?,
    val email: String?,
    val rol: String?,
    val password: String? = null
) {
    fun toDomain(fallbackRol: Rol = Rol.MECANICO): Usuario = Usuario(
        id = id ?: UUID.randomUUID().toString(),
        nombre = nombre.orEmpty(),
        email = email.orEmpty(),
        password = password.orEmpty(),
        rol = rol?.let { runCatching { Rol.valueOf(it.uppercase()) }.getOrNull() } ?: fallbackRol
    )
}

data class ClienteDto(
    val rut: String?,
    val nombre: String?,
    val correo: String? = null,
    val direccion: String? = null,
    val comuna: String? = null,
    val telefono: String? = null
) {
    fun toDomain(): Cliente? {
        val rutNormalizado = rut?.let(::normalizeRut) ?: return null
        return Cliente(
            rut = rutNormalizado,
            nombre = nombre.orEmpty(),
            correo = correo,
            direccion = direccion,
            comuna = comuna,
            telefono = telefono
        )
    }
}

// --- Vehículos ---

data class VehiculoDto(
    val patente: String?,
    val clienteRut: String?,
    val marca: String?,
    val modelo: String?,
    val anio: Int?,
    val color: String? = null,
    val kilometraje: Int? = null,
    val combustible: String? = null
) {
    fun toDomain(): Vehiculo? {
        val plate = patente?.uppercase() ?: return null
        val rut = clienteRut?.let(::normalizeRut) ?: return null
        return Vehiculo(
            patente = plate,
            clienteRut = rut,
            marca = marca.orEmpty(),
            modelo = modelo.orEmpty(),
            anio = anio ?: 0,
            color = color,
            kilometraje = kilometraje,
            combustible = combustible
        )
    }
}

// --- Presupuestos ---

data class PresupuestoDto(
    val otId: String?,
    val items: List<PresupuestoItemDto> = emptyList(),
    val aprobado: Boolean = false,
    val ivaPorc: Int = 19
) {
    fun toDomain(): Presupuesto? {
        val id = otId ?: return null
        val presupuesto = Presupuesto(otId = id)
        presupuesto.items.clear()
        presupuesto.items.addAll(items.mapNotNull { it.toDomain() })
        presupuesto.aprobado = aprobado
        presupuesto.ivaPorc = ivaPorc
        return presupuesto
    }
}

data class PresupuestoItemDto(
    val id: String? = null,
    val tipo: String?,
    val descripcion: String?,
    val cantidad: Int?,
    val precioUnit: Int?
) {
    fun toDomain(): PresupuestoItem? {
        val tipoEnum = tipo?.let { runCatching { ItemTipo.valueOf(it.uppercase()) }.getOrNull() } ?: return null
        val qty = cantidad ?: return null
        val price = precioUnit ?: return null
        return PresupuestoItem(
            id = id ?: UUID.randomUUID().toString(),
            tipo = tipoEnum,
            descripcion = descripcion.orEmpty(),
            cantidad = qty,
            precioUnit = price
        )
    }
}

// --- Órdenes ---

data class OtDto(
    val id: String?,
    val numero: Int?,
    val vehiculoPatente: String?,
    val estado: String? = null,
    val mecanicosAsignados: List<String>? = null,
    val notas: String? = null,
    val fechaCreacion: Long? = null
) {
    fun toDomain(): Ot? {
        val numeroOt = numero ?: return null
        val patente = vehiculoPatente ?: return null
        val otEstado = estado?.let { runCatching { OtState.valueOf(it.uppercase()) }.getOrNull() } ?: OtState.BORRADOR
        return Ot(
            id = id ?: UUID.randomUUID().toString(),
            numero = numeroOt,
            vehiculoPatente = patente,
            estado = otEstado,
            mecanicosAsignados = mecanicosAsignados ?: emptyList(),
            notas = notas,
            fechaCreacion = fechaCreacion ?: System.currentTimeMillis()
        )
    }
}

data class OtDetalleDto(
    val ot: OtDto?,
    val cliente: ClienteDto? = null,
    val vehiculo: VehiculoDto? = null,
    val mecanicos: List<UsuarioDto> = emptyList(),
    val presupuesto: PresupuestoDto? = null,
    val tareas: List<TareaDto> = emptyList(),
    val sintomas: List<SintomaDto> = emptyList()
) {
    fun toDomain(): OtDetalle? {
        val otDomain = ot?.toDomain() ?: return null
        val presupuestoDomain = presupuesto?.toDomain() ?: Presupuesto(otDomain.id)
        return OtDetalle(
            ot = otDomain,
            cliente = cliente?.toDomain(),
            vehiculo = vehiculo?.toDomain(),
            mecanicosAsignados = mecanicos.map { it.toDomain() },
            presupuesto = presupuestoDomain,
            tareas = tareas.mapNotNull { it.toDomain() },
            sintomas = sintomas.mapNotNull { it.toDomain(otDomain.id) }
        )
    }
}

// --- Tareas y síntomas ---

data class TareaDto(
    val id: String? = null,
    val titulo: String?,
    val descripcion: String? = null,
    val fechaCreacion: Long? = null,
    val fechaInicio: Long? = null,
    val fechaTermino: Long? = null,
    val estado: String? = null
) {
    fun toDomain(): TareaOt? {
        val taskState = estado?.let { runCatching { TareaEstado.valueOf(it.uppercase()) }.getOrNull() } ?: TareaEstado.CREADA
        return TareaOt(
            id = id ?: UUID.randomUUID().toString(),
            titulo = titulo.orEmpty(),
            descripcion = descripcion,
            fechaCreacion = fechaCreacion ?: System.currentTimeMillis(),
            fechaInicio = fechaInicio,
            fechaTermino = fechaTermino,
            estado = taskState
        )
    }
}

data class SintomaDto(
    val id: String? = null,
    val descripcion: String?,
    val registradoEn: Long? = null,
    val fotos: List<String> = emptyList()
) {
    fun toDomain(otId: String): SintomaOt? {
        return SintomaOt(
            id = id ?: UUID.randomUUID().toString(),
            otId = otId,
            descripcion = descripcion.orEmpty(),
            registradoEn = registradoEn,
            fotos = fotos
        )
    }
}

// --- API externa ---

data class ExternalVehicleDto(
    val marca: String? = null,
    val modelo: String? = null,
    val anio: Int? = null,
    val color: String? = null,
    val kilometraje: Int? = null,
    val combustible: String? = null,
    val clienteRut: String? = null
) {
    fun toVehiculo(patente: String): Vehiculo? {
        val rut = clienteRut?.let(::normalizeRut) ?: return null
        val year = anio ?: return null
        return Vehiculo(
            patente = patente.uppercase(),
            clienteRut = rut,
            marca = marca.orEmpty(),
            modelo = modelo.orEmpty(),
            anio = year,
            color = color,
            kilometraje = kilometraje,
            combustible = combustible
        )
    }
}
