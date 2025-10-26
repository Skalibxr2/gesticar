package com.hans.gesticar.repository

import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.Usuario

interface Repository {
    suspend fun obtenerOts(): List<Ot>
    suspend fun findUserByEmail(email: String): Usuario?
    suspend fun buscarOtPorNumero(numero: Int): Ot?
    suspend fun buscarOtPorPatente(patente: String): List<Ot>
    suspend fun aprobarPresupuesto(otId: String)
    suspend fun cambiarEstado(otId: String, nuevo: OtState): Boolean
}
