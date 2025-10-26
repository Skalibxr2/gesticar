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
    val isLoggedIn: Boolean = false,
    val usuarioActual: Usuario? = null,
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
    fun login(email: String, password: String) {
        val user = repo.validarCredenciales(email, password)

        _ui.update {
            it.copy(
                isLoggedIn = user != null,
                usuarioActual = user,
                mensaje = if (user == null) "Credenciales inválidas" else null
            )
        }
    }


    fun logout() {
        _ui.update { it.copy(isLoggedIn = false, usuarioActual = null, mensaje = null) }
    }

    fun crearUsuario(nombre: String, email: String, password: String, rol: Rol): Boolean {
        val actual = _ui.value.usuarioActual
        if (actual?.rol != Rol.ADMIN) {
            _ui.update { it.copy(mensaje = "Solo un administrador puede crear usuarios") }
            return false
        }

        val resultado = runCatching { repo.crearUsuario(nombre, email, password, rol) }
        _ui.update {
            it.copy(
                mensaje = resultado.fold(
                    onSuccess = { "Usuario creado correctamente" },
                    onFailure = { it.message ?: "Error al crear usuario" }
                )
            )
        }
        return resultado.isSuccess
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