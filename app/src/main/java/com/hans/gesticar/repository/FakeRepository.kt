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
import com.hans.gesticar.util.normalizeRut
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
        ),
        Usuario(
            nombre = "Mecánica Ana",
            email = "ana@gesticar.cl",
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
        val clienteUno = Cliente(rut = normalizeRut("12345678-9"), nombre = "Juan Pérez", correo = "juan@example.com")
        clientes += clienteUno
        val vehiculoUno = Vehiculo(
            clienteRut = clienteUno.rut,
            patente = "ABCD12",
            marca = "Toyota",
            modelo = "Yaris",
            anio = 2018,
            color = "Rojo",
            kilometraje = 75000,
            combustible = "Gasolina"
        )
        vehiculos += vehiculoUno

        val clienteDos = Cliente(rut = normalizeRut("20123456-K"), nombre = "María González", correo = "maria@example.com")
        clientes += clienteDos
        val vehiculoDos = Vehiculo(
            clienteRut = clienteDos.rut,
            patente = "EFGH34",
            marca = "Hyundai",
            modelo = "Accent",
            anio = 2020,
            color = "Azul",
            kilometraje = 40000,
            combustible = "Gasolina"
        )
        vehiculos += vehiculoDos

        val mecanicos = usuarios.filter { it.rol == Rol.MECANICO }
        val ot = crearOtInternal(
            vehiculoPatente = vehiculoUno.patente,
            notas = "Ruidos en tren delantero",
            mecanicos = mecanicos.take(1).map(Usuario::id)
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
        guardarCliente(cliente)

        val rutNormalizado = normalizeRut(cliente.rut)
        val vehiculoNormalizado = vehiculo.copy(
            patente = vehiculo.patente.uppercase(),
            clienteRut = rutNormalizado
        )
        guardarVehiculo(vehiculoNormalizado)

        val ot = crearOtInternal(vehiculoNormalizado.patente, sintomas, mecanicosIds)

        val presupuesto = presupuestos.getValue(ot.id)
        presupuesto.items.clear()
        presupuesto.items += presupuestoItems
        presupuesto.aprobado = presupuestoAprobado
        if (presupuestoAprobado) {
            log(ot.id, "PRESUPUESTO_APROBADO")
        }
        return ot
    }

    override suspend fun buscarClientePorRut(rut: String): Cliente? {
        val rutNormalizado = normalizeRut(rut)
        return clientes.firstOrNull { normalizeRut(it.rut) == rutNormalizado }
    }

    override suspend fun guardarCliente(cliente: Cliente) {
        val rutNormalizado = normalizeRut(cliente.rut)
        clientes.removeAll { normalizeRut(it.rut) == rutNormalizado }
        clientes += cliente.copy(rut = rutNormalizado)
    }

    override suspend fun obtenerVehiculosPorRut(rut: String): List<Vehiculo> {
        val rutNormalizado = normalizeRut(rut)
        return vehiculos.filter { normalizeRut(it.clienteRut) == rutNormalizado }
    }

    override suspend fun guardarVehiculo(vehiculo: Vehiculo) {
        val patenteUpper = vehiculo.patente.uppercase()
        vehiculos.removeAll { it.patente.equals(patenteUpper, ignoreCase = true) }
        vehiculos += vehiculo.copy(patente = patenteUpper, clienteRut = normalizeRut(vehiculo.clienteRut))
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
