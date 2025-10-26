package com.hans.gesticar


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.hans.gesticar.ui.Routes
import com.hans.gesticar.ui.screens.HomeMenuScreen
import com.hans.gesticar.ui.screens.SearchOtScreen
import com.hans.gesticar.ui.screens.LoginScreen
import com.hans.gesticar.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    val vm: MainViewModel = viewModel()

                    val uiState by vm.ui.collectAsState()

                    NavHost(navController = nav, startDestination = Routes.LOGIN) {
                        composable(Routes.LOGIN) {
                            LaunchedEffect(uiState.isLoggedIn) {
                                if (uiState.isLoggedIn) {
                                    nav.navigate(Routes.HOME) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                }
                            }
                            LoginScreen(
                                uiState = uiState,
                                onLogin = { email, pass -> vm.login(email, pass) }
                            )
                        }
                        composable(Routes.HOME) {
                            LaunchedEffect(uiState.isLoggedIn) {
                                if (!uiState.isLoggedIn) {
                                    nav.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                    }
                                }
                            }
                            HomeMenuScreen(vm = vm, nav = nav)
                        }
                        composable(Routes.SEARCH_OT) {
                            LaunchedEffect(uiState.isLoggedIn) {
                                if (!uiState.isLoggedIn) {
                                    nav.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.SEARCH_OT) { inclusive = true }
                                    }
                                }
                            }
                            SearchOtScreen(vm = vm)
                        }
                    }
                }
            }
        }
    }
}