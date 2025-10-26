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
import com.hans.gesticar.ui.screens.HomeMenuScreen
import com.hans.gesticar.ui.screens.SearchOtScreen
import com.hans.gesticar.ui.screens.LoginScreen
import com.hans.gesticar.viewmodel.MainViewModel
import com.hans.gesticar.viewmodel.MainViewModelFactory
import com.hans.gesticar.repository.SqliteRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    val context = LocalContext.current
                    val repository = remember { SqliteRepository(context.applicationContext) }
                    val vm: MainViewModel = viewModel(factory = MainViewModelFactory(repository))
                    val ui by vm.ui.collectAsState()

                    LaunchedEffect(ui.adminLoggedIn) {
                        if (ui.adminLoggedIn) {
                            nav.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    }

                    NavHost(navController = nav, startDestination = Routes.LOGIN) {
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                errorMessage = if (!ui.adminLoggedIn) ui.mensaje else null,
                                onLogin = { email, pass ->
                                    vm.loginAdmin(email, pass)
                                }
                            )
                        }
                        composable(Routes.HOME) { HomeMenuScreen(vm = vm, nav = nav) }
                        composable(Routes.SEARCH_OT) { SearchOtScreen(vm = vm) }
                    }
                }
            }
        }
    }
}