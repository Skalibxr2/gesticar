package com.hans.gesticar.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.Usuario
import java.util.UUID

private const val DB_NAME = "gesticar.db"
private const val DB_VERSION = 1

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
                    id TEXT PRIMARY KEY,
                    nombre TEXT NOT NULL,
                    telefono TEXT,
                    email TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE vehiculos (
                    id TEXT PRIMARY KEY,
                    cliente_id TEXT NOT NULL,
                    patente TEXT NOT NULL,
                    marca TEXT NOT NULL,
                    modelo TEXT NOT NULL,
                    anio INTEGER NOT NULL,
                    color TEXT,
                    kilometraje INTEGER,
                    combustible TEXT,
                    observaciones TEXT,
                    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE ots (
                    id TEXT PRIMARY KEY,
                    numero INTEGER NOT NULL,
                    vehiculo_id TEXT NOT NULL,
                    estado TEXT NOT NULL,
                    mecanico_asignado_id TEXT,
                    notas TEXT,
                    FOREIGN KEY (vehiculo_id) REFERENCES vehiculos(id)
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

                val clienteId = UUID.randomUUID().toString()
                db.insertOrThrow(
                    "clientes",
                    null,
                    ContentValues().apply {
                        put("id", clienteId)
                        put("nombre", "Juan Pérez")
                        put("email", "juan@example.com")
                    }
                )

                val vehiculoId = UUID.randomUUID().toString()
                db.insertOrThrow(
                    "vehiculos",
                    null,
                    ContentValues().apply {
                        put("id", vehiculoId)
                        put("cliente_id", clienteId)
                        put("patente", "ABCD12")
                        put("marca", "Toyota")
                        put("modelo", "Yaris")
                        put("anio", 2018)
                        put("color", "Rojo")
                        put("kilometraje", 75000)
                        put("combustible", "Gasolina")
                    }
                )

                val otId = UUID.randomUUID().toString()
                db.insertOrThrow(
                    "ots",
                    null,
                    ContentValues().apply {
                        put("id", otId)
                        put("numero", 1001)
                        put("vehiculo_id", vehiculoId)
                        put("estado", OtState.PEND_APROB.name)
                        put("notas", "Ruidos en tren delantero")
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

    override suspend fun obtenerOts(): List<Ot> = helper.readableDatabase.useList(
        "SELECT id, numero, vehiculo_id, estado, mecanico_asignado_id, notas FROM ots ORDER BY numero"
    ) { cursor ->
        cursorToOt(cursor)
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
        val cursor = helper.readableDatabase.rawQuery(
            "SELECT id, numero, vehiculo_id, estado, mecanico_asignado_id, notas FROM ots WHERE numero = ?",
            arrayOf(numero.toString())
        )
        cursor.use {
            return if (it.moveToFirst()) cursorToOt(it) else null
        }
    }

    override suspend fun buscarOtPorPatente(patente: String): List<Ot> = helper.readableDatabase.useList(
        """
        SELECT o.id, o.numero, o.vehiculo_id, o.estado, o.mecanico_asignado_id, o.notas
        FROM ots o
        INNER JOIN vehiculos v ON v.id = o.vehiculo_id
        WHERE UPPER(v.patente) = UPPER(?)
        ORDER BY o.numero
        """.trimIndent(),
        arrayOf(patente)
    ) { cursor ->
        cursorToOt(cursor)
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

    private fun cursorToOt(cursor: Cursor): Ot = Ot(
        id = cursor.getString(0),
        numero = cursor.getInt(1),
        vehiculoId = cursor.getString(2),
        estado = OtState.valueOf(cursor.getString(3)),
        mecanicoAsignadoId = cursor.getString(4),
        notas = cursor.getString(5)
    )

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

private inline fun <T> SQLiteDatabase.useList(
    query: String,
    args: Array<String>? = null,
    mapper: (Cursor) -> T
): List<T> {
    val cursor = rawQuery(query, args)
    cursor.use {
        val list = ArrayList<T>(cursor.count.coerceAtLeast(0))
        while (cursor.moveToNext()) {
            list += mapper(cursor)
        }
        return list
    }
}
