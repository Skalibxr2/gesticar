package com.hans.gesticar.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import java.util.UUID

private const val DB_NAME = "gesticar.db"
private const val DB_VERSION = 3

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

                val mechanicId = UUID.randomUUID().toString()
                db.insertOrThrow(
                    "usuarios",
                    null,
                    ContentValues().apply {
                        put("id", mechanicId)
                        put("nombre", "Mecánico Juan")
                        put("email", "mecanico@gesticar.cl")
                        put("rol", Rol.MECANICO.name)
                    }
                )

                val clienteRut = "12.345.678-9"
                db.insertOrThrow(
                    "clientes",
                    null,
                    ContentValues().apply {
                        put("rut", clienteRut)
                        put("nombre", "Juan Pérez")
                        put("correo", "juan@example.com")
                    }
                )

                val patente = "ABCD12"
                db.insertOrThrow(
                    "vehiculos",
                    null,
                    ContentValues().apply {
                        put("patente", patente)
                        put("cliente_rut", clienteRut)
                        put("marca", "Toyota")
                        put("modelo", "Yaris")
                        put("anio", 2018)
                        put("color", "Rojo")
                        put("kilometraje", 75000)
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
                        put("vehiculo_patente", patente)
                        put("estado", OtState.BORRADOR.name)
                        put("notas", "Ruidos en tren delantero")
                        put("creada_en", createdAt)
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
                        put("mecanico_id", mechanicId)
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

    override suspend fun crearOt(
        cliente: Cliente,
        vehiculo: Vehiculo,
        mecanicosIds: List<String>,
        presupuestoItems: List<PresupuestoItem>,
        presupuestoAprobado: Boolean,
        sintomas: String?
    ): Ot {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val rut = cliente.rut.uppercase()
            val patente = vehiculo.patente.uppercase()

            db.insertWithOnConflict(
                "clientes",
                null,
                ContentValues().apply {
                    put("rut", rut)
                    put("nombre", cliente.nombre)
                    put("correo", cliente.correo)
                    put("direccion", cliente.direccion)
                    put("telefono", cliente.telefono)
                },
                SQLiteDatabase.CONFLICT_REPLACE
            )

            db.insertWithOnConflict(
                "vehiculos",
                null,
                ContentValues().apply {
                    put("patente", patente)
                    put("cliente_rut", rut)
                    put("marca", vehiculo.marca)
                    put("modelo", vehiculo.modelo)
                    put("anio", vehiculo.anio)
                    put("color", vehiculo.color)
                    put("kilometraje", vehiculo.kilometraje)
                    put("combustible", vehiculo.combustible)
                },
                SQLiteDatabase.CONFLICT_REPLACE
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
                    put("notas", sintomas)
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

            insertAudit(db, otId, "CREAR_OT")
            if (presupuestoAprobado) {
                insertAudit(db, otId, "PRESUPUESTO_APROBADO")
            }

            db.setTransactionSuccessful()
            return Ot(
                id = otId,
                numero = numero,
                vehiculoPatente = patente,
                estado = OtState.BORRADOR,
                mecanicosAsignados = mecanicosIds.distinct(),
                notas = sintomas,
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

            if (nuevo == OtState.EN_EJECUCION && !isPresupuestoAprobado(db, otId)) {
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

    private fun isPresupuestoAprobado(db: SQLiteDatabase, otId: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT aprobado FROM presupuestos WHERE ot_id = ?",
            arrayOf(otId)
        )
        cursor.use {
            return it.moveToFirst() && it.getInt(0) == 1
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
