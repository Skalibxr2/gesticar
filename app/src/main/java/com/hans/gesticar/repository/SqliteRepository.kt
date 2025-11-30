package com.hans.gesticar.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtDetalle
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.Presupuesto
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.TareaEstado
import com.hans.gesticar.model.TareaOt
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import com.hans.gesticar.util.normalizeRut
import com.hans.gesticar.model.SintomaInput
import com.hans.gesticar.model.SintomaOt
import java.util.UUID

private const val DB_NAME = "gesticar.db"
private const val DB_VERSION = 5

class SqliteRepository(context: Context) : Repository {
    private val helper = object : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE usuarios (
                    id TEXT PRIMARY KEY,
                    nombre TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    rol TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE clientes (
                    rut TEXT PRIMARY KEY,
                    nombre TEXT NOT NULL,
                    correo TEXT,
                    comuna TEXT,
                    direccion TEXT,
                    telefono TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE vehiculos (
                    patente TEXT PRIMARY KEY,
                    cliente_rut TEXT NOT NULL,
                    marca TEXT NOT NULL,
                    modelo TEXT NOT NULL,
                    anio INTEGER NOT NULL,
                    color TEXT,
                    kilometraje INTEGER,
                    combustible TEXT,
                    FOREIGN KEY (cliente_rut) REFERENCES clientes(rut)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE ots (
                    id TEXT PRIMARY KEY,
                    numero INTEGER NOT NULL,
                    vehiculo_patente TEXT NOT NULL,
                    estado TEXT NOT NULL,
                    notas TEXT,
                    creada_en INTEGER NOT NULL,
                    FOREIGN KEY (vehiculo_patente) REFERENCES vehiculos(patente)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE ot_sintomas (
                    id TEXT PRIMARY KEY,
                    ot_id TEXT NOT NULL,
                    descripcion TEXT NOT NULL,
                    registrado_en INTEGER,
                    FOREIGN KEY (ot_id) REFERENCES ots(id)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE ot_sintoma_fotos (
                    id TEXT PRIMARY KEY,
                    ot_id TEXT NOT NULL,
                    sintoma_id TEXT NOT NULL,
                    uri TEXT NOT NULL,
                    FOREIGN KEY (ot_id) REFERENCES ots(id),
                    FOREIGN KEY (sintoma_id) REFERENCES ot_sintomas(id)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE presupuestos (
                    ot_id TEXT PRIMARY KEY,
                    aprobado INTEGER NOT NULL DEFAULT 0,
                    iva INTEGER NOT NULL DEFAULT 19,
                    FOREIGN KEY (ot_id) REFERENCES ots(id)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE ot_mecanicos (
                    ot_id TEXT NOT NULL,
                    mecanico_id TEXT NOT NULL,
                    PRIMARY KEY (ot_id, mecanico_id),
                    FOREIGN KEY (ot_id) REFERENCES ots(id),
                    FOREIGN KEY (mecanico_id) REFERENCES usuarios(id)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE presupuesto_items (
                    id TEXT PRIMARY KEY,
                    ot_id TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    descripcion TEXT NOT NULL,
                    cantidad INTEGER NOT NULL,
                    precio_unit INTEGER NOT NULL,
                    FOREIGN KEY (ot_id) REFERENCES ots(id)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE ot_tareas (
                    id TEXT PRIMARY KEY,
                    ot_id TEXT NOT NULL,
                    titulo TEXT NOT NULL,
                    descripcion TEXT,
                    fecha_creacion INTEGER NOT NULL,
                    fecha_inicio INTEGER,
                    fecha_termino INTEGER,
                    estado TEXT NOT NULL,
                    FOREIGN KEY (ot_id) REFERENCES ots(id)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE audit_logs (
                    id TEXT PRIMARY KEY,
                    ot_id TEXT NOT NULL,
                    usuario_email TEXT NOT NULL,
                    accion TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    FOREIGN KEY (ot_id) REFERENCES ots(id)
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS ot_sintoma_fotos")
            db.execSQL("DROP TABLE IF EXISTS ot_sintomas")
            db.execSQL("DROP TABLE IF EXISTS ot_tareas")
            db.execSQL("DROP TABLE IF EXISTS ot_mecanicos")
            db.execSQL("DROP TABLE IF EXISTS audit_logs")
            db.execSQL("DROP TABLE IF EXISTS presupuesto_items")
            db.execSQL("DROP TABLE IF EXISTS presupuestos")
            db.execSQL("DROP TABLE IF EXISTS ots")
            db.execSQL("DROP TABLE IF EXISTS vehiculos")
            db.execSQL("DROP TABLE IF EXISTS clientes")
            db.execSQL("DROP TABLE IF EXISTS usuarios")
            onCreate(db)
        }
    }

    init {
        seedIfNeeded()
    }

    private fun insertarSintomas(db: SQLiteDatabase, otId: String, sintomas: List<SintomaInput>) {
        db.delete("ot_sintomas", "ot_id = ?", arrayOf(otId))
        db.delete("ot_sintoma_fotos", "ot_id = ?", arrayOf(otId))
        sintomas.forEach { sintoma ->
            val sintomaId = UUID.randomUUID().toString()
            db.insert(
                "ot_sintomas",
                null,
                ContentValues().apply {
                    put("id", sintomaId)
                    put("ot_id", otId)
                    put("descripcion", sintoma.descripcion)
                    put("registrado_en", sintoma.registradoEn)
                }
            )
            sintoma.fotos.forEach { uri ->
                db.insert(
                    "ot_sintoma_fotos",
                    null,
                    ContentValues().apply {
                        put("id", UUID.randomUUID().toString())
                        put("ot_id", otId)
                        put("sintoma_id", sintomaId)
                        put("uri", uri)
                    }
                )
            }
        }
    }

    private fun obtenerSintomasInterno(db: SQLiteDatabase, otId: String): List<SintomaOt> {
        val fotosPorSintoma = mutableMapOf<String, MutableList<String>>()
        db.rawQuery(
            "SELECT sintoma_id, uri FROM ot_sintoma_fotos WHERE ot_id = ?",
            arrayOf(otId)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val sintomaId = cursor.getString(0)
                val uri = cursor.getString(1)
                fotosPorSintoma.getOrPut(sintomaId) { mutableListOf() }.add(uri)
            }
        }

        val cursor = db.rawQuery(
            "SELECT id, descripcion, registrado_en FROM ot_sintomas WHERE ot_id = ?",
            arrayOf(otId)
        )
        cursor.use {
            val list = ArrayList<SintomaOt>(it.count.coerceAtLeast(0))
            while (it.moveToNext()) {
                val sintomaId = it.getString(0)
                list += SintomaOt(
                    id = sintomaId,
                    otId = otId,
                    descripcion = it.getString(1),
                    registradoEn = if (it.isNull(2)) null else it.getLong(2),
                    fotos = fotosPorSintoma[sintomaId]?.toList() ?: emptyList()
                )
            }
            return list
        }
    }

    private fun seedIfNeeded() {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM usuarios", null)
            val hasData = cursor.moveToFirst() && cursor.getLong(0) > 0
            cursor.close()
            if (!hasData) {
                val adminId = UUID.randomUUID().toString()
                db.insertOrThrow(
                    "usuarios",
                    null,
                    ContentValues().apply {
                        put("id", adminId)
                        put("nombre", "Admin")
                        put("email", "admin@gesticar.cl")
                        put("rol", Rol.ADMIN.name)
                    }
                )

                val mechanicOneId = UUID.randomUUID().toString()
                db.insertOrThrow(
                    "usuarios",
                    null,
                    ContentValues().apply {
                        put("id", mechanicOneId)
                        put("nombre", "Mecánico Juan")
                        put("email", "mecanico@gesticar.cl")
                        put("rol", Rol.MECANICO.name)
                    }
                )

                val mechanicTwoId = UUID.randomUUID().toString()
                db.insertOrThrow(
                    "usuarios",
                    null,
                    ContentValues().apply {
                        put("id", mechanicTwoId)
                        put("nombre", "Mecánica Ana")
                        put("email", "ana@gesticar.cl")
                        put("rol", Rol.MECANICO.name)
                    }
                )

                val clienteUnoRut = normalizeRut("12345678-9")
                db.insertOrThrow(
                    "clientes",
                    null,
                    ContentValues().apply {
                        put("rut", clienteUnoRut)
                        put("nombre", "Juan Pérez")
                        put("correo", "juan@example.com")
                        put("comuna", "Santiago")
                    }
                )

                val patenteUno = "ABCD12"
                db.insertOrThrow(
                    "vehiculos",
                    null,
                    ContentValues().apply {
                        put("patente", patenteUno)
                        put("cliente_rut", clienteUnoRut)
                        put("marca", "Toyota")
                        put("modelo", "Yaris")
                        put("anio", 2018)
                        put("color", "Rojo")
                        put("kilometraje", 75000)
                        put("combustible", "Gasolina")
                    }
                )

                val clienteDosRut = normalizeRut("20123456-K")
                db.insertOrThrow(
                    "clientes",
                    null,
                    ContentValues().apply {
                        put("rut", clienteDosRut)
                        put("nombre", "María González")
                        put("correo", "maria@example.com")
                        put("comuna", "Valparaíso")
                    }
                )

                val patenteDos = "EFGH34"
                db.insertOrThrow(
                    "vehiculos",
                    null,
                    ContentValues().apply {
                        put("patente", patenteDos)
                        put("cliente_rut", clienteDosRut)
                        put("marca", "Hyundai")
                        put("modelo", "Accent")
                        put("anio", 2020)
                        put("color", "Azul")
                        put("kilometraje", 40000)
                        put("combustible", "Gasolina")
                    }
                )

                val otId = UUID.randomUUID().toString()
                val createdAt = System.currentTimeMillis()
                db.insertOrThrow(
                    "ots",
                    null,
                    ContentValues().apply {
                        put("id", otId)
                        put("numero", 1000)
                        put("vehiculo_patente", patenteUno)
                        put("estado", OtState.BORRADOR.name)
                        put("notas", "Ruidos en tren delantero")
                        put("creada_en", createdAt)
                    }
                )

                val sintomaId = UUID.randomUUID().toString()
                db.insertOrThrow(
                    "ot_sintomas",
                    null,
                    ContentValues().apply {
                        put("id", sintomaId)
                        put("ot_id", otId)
                        put("descripcion", "Ruidos en tren delantero")
                        put("registrado_en", createdAt)
                    }
                )

                db.insertOrThrow(
                    "presupuestos",
                    null,
                    ContentValues().apply {
                        put("ot_id", otId)
                        put("aprobado", 0)
                        put("iva", 19)
                    }
                )

                db.insertOrThrow(
                    "ot_mecanicos",
                    null,
                    ContentValues().apply {
                        put("ot_id", otId)
                        put("mecanico_id", mechanicOneId)
                    }
                )

                insertPresupuestoItem(
                    db,
                    otId,
                    ItemTipo.MO,
                    "Diagnóstico",
                    1,
                    15000
                )

                insertPresupuestoItem(
                    db,
                    otId,
                    ItemTipo.REP,
                    "Bujías",
                    4,
                    8000
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertPresupuestoItem(
        db: SQLiteDatabase,
        otId: String,
        tipo: ItemTipo,
        descripcion: String,
        cantidad: Int,
        precioUnit: Int
    ) {
        db.insertOrThrow(
            "presupuesto_items",
            null,
            ContentValues().apply {
                put("id", UUID.randomUUID().toString())
                put("ot_id", otId)
                put("tipo", tipo.name)
                put("descripcion", descripcion)
                put("cantidad", cantidad)
                put("precio_unit", precioUnit)
            }
        )
    }

    override suspend fun obtenerOts(): List<Ot> {
        val db = helper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, numero, vehiculo_patente, estado, notas, creada_en FROM ots ORDER BY numero",
            null
        )
        return mapOts(db, cursor)
    }

    override suspend fun findUserByEmail(email: String): Usuario? {
        val cursor = helper.readableDatabase.rawQuery(
            "SELECT id, nombre, email, rol FROM usuarios WHERE LOWER(email) = LOWER(?)",
            arrayOf(email)
        )
        cursor.use {
            return if (it.moveToFirst()) {
                Usuario(
                    id = it.getString(0),
                    nombre = it.getString(1),
                    email = it.getString(2),
                    rol = Rol.valueOf(it.getString(3))
                )
            } else {
                null
            }
        }
    }

    override suspend fun buscarOtPorNumero(numero: Int): Ot? {
        val db = helper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, numero, vehiculo_patente, estado, notas, creada_en FROM ots WHERE numero = ?",
            arrayOf(numero.toString())
        )
        cursor.use {
            if (!it.moveToFirst()) return null
            val otId = it.getString(0)
            val mecanicos = obtenerMecanicosOt(db, otId)
            return cursorToOt(it, mecanicos)
        }
    }

    override suspend fun buscarOtPorPatente(patente: String): List<Ot> {
        val db = helper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT o.id, o.numero, o.vehiculo_patente, o.estado, o.notas, o.creada_en
            FROM ots o
            WHERE UPPER(o.vehiculo_patente) = UPPER(?)
            ORDER BY o.numero
            """.trimIndent(),
            arrayOf(patente)
        )
        return mapOts(db, cursor)
    }

    override suspend fun buscarOtPorRut(rut: String): List<Ot> {
        val db = helper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT o.id, o.numero, o.vehiculo_patente, o.estado, o.notas, o.creada_en
            FROM ots o
            JOIN vehiculos v ON o.vehiculo_patente = v.patente
            WHERE v.cliente_rut = ?
            ORDER BY o.numero
            """.trimIndent(),
            arrayOf(normalizeRut(rut))
        )
        return mapOts(db, cursor)
    }

    override suspend fun buscarOtPorEstado(estado: OtState): List<Ot> {
        val db = helper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT id, numero, vehiculo_patente, estado, notas, creada_en
            FROM ots
            WHERE estado = ?
            ORDER BY numero
            """.trimIndent(),
            arrayOf(estado.name)
        )
        return mapOts(db, cursor)
    }

    override suspend fun obtenerMecanicos(): List<Usuario> {
        val db = helper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, nombre, email FROM usuarios WHERE rol = ?",
            arrayOf(Rol.MECANICO.name)
        )
        cursor.use {
            val list = ArrayList<Usuario>(it.count.coerceAtLeast(0))
            while (it.moveToNext()) {
                list += Usuario(
                    id = it.getString(0),
                    nombre = it.getString(1),
                    email = it.getString(2),
                    rol = Rol.MECANICO
                )
            }
            return list
        }
    }

    override suspend fun obtenerSiguienteNumeroOt(): Int {
        val db = helper.readableDatabase
        val cursor = db.rawQuery("SELECT COALESCE(MAX(numero), 999) + 1 FROM ots", null)
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 1000
        }
    }

    override suspend fun obtenerDetalleOt(otId: String): OtDetalle? {
        val db = helper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT id, numero, vehiculo_patente, estado, notas, creada_en
            FROM ots
            WHERE id = ?
            """.trimIndent(),
            arrayOf(otId)
        )
        cursor.use {
            if (!it.moveToFirst()) return null
            val mecanicosIds = obtenerMecanicosOt(db, otId)
            val ot = cursorToOt(it, mecanicosIds)
            val vehiculo = obtenerVehiculo(db, ot.vehiculoPatente)
            val cliente = vehiculo?.let { v -> obtenerCliente(db, v.clienteRut) }
            val mecanicos = if (mecanicosIds.isEmpty()) {
                emptyList()
            } else {
                obtenerUsuariosPorIds(db, mecanicosIds)
            }
            val presupuesto = obtenerPresupuesto(db, otId)
            val tareas = obtenerTareas(db, otId)
            val sintomas = obtenerSintomasInterno(db, otId)
            return OtDetalle(
                ot = ot,
                cliente = cliente,
                vehiculo = vehiculo,
                mecanicosAsignados = mecanicos,
                presupuesto = presupuesto,
                tareas = tareas,
                sintomas = sintomas
            )
        }
    }

    override suspend fun buscarClientePorRut(rut: String): Cliente? {
        val rutNormalizado = normalizeRut(rut)
        val cursor = helper.readableDatabase.rawQuery(
            "SELECT rut, nombre, correo, comuna, direccion, telefono FROM clientes WHERE rut = ?",
            arrayOf(rutNormalizado)
        )
        cursor.use {
            return if (it.moveToFirst()) {
                Cliente(
                    rut = it.getString(0),
                    nombre = it.getString(1),
                    correo = it.getString(2),
                    comuna = it.getString(3),
                    direccion = it.getString(4),
                    telefono = it.getString(5)
                )
            } else {
                null
            }
        }
    }

    override suspend fun guardarCliente(cliente: Cliente) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            upsertCliente(db, cliente)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun obtenerVehiculosPorRut(rut: String): List<Vehiculo> {
        val rutNormalizado = normalizeRut(rut)
        val cursor = helper.readableDatabase.rawQuery(
            """
            SELECT patente, cliente_rut, marca, modelo, anio, color, kilometraje, combustible
            FROM vehiculos
            WHERE cliente_rut = ?
            ORDER BY patente
            """.trimIndent(),
            arrayOf(rutNormalizado)
        )
        cursor.use {
            val list = ArrayList<Vehiculo>(it.count.coerceAtLeast(0))
            while (it.moveToNext()) {
                list += Vehiculo(
                    patente = it.getString(0),
                    clienteRut = it.getString(1),
                    marca = it.getString(2),
                    modelo = it.getString(3),
                    anio = it.getInt(4),
                    color = it.getString(5),
                    kilometraje = if (it.isNull(6)) null else it.getInt(6),
                    combustible = it.getString(7)
                )
            }
            return list
        }
    }

    override suspend fun buscarVehiculoPorPatente(patente: String): Vehiculo? {
        val db = helper.readableDatabase
        return obtenerVehiculo(db, patente.uppercase())
    }

    override suspend fun guardarVehiculo(vehiculo: Vehiculo) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            upsertVehiculo(db, vehiculo)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun desasociarVehiculo(patente: String) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val vehiculo = obtenerVehiculo(db, patente.uppercase()) ?: return
            upsertVehiculo(db, vehiculo.copy(clienteRut = ""))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun actualizarClienteVehiculo(patente: String, clienteRut: String): Vehiculo? {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val vehiculo = obtenerVehiculo(db, patente.uppercase()) ?: return null
            val actualizado = vehiculo.copy(clienteRut = normalizeRut(clienteRut))
            upsertVehiculo(db, actualizado)
            db.setTransactionSuccessful()
            return actualizado
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun actualizarNotasOt(otId: String, notas: String?) {
        val db = helper.writableDatabase
        val values = ContentValues().apply { put("notas", notas) }
        db.update("ots", values, "id = ?", arrayOf(otId))
        insertAudit(db, otId, "ACTUALIZAR_NOTAS")
    }

    override suspend fun actualizarMecanicosOt(otId: String, mecanicosIds: List<String>) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("ot_mecanicos", "ot_id = ?", arrayOf(otId))
            mecanicosIds.forEach { mecanicoId ->
                db.insert(
                    "ot_mecanicos",
                    null,
                    ContentValues().apply {
                        put("ot_id", otId)
                        put("mecanico_id", mecanicoId)
                    }
                )
            }
            insertAudit(db, otId, "ACTUALIZAR_MECANICOS")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun actualizarVehiculoOt(otId: String, patente: String): Boolean {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery(
                "SELECT vehiculo_patente, estado FROM ots WHERE id = ?",
                arrayOf(otId)
            )
            cursor.use {
                if (!it.moveToFirst()) return false
                val estadoActual = OtState.valueOf(it.getString(1))
                if (estadoActual == OtState.EN_EJECUCION || estadoActual == OtState.FINALIZADA || estadoActual == OtState.CANCELADA) {
                    return false
                }
            }
            val patenteUpper = patente.uppercase()
            val vehiculoExiste = db.rawQuery(
                "SELECT 1 FROM vehiculos WHERE UPPER(patente) = UPPER(?)",
                arrayOf(patenteUpper)
            ).use { it.moveToFirst() }
            if (!vehiculoExiste) {
                return false
            }
            val values = ContentValues().apply { put("vehiculo_patente", patenteUpper) }
            db.update("ots", values, "id = ?", arrayOf(otId))
            insertAudit(db, otId, "ACTUALIZAR_VEHICULO")
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun guardarPresupuesto(
        otId: String,
        items: List<PresupuestoItem>,
        aprobado: Boolean,
        ivaPorc: Int
    ) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("ot_id", otId)
                put("aprobado", if (aprobado) 1 else 0)
                put("iva", ivaPorc)
            }
            val updated = db.update("presupuestos", values, "ot_id = ?", arrayOf(otId))
            if (updated == 0) {
                db.insert("presupuestos", null, values)
            }
            db.delete("presupuesto_items", "ot_id = ?", arrayOf(otId))
            items.forEach { item ->
                db.insert(
                    "presupuesto_items",
                    null,
                    ContentValues().apply {
                        put("id", item.id)
                        put("ot_id", otId)
                        put("tipo", item.tipo.name)
                        put("descripcion", item.descripcion)
                        put("cantidad", item.cantidad)
                        put("precio_unit", item.precioUnit)
                    }
                )
            }
            insertAudit(db, otId, "ACTUALIZAR_PRESUPUESTO")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun guardarTareas(otId: String, tareas: List<TareaOt>) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("ot_tareas", "ot_id = ?", arrayOf(otId))
            tareas.forEach { tarea ->
                db.insert(
                    "ot_tareas",
                    null,
                    ContentValues().apply {
                        put("id", tarea.id)
                        put("ot_id", otId)
                        put("titulo", tarea.titulo)
                        put("descripcion", tarea.descripcion)
                        put("fecha_creacion", tarea.fechaCreacion)
                        put("fecha_inicio", tarea.fechaInicio)
                        put("fecha_termino", tarea.fechaTermino)
                        put("estado", tarea.estado.name)
                    }
                )
            }
            insertAudit(db, otId, "ACTUALIZAR_TAREAS")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun obtenerSintomas(otId: String): List<SintomaOt> {
        val db = helper.readableDatabase
        return obtenerSintomasInterno(db, otId)
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
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val rut = normalizeRut(cliente.rut)
            val patente = vehiculo.patente.uppercase()

            upsertCliente(db, cliente.copy(rut = rut))
            upsertVehiculo(
                db,
                vehiculo.copy(
                    patente = patente,
                    clienteRut = rut
                )
            )

            val numero = calcularSiguienteNumero(db)
            val otId = UUID.randomUUID().toString()
            val createdAt = System.currentTimeMillis()

            db.insertOrThrow(
                "ots",
                null,
                ContentValues().apply {
                    put("id", otId)
                    put("numero", numero)
                    put("vehiculo_patente", patente)
                    put("estado", OtState.BORRADOR.name)
                    put("notas", null as String?)
                    put("creada_en", createdAt)
                }
            )

            db.insertWithOnConflict(
                "presupuestos",
                null,
                ContentValues().apply {
                    put("ot_id", otId)
                    put("aprobado", if (presupuestoAprobado) 1 else 0)
                    put("iva", 19)
                },
                SQLiteDatabase.CONFLICT_REPLACE
            )

            db.delete("presupuesto_items", "ot_id = ?", arrayOf(otId))
            presupuestoItems.forEach { item ->
                db.insertOrThrow(
                    "presupuesto_items",
                    null,
                    ContentValues().apply {
                        put("id", item.id)
                        put("ot_id", otId)
                        put("tipo", item.tipo.name)
                        put("descripcion", item.descripcion)
                        put("cantidad", item.cantidad)
                        put("precio_unit", item.precioUnit)
                    }
                )
            }

            db.delete("ot_mecanicos", "ot_id = ?", arrayOf(otId))
            mecanicosIds.distinct().forEach { mecanicoId ->
                db.insert(
                    "ot_mecanicos",
                    null,
                    ContentValues().apply {
                        put("ot_id", otId)
                        put("mecanico_id", mecanicoId)
                    }
                )
            }

            db.delete("ot_tareas", "ot_id = ?", arrayOf(otId))
            tareas.forEach { tarea ->
                db.insert(
                    "ot_tareas",
                    null,
                    ContentValues().apply {
                        put("id", tarea.id)
                        put("ot_id", otId)
                        put("titulo", tarea.titulo)
                        put("descripcion", tarea.descripcion)
                        put("fecha_creacion", tarea.fechaCreacion)
                        put("fecha_inicio", tarea.fechaInicio)
                        put("fecha_termino", tarea.fechaTermino)
                        put("estado", tarea.estado.name)
                    }
                )
            }

            insertAudit(db, otId, "CREAR_OT")
            if (presupuestoAprobado) {
                insertAudit(db, otId, "PRESUPUESTO_APROBADO")
            }

            insertarSintomas(db, otId, sintomas)

            db.setTransactionSuccessful()
            return Ot(
                id = otId,
                numero = numero,
                vehiculoPatente = patente,
                estado = OtState.BORRADOR,
                mecanicosAsignados = mecanicosIds.distinct(),
                notas = null,
                fechaCreacion = createdAt
            )
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun aprobarPresupuesto(otId: String) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply { put("aprobado", 1) }
            db.update("presupuestos", values, "ot_id = ?", arrayOf(otId))
            insertAudit(db, otId, "APROBAR_PRESUPUESTO")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun cambiarEstado(otId: String, nuevo: OtState): Boolean {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val current = db.rawQuery(
                "SELECT estado FROM ots WHERE id = ?",
                arrayOf(otId)
            )
            val hasOt = current.moveToFirst()
            if (!hasOt) {
                current.close()
                return false
            }
            val currentState = OtState.valueOf(current.getString(0))
            current.close()

            if (currentState == OtState.FINALIZADA || currentState == OtState.CANCELADA) {
                return false
            }

            if (nuevo == OtState.EN_EJECUCION && !isPresupuestoAprobado(db, otId)) {
                return false
            }
            if (nuevo == OtState.EN_EJECUCION && !hasMecanicosAsignados(db, otId)) {
                return false
            }
            if (nuevo == OtState.FINALIZADA && currentState != OtState.EN_EJECUCION) {
                return false
            }

            val values = ContentValues().apply { put("estado", nuevo.name) }
            db.update("ots", values, "id = ?", arrayOf(otId))
            insertAudit(db, otId, "CAMBIAR_ESTADO:${nuevo.name}")
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun eliminarOt(otId: String): Boolean {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val current = db.rawQuery(
                "SELECT estado FROM ots WHERE id = ?",
                arrayOf(otId)
            )
            if (!current.moveToFirst()) {
                current.close()
                return false
            }
            val estadoActual = OtState.valueOf(current.getString(0))
            current.close()
            if (estadoActual != OtState.BORRADOR) {
                return false
            }

            db.delete("presupuesto_items", "ot_id = ?", arrayOf(otId))
            db.delete("ot_mecanicos", "ot_id = ?", arrayOf(otId))
            db.delete("ot_tareas", "ot_id = ?", arrayOf(otId))
            db.delete("ot_sintoma_fotos", "ot_id = ?", arrayOf(otId))
            db.delete("ot_sintomas", "ot_id = ?", arrayOf(otId))
            db.delete("ot_logs", "ot_id = ?", arrayOf(otId))
            val removed = db.delete("ots", "id = ?", arrayOf(otId))
            if (removed == 0) {
                return false
            }
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    private fun isPresupuestoAprobado(db: SQLiteDatabase, otId: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT aprobado FROM presupuestos WHERE ot_id = ?",
            arrayOf(otId)
        )
        cursor.use {
            return it.moveToFirst() && it.getInt(0) == 1
        }
    }

    private fun hasMecanicosAsignados(db: SQLiteDatabase, otId: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ot_mecanicos WHERE ot_id = ?",
            arrayOf(otId)
        )
        cursor.use {
            return it.moveToFirst() && it.getInt(0) > 0
        }
    }

    private fun cursorToOt(cursor: Cursor, mecanicos: List<String>): Ot = Ot(
        id = cursor.getString(0),
        numero = cursor.getInt(1),
        vehiculoPatente = cursor.getString(2),
        estado = OtState.valueOf(cursor.getString(3)),
        mecanicosAsignados = mecanicos,
        notas = cursor.getString(4),
        fechaCreacion = cursor.getLong(5)
    )

    private fun obtenerVehiculo(db: SQLiteDatabase, patente: String): Vehiculo? {
        val cursor = db.rawQuery(
            """
            SELECT patente, cliente_rut, marca, modelo, anio, color, kilometraje, combustible
            FROM vehiculos
            WHERE UPPER(patente) = UPPER(?)
            """.trimIndent(),
            arrayOf(patente)
        )
        cursor.use {
            return if (it.moveToFirst()) {
                Vehiculo(
                    patente = it.getString(0),
                    clienteRut = it.getString(1),
                    marca = it.getString(2),
                    modelo = it.getString(3),
                    anio = it.getInt(4),
                    color = it.getString(5),
                    kilometraje = if (it.isNull(6)) null else it.getInt(6),
                    combustible = it.getString(7)
                )
            } else {
                null
            }
        }
    }

    private fun obtenerCliente(db: SQLiteDatabase, rut: String): Cliente? {
        val cursor = db.rawQuery(
            "SELECT rut, nombre, correo, comuna, direccion, telefono FROM clientes WHERE rut = ?",
            arrayOf(rut)
        )
        cursor.use {
            return if (it.moveToFirst()) {
                Cliente(
                    rut = it.getString(0),
                    nombre = it.getString(1),
                    correo = it.getString(2),
                    comuna = it.getString(3),
                    direccion = it.getString(4),
                    telefono = it.getString(5)
                )
            } else {
                null
            }
        }
    }

    private fun obtenerUsuariosPorIds(db: SQLiteDatabase, ids: List<String>): List<Usuario> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val cursor = db.rawQuery(
            "SELECT id, nombre, email, rol FROM usuarios WHERE id IN ($placeholders)",
            ids.toTypedArray()
        )
        cursor.use {
            val list = ArrayList<Usuario>(it.count.coerceAtLeast(0))
            while (it.moveToNext()) {
                list += Usuario(
                    id = it.getString(0),
                    nombre = it.getString(1),
                    email = it.getString(2),
                    rol = Rol.valueOf(it.getString(3))
                )
            }
            return list
        }
    }

    private fun obtenerPresupuesto(db: SQLiteDatabase, otId: String): Presupuesto {
        val cursor = db.rawQuery(
            "SELECT aprobado, iva FROM presupuestos WHERE ot_id = ?",
            arrayOf(otId)
        )
        val items = mutableListOf<PresupuestoItem>()
        var aprobado = false
        var iva = 19
        cursor.use {
            if (it.moveToFirst()) {
                aprobado = it.getInt(0) == 1
                iva = it.getInt(1)
            }
        }
        val itemsCursor = db.rawQuery(
            """
            SELECT id, tipo, descripcion, cantidad, precio_unit
            FROM presupuesto_items
            WHERE ot_id = ?
            ORDER BY descripcion
            """.trimIndent(),
            arrayOf(otId)
        )
        itemsCursor.use {
            while (it.moveToNext()) {
                items += PresupuestoItem(
                    id = it.getString(0),
                    tipo = ItemTipo.valueOf(it.getString(1)),
                    descripcion = it.getString(2),
                    cantidad = it.getInt(3),
                    precioUnit = it.getInt(4)
                )
            }
        }
        return Presupuesto(
            otId = otId,
            items = items,
            aprobado = aprobado,
            ivaPorc = iva
        )
    }

    private fun obtenerTareas(db: SQLiteDatabase, otId: String): List<TareaOt> {
        val cursor = db.rawQuery(
            """
            SELECT id, titulo, descripcion, fecha_creacion, fecha_inicio, fecha_termino, estado
            FROM ot_tareas
            WHERE ot_id = ?
            ORDER BY titulo
            """.trimIndent(),
            arrayOf(otId)
        )
        cursor.use {
            val list = ArrayList<TareaOt>(it.count.coerceAtLeast(0))
            while (it.moveToNext()) {
                val fechaInicio = if (it.isNull(4)) null else it.getLong(4)
                val fechaTermino = if (it.isNull(5)) null else it.getLong(5)
                list += TareaOt(
                    id = it.getString(0),
                    titulo = it.getString(1),
                    descripcion = it.getString(2),
                    fechaCreacion = it.getLong(3),
                    fechaInicio = fechaInicio,
                    fechaTermino = fechaTermino,
                    estado = TareaEstado.valueOf(it.getString(6))
                )
            }
            return list
        }
    }

    private fun mapOts(db: SQLiteDatabase, cursor: Cursor): List<Ot> {
        cursor.use {
            val list = ArrayList<Ot>(it.count.coerceAtLeast(0))
            while (it.moveToNext()) {
                val otId = it.getString(0)
                val mecanicos = obtenerMecanicosOt(db, otId)
                list += cursorToOt(it, mecanicos)
            }
            return list
        }
    }

    private fun obtenerMecanicosOt(db: SQLiteDatabase, otId: String): List<String> {
        val cursor = db.rawQuery(
            "SELECT mecanico_id FROM ot_mecanicos WHERE ot_id = ?",
            arrayOf(otId)
        )
        cursor.use {
            val list = ArrayList<String>(it.count.coerceAtLeast(0))
            while (it.moveToNext()) {
                list += it.getString(0)
            }
            return list
        }
    }

    private fun calcularSiguienteNumero(db: SQLiteDatabase): Int {
        val cursor = db.rawQuery("SELECT COALESCE(MAX(numero), 999) + 1 FROM ots", null)
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 1000
        }
    }

    private fun upsertCliente(db: SQLiteDatabase, cliente: Cliente) {
        val rutNormalizado = normalizeRut(cliente.rut)
        db.insertWithOnConflict(
            "clientes",
            null,
            ContentValues().apply {
                put("rut", rutNormalizado)
                put("nombre", cliente.nombre)
                put("correo", cliente.correo)
                put("comuna", cliente.comuna)
                put("direccion", cliente.direccion)
                put("telefono", cliente.telefono)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun upsertVehiculo(db: SQLiteDatabase, vehiculo: Vehiculo) {
        val patenteUpper = vehiculo.patente.uppercase()
        val rutNormalizado = normalizeRut(vehiculo.clienteRut)
        db.insertWithOnConflict(
            "vehiculos",
            null,
            ContentValues().apply {
                put("patente", patenteUpper)
                put("cliente_rut", rutNormalizado)
                put("marca", vehiculo.marca)
                put("modelo", vehiculo.modelo)
                put("anio", vehiculo.anio)
                put("color", vehiculo.color)
                put("kilometraje", vehiculo.kilometraje)
                put("combustible", vehiculo.combustible)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun insertAudit(db: SQLiteDatabase, otId: String, accion: String) {
        db.insert(
            "audit_logs",
            null,
            ContentValues().apply {
                put("id", UUID.randomUUID().toString())
                put("ot_id", otId)
                put("usuario_email", "admin@gesticar.cl")
                put("accion", accion)
                put("timestamp", System.currentTimeMillis())
            }
        )
    }
}
