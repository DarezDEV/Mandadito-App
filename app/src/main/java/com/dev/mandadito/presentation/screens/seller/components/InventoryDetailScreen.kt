package com.dev.mandadito.presentation.screens.seller  // Ajusta si tu package es diferente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.Inventory
import com.dev.mandadito.data.repository.InventoryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    inventoryId: String,
    sellerId: String,
    sellerName: String,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = remember { InventoryRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    var inventory by remember { mutableStateOf<Inventory?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estados editables
    var editProductName by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editCurrentStock by remember { mutableStateOf("") }
    var editMinStock by remember { mutableStateOf("") }
    var editUnitPrice by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }

    // Cargar datos (aún es simulado, adapta si tienes carga real)
    LaunchedEffect(inventoryId) {
        isLoading = true
        // TODO: Aquí deberías cargar el item real desde el repository usando inventoryId
        // Por ahora, simulamos (reemplaza con carga real)
        // val result = repository.getInventoryItemById(inventoryId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Producto" else "Detalle del Producto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (!isEditing && inventory != null) {
                        IconButton(onClick = {
                            inventory?.let {
                                editProductName = it.productName
                                editCategory = it.category ?: ""
                                editCurrentStock = it.currentStock.toString()
                                editMinStock = it.minStock.toString()
                                editUnitPrice = it.unitPrice.toString()
                                editLocation = it.location ?: ""
                                editDescription = it.description ?: ""
                                isEditing = true
                            }
                        }) {
                            Icon(Icons.Default.Edit, "Editar")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Eliminar")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (inventory == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Producto no encontrado")
            }
        } else {
            Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                if (isEditing) {
                    // Formulario de edición
                    OutlinedTextField(
                        value = editProductName,
                        onValueChange = { editProductName = it },
                        label = { Text("Nombre del producto") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Categoría") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editCurrentStock,
                            onValueChange = { if (it.all { char -> char.isDigit() } || it.isEmpty()) editCurrentStock = it },
                            label = { Text("Stock actual") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = editMinStock,
                            onValueChange = { if (it.all { char -> char.isDigit() } || it.isEmpty()) editMinStock = it },
                            label = { Text("Stock mínimo") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    OutlinedTextField(
                        value = editUnitPrice,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) editUnitPrice = it },
                        label = { Text("Precio unitario") },
                        leadingIcon = { Text("$") },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("Ubicación") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Descripción") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )

                    Row(modifier = Modifier.padding(16.dp)) {
                        OutlinedButton(onClick = { isEditing = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar")
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val stock = editCurrentStock.toIntOrNull() ?: 0
                                val minStock = editMinStock.toIntOrNull() ?: 5
                                val price = editUnitPrice.toDoubleOrNull() ?: 0.0

                                // Determinar el status basado en el stock
                                val newStatus = when {
                                    stock == 0 -> "OUT_OF_STOCK"
                                    stock <= minStock -> "LOW_STOCK"
                                    else -> "IN_STOCK"
                                }

                                val updatedItem = inventory!!.copy(
                                    productName = editProductName.trim(),
                                    category = editCategory.takeIf { it.isNotBlank() },
                                    currentStock = stock,
                                    minStock = minStock,
                                    unitPrice = price,
                                    totalValue = stock * price,
                                    location = editLocation.takeIf { it.isNotBlank() },
                                    description = editDescription.takeIf { it.isNotBlank() },
                                    status = newStatus
                                )

                                scope.launch {
                                    repository.updateInventoryItem(updatedItem).fold(
                                        onSuccess = {
                                            inventory = it
                                            isEditing = false
                                            snackbarHostState.showSnackbar("Producto actualizado correctamente")
                                        },
                                        onFailure = {
                                            errorMessage = "Error al guardar: ${it.message}"
                                            snackbarHostState.showSnackbar(errorMessage ?: "Error desconocido")
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar")
                        }
                    }
                } else {
                    // Vista normal (detalle)
                    inventory?.let { item ->
                        DetailCard(title = "Información General") {
                            DetailRow("Nombre", item.productName)
                            item.category?.let { DetailRow("Categoría", it) }
                            DetailRow("Stock Actual", item.currentStock.toString())
                            DetailRow("Stock Mínimo", item.minStock.toString())
                            DetailRow("Estado", item.status.replace("_", " "))
                        }
                        DetailCard(title = "Precio e Inventario") {
                            DetailRow("Precio Unitario", "$${item.unitPrice}")
                            DetailRow("Valor Total", "$${item.totalValue}")
                        }
                        if (!item.location.isNullOrEmpty() || !item.description.isNullOrEmpty()) {
                            DetailCard(title = "Información Adicional") {
                                item.location?.let { DetailRow("Ubicación", it) }
                                item.description?.let { DetailRow("Descripción", it) }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de eliminación (mantén el que tenías)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Producto") },
            text = { Text("¿Estás seguro? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    // TODO: Implementar eliminación real
                    showDeleteDialog = false
                    onNavigateBack()
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Las funciones DetailCard y DetailRow se mantienen iguales a las que tenías
@Composable
fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) { /* igual que antes */ }

@Composable
fun DetailRow(label: String, value: String) { /* igual que antes */ }