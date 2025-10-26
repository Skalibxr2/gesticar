package com.hans.gesticar.repository

import com.hans.gesticar.model.*
import java.util.concurrent.atomic.AtomicInteger

class FakeRepository {
    private val otCounter = AtomicInteger(1000)

    val usuarios = mutableListOf(
        Usuario(nombre = "Admin", email = "admin@gesticar.cl", password = "admin", rol = Rol.ADMIN)
    )
    val clientes = mutableListOf<Cliente>()
    val vehiculos = mutableListOf<Vehiculo>()
    val ots = mutableListOf<Ot>()
    val presupuestos = mutableMapOf<String, Presupuesto>() // key: otId
    val evidencias = mutableListOf<Evidencia>()
    val audit = mutableListOf<AuditLog>()

    init {
        // Seed mínimo
        val c = Cliente(nombre = "Juan Pérez", email = "juan@example.com")
        clientes += c
        val v = Vehiculo(
            clienteId = c.id,
            patente = "ABCD12",
            marca = "Toyota",
            modelo = "Yaris",
            anio = 2018,
            color = "Rojo",
            kilometraje = 75000,
            combustible = "Gasolina"
        )
        vehiculos += v
        val ot = crearOT(vehiculoId = v.id, notas = "Ruidos en tren delantero")
        agregarItemPresupuesto(ot.id, ItemTipo.MO, "Diagnóstico", 1, 15000)
        agregarItemPresupuesto(ot.id, ItemTipo.REP, "Bujías", 4, 8000)
    }

    fun nextOtNumber(): Int = otCounter.incrementAndGet()

    fun findUserByEmail(email: String): Usuario? =
        usuarios.firstOrNull { it.email.equals(email, ignoreCase = true) }

    fun validarCredenciales(email: String, password: String): Usuario? =
        usuarios.firstOrNull { it.email.equals(email, ignoreCase = true) && it.password == password }

    fun crearUsuario(nombre: String, email: String, password: String, rol: Rol): Usuario {
        if (usuarios.any { it.email.equals(email, ignoreCase = true) }) {
            throw IllegalArgumentException("El correo ya está registrado")
        }
        val user = Usuario(nombre = nombre, email = email, password = password, rol = rol)
        usuarios += user
        return user
    }

    fun crearCliente(nombre: String, telefono: String?, email: String?): Cliente {
        val c = Cliente(nombre = nombre, telefono = telefono, email = email)
        clientes += c
        return c
    }

    fun crearVehiculo(
        clienteId: String, patente: String, marca: String, modelo: String, anio: Int,
        color: String?, km: Int?, combustible: String?, obs: String?
    ): Vehiculo {
        val v = Vehiculo(
            clienteId = clienteId,
            patente = patente.uppercase(),
            marca = marca,
            modelo = modelo,
            anio = anio,
            color = color,
            kilometraje = km,
            combustible = combustible,
            observaciones = obs
        )
        vehiculos += v
        return v
    }

    fun crearOT(vehiculoId: String, notas: String?): Ot {
        val ot = Ot(numero = nextOtNumber(), vehiculoId = vehiculoId, notas = notas)
        ots += ot
        presupuestos[ot.id] = Presupuesto(otId = ot.id)
        log(ot.id, "CREAR_OT")
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

    fun aprobarPresupuesto(otId: String) {
        val p = obtenerPresupuesto(otId)
        p.aprobado = true
        log(otId, "APROBAR_PRESUPUESTO")
    }

    fun cambiarEstado(otId: String, nuevo: OtState): Boolean {
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

    fun buscarOtPorNumero(numero: Int): Ot? = ots.find { it.numero == numero }


    fun buscarOtPorPatente(patente: String): List<Ot> {
        val pat = patente.uppercase()
        val vehIds = vehiculos.filter { it.patente == pat }.map { it.id }.toSet()
        return ots.filter { it.vehiculoId in vehIds }
    }

    fun agregarEvidencia(otId: String, etapa: EvidenciaEtapa, uriLocal: String) {
        evidencias += Evidencia(otId = otId, etapa = etapa, uriLocal = uriLocal)
        log(otId, "AGREGAR_EVIDENCIA:${etapa}")
    }

    fun log(otId: String, accion: String, usuarioEmail: String = "admin@gesticar.cl") {
        audit += AuditLog(otId = otId, usuarioEmail = usuarioEmail, accion = accion)
    }
}