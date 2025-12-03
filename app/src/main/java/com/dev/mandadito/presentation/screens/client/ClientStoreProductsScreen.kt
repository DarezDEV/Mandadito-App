package com.dev.mandadito.presentation.screens.client

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dev.mandadito.presentation.viewmodels.ClientStoreProductsViewModel
import com.dev.mandadito.data.models.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dev.mandadito.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientStoreProductsScreen(
    colmadoId: String,
    navController: NavController,
    viewModel: ClientStoreProductsViewModel = viewModel(),
    onProductSelected: (String) -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val colmado by viewModel.selectedColmado.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Cargar productos cuando cambia el ID
    LaunchedEffect(colmadoId) {
        viewModel.loadProductsForColmado(colmadoId)
    }

    // Filtrar productos localmente
    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        colmado?.name ?: "Productos",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { viewModel.loadProductsForColmado(colmadoId) }
                        ) {
                            Text("Reintentar")
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Barra de búsqueda
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .shadow(6.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Buscar productos...") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }

                        // Grid de productos
                        if (filteredProducts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No hay productos disponibles")
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(filteredProducts.size) { index ->
                                    val producto = filteredProducts[index]
                                    ProductCard(
                                        product = producto,
                                        onClick = { onProductSelected(producto.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            AsyncImage(
                model = product.imageUrl ?: "https://via.placeholder.com/150",
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "RD$ ${product.price}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (product.stock <= 0) {
                    Text(
                        text = "Agotado",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}