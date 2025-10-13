package com.hans.gesticar.model


import java.util.UUID
import java.time.Instant


// Usuario mínimo para admin login mock
enum class Rol { ADMIN, MECANICO }


data class Usuario(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val email: String,
    val rol: Rol
)


data class Cliente(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val telefono: String? = null,
    val email: String? = null
)


data class Vehiculo(
    val id: String = UUID.randomUUID().toString(),
    val clienteId: String,
    val patente: String, // Ej.: ABCD12
    val marca: String,
    val modelo: String,
    val anio: Int,
    val color: String? = null,
    val kilometraje: Int? = null,
    val combustible: String? = null, // Ej.: Gasolina/Diesel/Híbrido/Eléctrico
    val observaciones: String? = null
)


data class PresupuestoItem(
    val id: String = UUID.randomUUID().toString(),
    val tipo: ItemTipo,
    val descripcion: String,
    val cantidad: Int,
    val precioUnit: Int // CLP en entero para MVP
) {
    val total: Int get() = cantidad * precioUnit
}


data class Presupuesto(
    val otId: String,
    val items: MutableList<PresupuestoItem> = mutableListOf(),
    var aprobado: Boolean = false,
    var ivaPorc: Int = 19 // CLP-Chile por defecto
) {
    val subtotalRep: Int get() = items.filter { it.tipo == ItemTipo.REP }.sumOf { it.total }
    val subtotalMo: Int get() = items.filter { it.tipo == ItemTipo.MO }.sumOf { it.total }
    val subtotal: Int get() = subtotalRep + subtotalMo
    val iva: Int get() = (subtotal * ivaPorc) / 100
    val total: Int get() = subtotal + iva
}
data class Evidencia(
    val id: String = UUID.randomUUID().toString(),
    val otId: String,
    val etapa: EvidenciaEtapa,
    val uriLocal: String,
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val otId: String,
    val usuarioEmail: String,
    val accion: String,
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class Ot(
    val id: String = UUID.randomUUID().toString(),
    val numero: Int, // Número visible de OT
    val vehiculoId: String,
    var estado: OtState = OtState.BORRADOR,
    var mecanicoAsignadoId: String? = null,
    var notas: String? = null
)