package com.hans.gesticar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class UiState(
    val estaAutenticado: Boolean = false,
    val usuarioActual: Usuario? = null,
    val displayName: String? = null,
    val ots: List<Ot> = emptyList(),
    val seleccion: Ot? = null,
    val resultadosBusqueda: List<Ot> = emptyList(),
    val mensaje: String? = null
)

class MainViewModel(
    private val repo: Repository
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        refreshOts()
    }

    private fun refreshOts() {
        viewModelScope.launch(Dispatchers.IO) {
            val ots = repo.obtenerOts()
            _ui.update { it.copy(ots = ots) }
        }
    }

    // --- Login ---
    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repo.findUserByEmail(email)
            val ok = user != null && (user.password.isBlank() || user.password == password)

            _ui.update {
                it.copy(
                    estaAutenticado = ok,
                    usuarioActual = if (ok) user else null,
                    displayName = user?.nombre,
                    resultadosBusqueda = emptyList(),
                    mensaje = if (ok) null else "Credenciales inválidas"
                )
            }
        }
    }

    fun logout() {
        _ui.update {
            it.copy(
                estaAutenticado = false,
                usuarioActual = null,
                resultadosBusqueda = emptyList()
            )
        }
    }

    // --- Búsquedas ---
    fun buscarPorNumero(numero: Int) {
        val usuario = _ui.value.usuarioActual
        if (usuario == null) {
            _ui.update {
                it.copy(resultadosBusqueda = emptyList(), mensaje = "Debes iniciar sesión para buscar órdenes.")
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val ot = repo.buscarOtPorNumero(numero)
            val resultados = listOfNotNull(ot).filtrarPara(usuario)
            val mensaje = when {
                ot == null -> "Sin resultados"
                resultados.isEmpty() -> "Esta OT no está asignada a ti"
                else -> null
            }
            _ui.update { it.copy(resultadosBusqueda = resultados, mensaje = mensaje) }
        }
    }

    fun buscarPorPatente(patente: String) {
        val usuario = _ui.value.usuarioActual
        if (usuario == null) {
            _ui.update {
                it.copy(resultadosBusqueda = emptyList(), mensaje = "Debes iniciar sesión para buscar órdenes.")
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val lista = repo.buscarOtPorPatente(patente).filtrarPara(usuario)
            val mensaje = if (lista.isEmpty()) "Sin resultados" else null
            _ui.update { it.copy(resultadosBusqueda = lista, mensaje = mensaje) }
        }
    }

    // --- Acciones admin ---
    fun aprobarPresupuesto(otId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.aprobarPresupuesto(otId)
            val currentResults = _ui.value.resultadosBusqueda
            val updatedOt = currentResults.firstOrNull { it.id == otId }?.numero?.let { repo.buscarOtPorNumero(it) }
            val refreshedResults = if (updatedOt != null) {
                currentResults.map { if (it.id == otId) updatedOt else it }
            } else {
                currentResults
            }
            val ots = repo.obtenerOts()
            _ui.update {
                it.copy(
                    mensaje = "Presupuesto aprobado",
                    resultadosBusqueda = refreshedResults,
                    ots = ots
                )
            }
        }
    }

    fun cambiarEstado(otId: String, nuevo: OtState) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repo.cambiarEstado(otId, nuevo)
            val currentResults = _ui.value.resultadosBusqueda
            val refreshedOt = if (ok) {
                currentResults.firstOrNull { it.id == otId }?.numero?.let { repo.buscarOtPorNumero(it) }
            } else {
                null
            }
            val resultados = if (refreshedOt != null) {
                currentResults.map { if (it.id == otId) refreshedOt else it }
            } else {
                currentResults
            }
            val ots = if (ok) repo.obtenerOts() else _ui.value.ots
            _ui.update {
                it.copy(
                    mensaje = if (ok) "Estado actualizado" else "Transición inválida",
                    resultadosBusqueda = resultados,
                    ots = ots
                )
            }
        }
    }
}

private fun List<Ot>.filtrarPara(usuario: Usuario?): List<Ot> {
    if (usuario?.rol == Rol.MECANICO) {
        return filter { it.mecanicoAsignadoId == usuario.id }
    }
    return this
}
