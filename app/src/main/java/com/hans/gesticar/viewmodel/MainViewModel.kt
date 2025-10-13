package com.hans.gesticar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hans.gesticar.model.*
import com.hans.gesticar.repository.FakeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class UiState(
    val adminLoggedIn: Boolean = false,
    val usuarioAdmin: String? = null,
    val displayName: String? = null,
    val ots: List<Ot> = emptyList(),
    val seleccion: Ot? = null,
    val resultadosBusqueda: List<Ot> = emptyList(),
    val mensaje: String? = null
)

class MainViewModel(
    val repo: FakeRepository = FakeRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState(ots = repo.ots.toList()))
    val ui: StateFlow<UiState> = _ui


    // --- Login admin (mock) ---
    fun loginAdmin(email: String, password: String) {
        val user = repo.findUserByEmail(email)

        // Regla de MVP: debe existir y ser ADMIN; password mock = "admin"
        val ok = user?.rol == Rol.ADMIN && password == "admin"

        _ui.update {
            it.copy(
                adminLoggedIn = ok,
                usuarioAdmin = if (ok) user!!.email else null,
                displayName  = if (ok) user!!.nombre else null,
                mensaje = if (ok) null else "Credenciales inválidas"
            )
        }
    }


    fun logout() {
        _ui.update { it.copy(adminLoggedIn = false, usuarioAdmin = null) }
    }


    // --- Búsquedas ---
    fun buscarPorNumero(numero: Int) {
        val ot = repo.buscarOtPorNumero(numero)
        _ui.update { it.copy(resultadosBusqueda = listOfNotNull(ot), mensaje = if (ot == null) "Sin resultados" else null) }
    }

    fun buscarPorPatente(patente: String) {
        val lista = repo.buscarOtPorPatente(patente)
        _ui.update { it.copy(resultadosBusqueda = lista, mensaje = if (lista.isEmpty()) "Sin resultados" else null) }
    }


    // --- Acciones admin ---
    fun aprobarPresupuesto(otId: String) {
        viewModelScope.launch {
            repo.aprobarPresupuesto(otId)
            _ui.update { it.copy(mensaje = "Presupuesto aprobado") }
        }
    }

    fun cambiarEstado(otId: String, nuevo: OtState) {
        viewModelScope.launch {
            val ok = repo.cambiarEstado(otId, nuevo)
            _ui.update { it.copy(mensaje = if (ok) "Estado actualizado" else "Transición inválida") }
        }
    }


}