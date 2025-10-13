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
import com.hans.gesticar.ui.Routes
import com.hans.gesticar.ui.screens.HomeScreen
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

                    NavHost(navController = nav, startDestination = Routes.LOGIN) {
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                onLogin = { email, pass ->
                                    vm.loginAdmin(email, pass)
                                    if (vm.ui.value.adminLoggedIn) nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                                }
                            )
                        }
                        composable(Routes.HOME) {
                            HomeScreen(vm = vm)
                        }
                    }
                }
            }
        }
    }
}