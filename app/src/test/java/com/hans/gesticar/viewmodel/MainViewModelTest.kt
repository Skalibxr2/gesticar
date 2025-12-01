package com.hans.gesticar.viewmodel

import com.hans.gesticar.MainDispatcherRule
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.repository.FakeRepository
import com.hans.gesticar.repository.RemoteRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `login usa credenciales remotas y activa sincronizacion`() = runTest {
        val fakeRepository = FakeRepository()
        val remoteUser = Usuario(nombre = "Usuario Remoto", email = "remoto@gesticar.cl", password = "seguro")
        val remoteRepository = mockk<RemoteRepository>()
        coEvery { remoteRepository.obtenerOts(any()) } returns Result.success(emptyList())
        coEvery { remoteRepository.login(remoteUser.email, remoteUser.password) } returns Result.success(remoteUser)

        val viewModel = MainViewModel(fakeRepository, remoteRepository)

        viewModel.login(remoteUser.email, remoteUser.password)

        val state = withTimeout(1_000) { viewModel.ui.first { it.estaAutenticado } }

        assertEquals(remoteUser.nombre, state.displayName)
        assertTrue(state.sincronizandoRemoto)
    }

    @Test
    fun `login fallido mantiene usuario desautenticado`() = runTest {
        val viewModel = MainViewModel(FakeRepository())

        viewModel.login("admin@gesticar.cl", "clave-incorrecta")

        val state = withTimeout(1_000) { viewModel.ui.first { !it.estaAutenticado } }

        assertFalse(state.estaAutenticado)
        assertEquals("Credenciales inválidas", state.mensaje)
    }

    @Test
    fun `buscarPorFiltros sin usuario muestra mensaje de sesion`() {
        val viewModel = MainViewModel(FakeRepository())

        viewModel.buscarPorFiltros(numero = null, patente = null, rut = null, estado = OtState.CREADA)

        val state = viewModel.ui.value

        assertEquals("Debes iniciar sesión para buscar órdenes.", state.mensaje)
        assertTrue(state.resultadosBusqueda.isEmpty())
    }
}
