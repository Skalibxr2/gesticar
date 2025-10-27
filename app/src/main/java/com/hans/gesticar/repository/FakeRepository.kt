package com.hans.gesticar.repository

import com.hans.gesticar.model.AuditLog
import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.Evidencia
import com.hans.gesticar.model.EvidenciaEtapa
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.Presupuesto
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import java.util.concurrent.atomic.AtomicInteger

class FakeRepository : Repository {
    private val otCounter = AtomicInteger(999)

    val usuarios = mutableListOf(
        Usuario(
            nombre = "Admin",
            email = "admin@gesticar.cl",
            password = "admin",
            rol = Rol.ADMIN
        ),
        Usuario(
            nombre = "Mecánico Juan",
            email = "mecanico@gesticar.cl",
            password = "mecanico",
            rol = Rol.MECANICO
        )
    )
    val clientes = mutableListOf<Cliente>()
    val vehiculos = mutableListOf<Vehiculo>()
    val ots = mutableListOf<Ot>()
    val presupuestos = mutableMapOf<String, Presupuesto>() // key: otId
    val evidencias = mutableListOf<Evidencia>()
    val audit = mutableListOf<AuditLog>()

    init {
        // Seed mínimo
        val c = Cliente(rut = "12.345.678-9", nombre = "Juan Pérez", correo = "juan@example.com")
        clientes += c
        val v = Vehiculo(
            clienteRut = c.rut,
            patente = "ABCD12",
            marca = "Toyota",
            modelo = "Yaris",
            anio = 2018,
            color = "Rojo",
            kilometraje = 75000,
            combustible = "Gasolina"
        )
        vehiculos += v
        val mecanico = usuarios.first { it.rol == Rol.MECANICO }
        val ot = crearOtInternal(
            vehiculoPatente = v.patente,
            notas = "Ruidos en tren delantero",
            mecanicos = listOf(mecanico.id)
        )
        agregarItemPresupuesto(ot.id, ItemTipo.MO, "Diagnóstico", 1, 15000)
        agregarItemPresupuesto(ot.id, ItemTipo.REP, "Bujías", 4, 8000)
    }

    fun nextOtNumber(): Int = otCounter.incrementAndGet()

    override suspend fun obtenerOts(): List<Ot> = ots.toList()

    override suspend fun findUserByEmail(email: String): Usuario? =
        usuarios.firstOrNull { it.email.equals(email, ignoreCase = true) }

    fun validarPassword(usuario: Usuario, password: String): Boolean =
        usuario.password == password

    fun obtenerUsuario(id: String): Usuario? = usuarios.firstOrNull { it.id == id }

    override suspend fun obtenerMecanicos(): List<Usuario> = usuarios.filter { it.rol == Rol.MECANICO }

    override suspend fun obtenerSiguienteNumeroOt(): Int = otCounter.get() + 1

    private fun crearOtInternal(
        vehiculoPatente: String,
        notas: String?,
        mecanicos: List<String>
    ): Ot {
        val ot = Ot(
            numero = nextOtNumber(),
            vehiculoPatente = vehiculoPatente,
            notas = notas,
            mecanicosAsignados = mecanicos,
            fechaCreacion = System.currentTimeMillis()
        )
        ots += ot
        presupuestos[ot.id] = Presupuesto(otId = ot.id)
        log(ot.id, "CREAR_OT")
        return ot
    }

    override suspend fun crearOt(
        cliente: Cliente,
        vehiculo: Vehiculo,
        mecanicosIds: List<String>,
        presupuestoItems: List<PresupuestoItem>,
        presupuestoAprobado: Boolean,
        sintomas: String?
    ): Ot {
        val rutUpper = cliente.rut.uppercase()
        clientes.removeAll { it.rut.equals(rutUpper, ignoreCase = true) }
        clientes += cliente.copy(rut = rutUpper)

        val patenteUpper = vehiculo.patente.uppercase()
        vehiculos.removeAll { it.patente.equals(patenteUpper, ignoreCase = true) }
        vehiculos += vehiculo.copy(patente = patenteUpper, clienteRut = rutUpper)

        val ot = crearOtInternal(patenteUpper, sintomas, mecanicosIds)

        val presupuesto = presupuestos.getValue(ot.id)
        presupuesto.items.clear()
        presupuesto.items += presupuestoItems
        presupuesto.aprobado = presupuestoAprobado
        if (presupuestoAprobado) {
            log(ot.id, "PRESUPUESTO_APROBADO")
        }
        return ot
    }

    fun obtenerPresupuesto(otId: String): Presupuesto = presupuestos.getValue(otId)

    fun agregarItemPresupuesto(
        otId: String,
        tipo: ItemTipo,
        desc: String,
        cant: Int,
        precioUnit: Int
    ) {
        val p = obtenerPresupuesto(otId)
        p.items += PresupuestoItem(
            tipo = tipo,
            descripcion = desc,
            cantidad = cant,
            precioUnit = precioUnit
        )
        log(otId, "AGREGAR_ITEM:${tipo}/${desc}")
    }

    override suspend fun aprobarPresupuesto(otId: String) {
        val p = obtenerPresupuesto(otId)
        p.aprobado = true
        log(otId, "APROBAR_PRESUPUESTO")
    }

    override suspend fun cambiarEstado(otId: String, nuevo: OtState): Boolean {
        val ot = ots.find { it.id == otId } ?: return false
        val p = obtenerPresupuesto(otId)
        // Regla: no se puede EN_EJECUCION sin presupuesto aprobado
        if (nuevo == OtState.EN_EJECUCION && !p.aprobado) return false
        // Regla: FINALIZADA solo si ya estaba en EN_EJECUCION
        if (nuevo == OtState.FINALIZADA && ot.estado != OtState.EN_EJECUCION) return false
        ot.estado = nuevo
        log(otId, "CAMBIAR_ESTADO:${nuevo}")
        return true
    }

    override suspend fun buscarOtPorNumero(numero: Int): Ot? = ots.find { it.numero == numero }

    override suspend fun buscarOtPorPatente(patente: String): List<Ot> {
        val pat = patente.uppercase()
        return ots.filter { it.vehiculoPatente == pat }
    }

    fun agregarEvidencia(otId: String, etapa: EvidenciaEtapa, uriLocal: String) {
        evidencias += Evidencia(otId = otId, etapa = etapa, uriLocal = uriLocal)
        log(otId, "AGREGAR_EVIDENCIA:${etapa}")
    }

    fun log(otId: String, accion: String, usuarioEmail: String = "admin@gesticar.cl") {
        audit += AuditLog(otId = otId, usuarioEmail = usuarioEmail, accion = accion)
    }
}
