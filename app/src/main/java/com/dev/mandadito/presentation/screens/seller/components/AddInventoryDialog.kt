package com.dev.mandadito.presentation.screens.seller.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.Inventory
import com.dev.mandadito.data.repository.InventoryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInventoryDialog(
    sellerId: String,
    sellerName: String = "",
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repo = remember { InventoryRepository() }

    var productName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var currentStock by remember { mutableStateOf("0") }
    var minStock by remember { mutableStateOf("5") }
    var unitPrice by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Producto al Inventario") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Nombre del producto *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentStock,
                        onValueChange = { if (it.all { char -> char.isDigit() } || it.isEmpty()) currentStock = it },
                        label = { Text("Stock inicial *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { if (it.all { char -> char.isDigit() } || it.isEmpty()) minStock = it },
                        label = { Text("Stock mínimo") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) unitPrice = it
                    },
                    label = { Text("Precio unitario *") },
                    leadingIcon = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicación (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (productName.isBlank() || currentStock.isBlank() || unitPrice.isBlank()) {
                        errorMessage = "Campos obligatorios faltantes"
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        val stock = currentStock.toIntOrNull() ?: 0
                        val price = unitPrice.toDoubleOrNull() ?: 0.0

                        val newItem = Inventory(
                            id = "", // se genera en Supabase
                            sellerId = sellerId,
                            sellerName = sellerName,
                            productName = productName.trim(),
                            category = category.takeIf { it.isNotBlank() },
                            currentStock = stock,
                            minStock = minStock.toIntOrNull() ?: 5,
                            unitPrice = price,
                            totalValue = stock * price,
                            location = location.takeIf { it.isNotBlank() },
                            description = description.takeIf { it.isNotBlank() },
                            createdAt = "",
                            updatedAt = ""
                        )

                        repo.addInventoryItem(newItem).onSuccess {
                            onSuccess()
                        }.onFailure {
                            errorMessage = "Error: ${it.message}"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar")
            }
        }
    )
}