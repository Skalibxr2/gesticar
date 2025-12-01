package com.hans.gesticar.repository

import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.SintomaInput
import com.hans.gesticar.model.TareaOt
import com.hans.gesticar.model.Vehiculo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FakeRepositoryTest {

    @Test
    fun `crearOt normaliza rut y patente y asigna numero consecutivo`() = runTest {
        val repository = FakeRepository()
        val cliente = Cliente(rut = "12.345.678-9", nombre = "Cliente Test", correo = "test@example.com")
        val vehiculo = Vehiculo(
            clienteRut = cliente.rut,
            patente = "ij1234",
            marca = "Ford",
            modelo = "Focus",
            anio = 2019,
            color = "Gris",
            kilometraje = 15000,
            combustible = "Gasolina"
        )

        val nuevaOt = repository.crearOt(
            cliente = cliente,
            vehiculo = vehiculo,
            mecanicosIds = emptyList(),
            presupuestoItems = emptyList(),
            presupuestoAprobado = false,
            sintomas = emptyList(),
            tareas = emptyList()
        )

        assertEquals("IJ1234", nuevaOt.vehiculoPatente)
        assertEquals(repository.obtenerSiguienteNumeroOt() - 1, nuevaOt.numero)
        assertEquals("12345678-9", repository.buscarClientePorRut(cliente.rut)?.rut)
    }

    @Test
    fun `guardarPresupuesto y sintomas se persisten en la OT`() = runTest {
        val repository = FakeRepository()
        val cliente = Cliente(rut = "20.123.456-k", nombre = "Cliente Dos", correo = "otro@example.com")
        val vehiculo = Vehiculo(
            clienteRut = cliente.rut,
            patente = "kzmn87",
            marca = "Kia",
            modelo = "Rio",
            anio = 2021,
            color = "Negro",
            kilometraje = 5000,
            combustible = "Gasolina"
        )
        val sintomas = listOf(SintomaInput(descripcion = "No enciende", registradoEn = "recepcion"))
        val presupuestoItems = listOf(
            PresupuestoItem(tipo = ItemTipo.MO, descripcion = "Diagnóstico", cantidad = 1, precioUnitario = 20000)
        )

        val ot = repository.crearOt(
            cliente = cliente,
            vehiculo = vehiculo,
            mecanicosIds = emptyList(),
            presupuestoItems = presupuestoItems,
            presupuestoAprobado = true,
            sintomas = sintomas,
            tareas = listOf(TareaOt(titulo = "Revisión"))
        )

        val detalle = repository.obtenerDetalleOt(ot.id)

        assertNotNull(detalle)
        assertTrue(detalle.presupuesto.aprobado)
        assertEquals(presupuestoItems.first().descripcion, detalle.presupuesto.items.first().descripcion)
        assertEquals(sintomas.first().descripcion, detalle.sintomas.first().descripcion)
    }
}
