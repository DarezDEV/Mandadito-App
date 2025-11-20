package com.dev.mandadito.presentation.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.mandadito.data.models.ProductWithCategories
import com.dev.mandadito.presentation.screens.seller.components.AddProductDialog
import com.dev.mandadito.presentation.screens.seller.components.EditProductDialog
import com.dev.mandadito.presentation.screens.seller.components.ProductCard
import com.dev.mandadito.presentation.viewmodels.seller.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProductsScreen(
    viewModel: ProductViewModel? = null
) {
    val context = LocalContext.current
    val productViewModel = viewModel ?: remember { ProductViewModel(context) }
    val uiState by productViewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<ProductWithCategories?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<ProductWithCategories?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            productViewModel.clearSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            productViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productos", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { productViewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar productos...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { productViewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.showActiveOnly,
                        onClick = { productViewModel.setShowActiveOnly(!uiState.showActiveOnly) },
                        label = { Text("Activos") }
                    )

                    if (uiState.categories.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = uiState.selectedCategoryFilter != null,
                                onClick = { expanded = true },
                                label = {
                                    Text(
                                        uiState.selectedCategoryFilter?.let { categoryId ->
                                            uiState.categories.find { it.id == categoryId }?.let {
                                                it.icon?.let { icon -> "$icon ${it.name}" } ?: it.name
                                            } ?: "Categoría"
                                        } ?: "Todas las categorías"
                                    )
                                }
                            )

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todas las categorías") },
                                    onClick = {
                                        productViewModel.setCategoryFilter(null)
                                        expanded = false
                                    }
                                )
                                uiState.categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                category.icon?.let { "$it ${category.name}" } ?: category.name
                                            )
                                        },
                                        onClick = {
                                            productViewModel.setCategoryFilter(category.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when {
                uiState.isLoading && productViewModel.filteredProducts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                productViewModel.filteredProducts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "No se encontraron productos" else "No hay productos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = productViewModel.filteredProducts,
                            key = { it.id }
                        ) { product ->
                            ProductCard(
                                product = product,
                                onEdit = { showEditDialog = product },
                                onDelete = { showDeleteConfirm = product },
                                onToggleActive = {
                                    productViewModel.updateProduct(
                                        productId = product.id,
                                        name = product.name,
                                        description = product.description,
                                        price = product.price,
                                        stock = product.stock,
                                        imageUri = null,
                                        categoryIds = product.categories.map { it.id },
                                        isActive = !product.isActive
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            categories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onProductAdded = { name, description, price, stock, imageUri, categoryIds ->
                productViewModel.createProduct(name, description, price, stock, imageUri, categoryIds)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { product ->
        EditProductDialog(
            product = product,
            categories = uiState.categories,
            onDismiss = { showEditDialog = null },
            onProductUpdated = { name, description, price, stock, imageUri, categoryIds ->
                productViewModel.updateProduct(
                    productId = product.id,
                    name = name,
                    description = description,
                    price = price,
                    stock = stock,
                    imageUri = imageUri,
                    categoryIds = categoryIds
                )
                showEditDialog = null
            }
        )
    }

    showDeleteConfirm?.let { product ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Estás seguro de que deseas eliminar el producto \"${product.name}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        productViewModel.deleteProduct(product.id)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

