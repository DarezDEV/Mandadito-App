package com.dev.mandadito.presentation.navigation

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.dev.mandadito.data.repository.AddressRepository
import com.dev.mandadito.presentation.screens.client.AddAddressScreen
import com.dev.mandadito.presentation.screens.client.AddressListScreen
import com.dev.mandadito.presentation.viewmodels.client.AddressViewModel

/**
 * Rutas del módulo de direcciones
 */
object AddressRoutes {
    const val GRAPH = "address_graph"
    const val LIST = "address_list"
    const val ADD = "address_add"
    const val ADD_WITH_ID = "address_add?addressId={addressId}"
}

/**
 * NavGraph del módulo de direcciones
 */
fun NavGraphBuilder.addressNavGraph(navController: NavHostController) {
    navigation(
        startDestination = AddressRoutes.LIST,
        route = AddressRoutes.GRAPH
    ) {
        composable(AddressRoutes.LIST) {
            val context = LocalContext.current
            val repository = remember { AddressRepository(context) }
            val viewModel = remember(repository) { AddressViewModel(repository) }

            AddressListScreen(
                viewModel = viewModel,
                onAddAddress = {
                    navController.navigate(AddressRoutes.ADD)
                },
                onEditAddress = { addressId ->
                    navController.navigateToEditAddress(addressId)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = AddressRoutes.ADD_WITH_ID,
            arguments = listOf(
                navArgument("addressId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val context = LocalContext.current
            val addressId = backStackEntry.arguments?.getString("addressId")

            val repository = remember { AddressRepository(context) }
            val viewModel = remember(repository) { AddressViewModel(repository) }

            AddAddressScreen(
                viewModel = viewModel,
                addressId = addressId,
                onNavigateBack = { createdAddressId ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("new_address_id", createdAddressId)
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Extensiones para facilitar la navegación
 */
fun NavHostController.navigateToAddressList() {
    navigate(AddressRoutes.LIST)
}

fun NavHostController.navigateToAddAddress() {
    navigate(AddressRoutes.ADD)
}

fun NavHostController.navigateToEditAddress(addressId: String) {
    navigate("address_add?addressId=$addressId")
}