package com.hans.gesticar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.Rol
import com.hans.gesticar.repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers


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

    // --- Login admin (mock) ---
    fun loginAdmin(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
    }


    fun logout() {
        _ui.update { it.copy(adminLoggedIn = false, usuarioAdmin = null, mensaje = null) }
    }


    // --- Búsquedas ---
    fun buscarPorNumero(numero: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val ot = repo.buscarOtPorNumero(numero)
            _ui.update {
                it.copy(
                    resultadosBusqueda = listOfNotNull(ot),
                    mensaje = if (ot == null) "Sin resultados" else null
                )
            }
        }
    }

    fun buscarPorPatente(patente: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lista = repo.buscarOtPorPatente(patente)
            _ui.update {
                it.copy(
                    resultadosBusqueda = lista,
                    mensaje = if (lista.isEmpty()) "Sin resultados" else null
                )
            }
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

class MainViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
