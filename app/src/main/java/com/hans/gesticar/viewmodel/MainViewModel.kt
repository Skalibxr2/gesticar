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
    val estaAutenticado: Boolean = false,
    val usuarioActual: Usuario? = null,
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
    fun login(email: String, password: String) {
        val user = repo.findUserByEmail(email)
        val ok = user != null && repo.validarPassword(user, password)

        _ui.update {
            it.copy(
                estaAutenticado = ok,
                usuarioActual = if (ok) user else null,
                displayName = if (ok) user!!.nombre else null,
                resultadosBusqueda = emptyList(),
                mensaje = if (ok) null else "Credenciales inválidas"
            )
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

        val ot = repo.buscarOtPorNumero(numero)
        val resultados = listOfNotNull(ot).filtrarPara(usuario)
        val mensaje = when {
            ot == null -> "Sin resultados"
            resultados.isEmpty() -> "Esta OT no está asignada a ti"
            else -> null
        }
        _ui.update { it.copy(resultadosBusqueda = resultados, mensaje = mensaje) }
    }

    fun buscarPorPatente(patente: String) {
        val usuario = _ui.value.usuarioActual
        if (usuario == null) {
            _ui.update {
                it.copy(resultadosBusqueda = emptyList(), mensaje = "Debes iniciar sesión para buscar órdenes.")
            }
            return
        }

        val lista = repo.buscarOtPorPatente(patente).filtrarPara(usuario)
        val mensaje = if (lista.isEmpty()) "Sin resultados" else null
        _ui.update { it.copy(resultadosBusqueda = lista, mensaje = mensaje) }
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

private fun List<Ot>.filtrarPara(usuario: Usuario?): List<Ot> {
    if (usuario?.rol == Rol.MECANICO) {
        return filter { it.mecanicoAsignadoId == usuario.id }
    }
    return this
}
