package com.hans.gesticar.repository

import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.OtDetalle
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import com.hans.gesticar.model.TareaOt

interface Repository {
    suspend fun obtenerOts(): List<Ot>
    suspend fun findUserByEmail(email: String): Usuario?
    suspend fun buscarOtPorNumero(numero: Int): Ot?
    suspend fun buscarOtPorPatente(patente: String): List<Ot>
    suspend fun buscarOtPorRut(rut: String): List<Ot>
    suspend fun buscarOtPorEstado(estado: OtState): List<Ot>
    suspend fun aprobarPresupuesto(otId: String)
    suspend fun cambiarEstado(otId: String, nuevo: OtState): Boolean
    suspend fun obtenerMecanicos(): List<Usuario>
    suspend fun obtenerSiguienteNumeroOt(): Int
    suspend fun obtenerDetalleOt(otId: String): OtDetalle?
    suspend fun buscarClientePorRut(rut: String): Cliente?
    suspend fun guardarCliente(cliente: Cliente)
    suspend fun obtenerVehiculosPorRut(rut: String): List<Vehiculo>
    suspend fun guardarVehiculo(vehiculo: Vehiculo)
    suspend fun actualizarNotasOt(otId: String, notas: String?)
    suspend fun actualizarMecanicosOt(otId: String, mecanicosIds: List<String>)
    suspend fun actualizarVehiculoOt(otId: String, patente: String): Boolean
    suspend fun guardarPresupuesto(otId: String, items: List<PresupuestoItem>, aprobado: Boolean, ivaPorc: Int)
    suspend fun guardarTareas(otId: String, tareas: List<TareaOt>)
    suspend fun crearOt(
        cliente: Cliente,
        vehiculo: Vehiculo,
        mecanicosIds: List<String>,
        presupuestoItems: List<PresupuestoItem>,
        presupuestoAprobado: Boolean,
        sintomas: String?
    ): Ot
}
