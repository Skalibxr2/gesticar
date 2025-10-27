package com.hans.gesticar


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hans.gesticar.ui.Routes
import com.hans.gesticar.ui.screens.LoginScreen
import com.hans.gesticar.ui.screens.CreateOtScreen
import com.hans.gesticar.ui.screens.HomeMenuScreen
import com.hans.gesticar.ui.screens.SearchOtScreen
import com.hans.gesticar.repository.SqliteRepository
import com.hans.gesticar.viewmodel.MainViewModel
import com.hans.gesticar.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    val context = LocalContext.current
                    val appContext = context.applicationContext
                    val factory = remember(appContext) { MainViewModelFactory(SqliteRepository(appContext)) }
                    val vm: MainViewModel = viewModel(factory = factory)
                    val uiState by vm.ui.collectAsState()

                    LaunchedEffect(uiState.estaAutenticado) {
                        if (uiState.estaAutenticado) {
                            nav.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    }

                    NavHost(navController = nav, startDestination = Routes.LOGIN) {
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                mensaje = uiState.mensaje,
                                onLogin = { email, pass -> vm.login(email, pass) }
                            )
                        }
                        composable(Routes.HOME) {
                            LaunchedEffect(uiState.estaAutenticado) {
                                if (!uiState.estaAutenticado) {
                                    nav.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                    }
                                }
                            }
                            HomeMenuScreen(vm = vm, nav = nav)
                        }
                        composable(Routes.SEARCH_OT) {
                            LaunchedEffect(uiState.estaAutenticado) {
                                if (!uiState.estaAutenticado) {
                                    nav.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.SEARCH_OT) { inclusive = true }
                                    }
                                }
                            }
                            SearchOtScreen(vm = vm)
                        }
                        composable(Routes.CREATE_OT) {
                            LaunchedEffect(uiState.estaAutenticado) {
                                if (!uiState.estaAutenticado) {
                                    nav.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.CREATE_OT) { inclusive = true }
                                    }
                                }
                            }
                            CreateOtScreen(vm = vm, nav = nav)
                        }
                    }
                }
            }
        }
    }
}
