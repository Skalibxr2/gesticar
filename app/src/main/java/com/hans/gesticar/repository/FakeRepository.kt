package com.hans.gesticar.repository

import com.hans.gesticar.model.AuditLog
import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.Evidencia
import com.hans.gesticar.model.EvidenciaEtapa
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.OtDetalle
import com.hans.gesticar.model.Presupuesto
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.SintomaInput
import com.hans.gesticar.model.SintomaOt
import com.hans.gesticar.model.TareaEstado
import com.hans.gesticar.model.TareaOt
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
    val tareas = mutableMapOf<String, MutableList<TareaOt>>()
    val sintomasPorOt = mutableMapOf<String, MutableList<SintomaOt>>()

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
        tareas[ot.id] = mutableListOf(
            TareaOt(
                titulo = "Revisión inicial",
                descripcion = "Verificar tren delantero",
                estado = TareaEstado.TERMINADA,
                fechaInicio = System.currentTimeMillis(),
                fechaTermino = System.currentTimeMillis()
            ),
            TareaOt(titulo = "Cambio de bujías", estado = TareaEstado.CREADA)
        )
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
        mecanicos: List<String>,
        tareasIniciales: List<TareaOt> = emptyList()
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
        tareas[ot.id] = tareasIniciales.map { it.copy() }.toMutableList()
        log(ot.id, "CREAR_OT")
        return ot
    }

    override suspend fun crearOt(
        cliente: Cliente,
        vehiculo: Vehiculo,
        mecanicosIds: List<String>,
        presupuestoItems: List<PresupuestoItem>,
        presupuestoAprobado: Boolean,
        sintomas: List<SintomaInput>,
        tareas: List<TareaOt>
    ): Ot {
        guardarCliente(cliente)

        val rutNormalizado = normalizeRut(cliente.rut)
        val vehiculoNormalizado = vehiculo.copy(
            patente = vehiculo.patente.uppercase(),
            clienteRut = rutNormalizado
        )
        guardarVehiculo(vehiculoNormalizado)

        val notas = sintomas.joinToString("\n") { it.descripcion }
        val ot = crearOtInternal(vehiculoNormalizado.patente, notas, mecanicosIds, tareas)

        sintomasPorOt[ot.id] = sintomas.map { input ->
            SintomaOt(
                otId = ot.id,
                descripcion = input.descripcion,
                registradoEn = input.registradoEn,
                fotos = input.fotos
            )
        }.toMutableList()

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

    override suspend fun buscarVehiculoPorPatente(patente: String): Vehiculo? {
        val patenteUpper = patente.uppercase()
        return vehiculos.firstOrNull { it.patente.equals(patenteUpper, ignoreCase = true) }
    }

    override suspend fun guardarVehiculo(vehiculo: Vehiculo) {
        val patenteUpper = vehiculo.patente.uppercase()
        vehiculos.removeAll { it.patente.equals(patenteUpper, ignoreCase = true) }
        vehiculos += vehiculo.copy(patente = patenteUpper, clienteRut = normalizeRut(vehiculo.clienteRut))
    }

    override suspend fun desasociarVehiculo(patente: String) {
        val patenteUpper = patente.uppercase()
        val index = vehiculos.indexOfFirst { it.patente.equals(patenteUpper, ignoreCase = true) }
        if (index >= 0) {
            val vehiculo = vehiculos[index]
            vehiculos[index] = vehiculo.copy(clienteRut = "")
        }
    }

    override suspend fun actualizarClienteVehiculo(patente: String, clienteRut: String): Vehiculo? {
        val patenteUpper = patente.uppercase()
        val index = vehiculos.indexOfFirst { it.patente.equals(patenteUpper, ignoreCase = true) }
        if (index < 0) return null
        val actualizado = vehiculos[index].copy(clienteRut = normalizeRut(clienteRut))
        vehiculos[index] = actualizado
        return actualizado
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
        if (ot.estado == OtState.FINALIZADA || ot.estado == OtState.CANCELADA) return false
        if (nuevo == OtState.EN_EJECUCION && ot.mecanicosAsignados.isEmpty()) return false
        if (nuevo == OtState.EN_EJECUCION && !p.aprobado) return false
        // Regla: FINALIZADA solo si ya estaba en EN_EJECUCION
        if (nuevo == OtState.FINALIZADA && ot.estado != OtState.EN_EJECUCION) return false
        ot.estado = nuevo
        log(otId, "CAMBIAR_ESTADO:${nuevo}")
        return true
    }

    override suspend fun eliminarOt(otId: String): Boolean {
        val ot = ots.firstOrNull { it.id == otId } ?: return false
        if (ot.estado != OtState.BORRADOR) return false
        ots.removeIf { it.id == otId }
        presupuestos.remove(otId)
        tareas.remove(otId)
        sintomasPorOt.remove(otId)
        log(otId, "ELIMINAR_OT")
        return true
    }

    override suspend fun buscarOtPorNumero(numero: Int): Ot? = ots.find { it.numero == numero }

    override suspend fun buscarOtPorPatente(patente: String): List<Ot> {
        val pat = patente.uppercase()
        return ots.filter { it.vehiculoPatente == pat }
    }

    override suspend fun buscarOtPorRut(rut: String): List<Ot> {
        val rutNormalizado = normalizeRut(rut)
        val patentes = vehiculos.filter { normalizeRut(it.clienteRut) == rutNormalizado }.map { it.patente }
        return ots.filter { it.vehiculoPatente in patentes }
    }

    override suspend fun buscarOtPorEstado(estado: OtState): List<Ot> =
        ots.filter { it.estado == estado }

    override suspend fun obtenerDetalleOt(otId: String): OtDetalle? {
        val ot = ots.firstOrNull { it.id == otId } ?: return null
        val vehiculo = vehiculos.firstOrNull { it.patente.equals(ot.vehiculoPatente, ignoreCase = true) }
        val cliente = vehiculo?.let { v ->
            clientes.firstOrNull { normalizeRut(it.rut) == normalizeRut(v.clienteRut) }
        }
        val presupuestoOriginal = presupuestos[otId] ?: Presupuesto(otId = otId)
        val presupuesto = Presupuesto(
            otId = presupuestoOriginal.otId,
            items = presupuestoOriginal.items.map { it.copy() }.toMutableList(),
            aprobado = presupuestoOriginal.aprobado,
            ivaPorc = presupuestoOriginal.ivaPorc
        )
        val mecanicos = usuarios.filter { it.id in ot.mecanicosAsignados }
        val tareasGuardadas = tareas[otId]?.map { it.copy() } ?: emptyList()
        return OtDetalle(
            ot = ot,
            cliente = cliente,
            vehiculo = vehiculo,
            mecanicosAsignados = mecanicos,
            presupuesto = presupuesto,
            tareas = tareasGuardadas,
            sintomas = sintomasPorOt[otId]?.map { it.copy() } ?: emptyList()
        )
    }

    override suspend fun obtenerSintomas(otId: String): List<SintomaOt> {
        return sintomasPorOt[otId]?.map { it.copy() } ?: emptyList()
    }

    override suspend fun actualizarNotasOt(otId: String, notas: String?) {
        val index = ots.indexOfFirst { it.id == otId }
        if (index >= 0) {
            val ot = ots[index]
            ots[index] = ot.copy(notas = notas)
            log(otId, "ACTUALIZAR_NOTAS")
        }
    }

    override suspend fun actualizarMecanicosOt(otId: String, mecanicosIds: List<String>) {
        val index = ots.indexOfFirst { it.id == otId }
        if (index >= 0) {
            val ot = ots[index]
            ots[index] = ot.copy(mecanicosAsignados = mecanicosIds)
            log(otId, "ACTUALIZAR_MECANICOS")
        }
    }

    override suspend fun actualizarVehiculoOt(otId: String, patente: String): Boolean {
        val index = ots.indexOfFirst { it.id == otId }
        if (index < 0) return false
        val ot = ots[index]
        if (ot.estado == OtState.EN_EJECUCION || ot.estado == OtState.FINALIZADA || ot.estado == OtState.CANCELADA) {
            return false
        }
        val patenteUpper = patente.uppercase()
        val existeVehiculo = vehiculos.any { it.patente == patenteUpper }
        if (!existeVehiculo) return false
        ots[index] = ot.copy(vehiculoPatente = patenteUpper)
        log(otId, "ACTUALIZAR_VEHICULO")
        return true
    }

    override suspend fun guardarPresupuesto(
        otId: String,
        items: List<PresupuestoItem>,
        aprobado: Boolean,
        ivaPorc: Int
    ) {
        val presupuesto = presupuestos.getOrPut(otId) { Presupuesto(otId = otId) }
        presupuesto.items.clear()
        presupuesto.items += items
        presupuesto.aprobado = aprobado
        presupuesto.ivaPorc = ivaPorc
        log(otId, "ACTUALIZAR_PRESUPUESTO")
    }

    override suspend fun guardarTareas(otId: String, tareas: List<TareaOt>) {
        val copia = tareas.map { it.copy() }.toMutableList()
        this.tareas[otId] = copia
        log(otId, "ACTUALIZAR_TAREAS")
    }

    fun agregarEvidencia(otId: String, etapa: EvidenciaEtapa, uriLocal: String) {
        evidencias += Evidencia(otId = otId, etapa = etapa, uriLocal = uriLocal)
        log(otId, "AGREGAR_EVIDENCIA:${etapa}")
    }

    fun log(otId: String, accion: String, usuarioEmail: String = "admin@gesticar.cl") {
        audit += AuditLog(otId = otId, usuarioEmail = usuarioEmail, accion = accion)
    }
}
