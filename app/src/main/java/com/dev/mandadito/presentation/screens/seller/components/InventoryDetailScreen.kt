package com.mandadito.components.seller

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

    var inventory by remember { mutableStateOf<Inventory?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Estados editables
    var editProductName by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editCurrentStock by remember { mutableStateOf("") }
    var editMinStock by remember { mutableStateOf("") }
    var editUnitPrice by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }

    // Cargar datos
    LaunchedEffect(inventoryId) {
        isLoading = true
        // TODO: Implementar repositorio para obtener item específico
        // Por ahora, simulamos carga
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
                    if (!isEditing) {
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
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (inventory == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("Producto no encontrado")
                    Button(onClick = onNavigateBack) {
                        Text("Volver")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isEditing) {
                    // Formulario de edición
                    OutlinedTextField(
                        value = editProductName,
                        onValueChange = { editProductName = it },
                        label = { Text("Nombre del producto") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Categoría") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) editUnitPrice = it
                        },
                        label = { Text("Precio unitario") },
                        leadingIcon = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("Ubicación") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Descripción") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    // TODO: Guardar cambios
                                    isEditing = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar")
                        }
                    }
                } else {
                    // Vista de solo lectura
                    inventory?.let { item ->
                        DetailCard(
                            title = "Información General",
                            content = {
                                DetailRow("Nombre", item.productName)
                                item.category?.let { DetailRow("Categoría", it) }
                                DetailRow("Stock Actual", item.currentStock.toString())
                                DetailRow("Stock Mínimo", item.minStock.toString())
                                DetailRow("Estado", item.status.replace("_", " "))
                            }
                        )

                        DetailCard(
                            title = "Precio e Inventario",
                            content = {
                                DetailRow("Precio Unitario", "$${item.unitPrice}")
                                DetailRow("Valor Total", "$${item.totalValue}")
                            }
                        )

                        if (!item.location.isNullOrEmpty() || !item.description.isNullOrEmpty()) {
                            DetailCard(
                                title = "Información Adicional",
                                content = {
                                    item.location?.let { DetailRow("Ubicación", it) }
                                    item.description?.let { DetailRow("Descripción", it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Eliminar Producto") },
            text = { Text("¿Estás seguro de eliminar este producto del inventario?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            // TODO: Eliminar producto
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    }
                ) {
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

@Composable
fun DetailCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}