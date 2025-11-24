package com.dev.mandadito.presentation.screens.seller.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.dev.mandadito.data.models.Category
import com.dev.mandadito.data.models.ProductWithCategories

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProductDialog(
    product: ProductWithCategories,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onProductUpdated: (
        name: String,
        description: String?,
        price: Double,
        stock: Int,
        newImageUris: List<Uri>,
        existingImageUrls: List<String>,
        categoryIds: List<String>
    ) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var description by remember { mutableStateOf(product.description ?: "") }
    var price by remember { mutableStateOf(product.price.toString()) }
    var stock by remember { mutableStateOf(product.stock.toString()) }

    // Imágenes existentes (URLs de la BD)
    var existingImageUrls by remember { mutableStateOf(product.allImageUrls) }
    // Nuevas imágenes seleccionadas (URIs locales)
    var newImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var selectedCategories by remember {
        mutableStateOf<Set<String>>(product.categories.map { it.id }.toSet())
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val totalImages = existingImageUrls.size + newImageUris.size + uris.size
        if (totalImages > 5) {
            val remainingSlots = 5 - (existingImageUrls.size + newImageUris.size)
            newImageUris = newImageUris + uris.take(remainingSlots.coerceAtLeast(0))
        } else {
            newImageUris = newImageUris + uris
        }
    }

    val totalImages = existingImageUrls.size + newImageUris.size

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Editar Producto",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Sección de imágenes
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Imágenes ($totalImages/5) *",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (totalImages < 5) {
                            IconButton(
                                onClick = { galleryLauncher.launch("image/*") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Agregar imágenes"
                                )
                            }
                        }
                    }

                    if (totalImages == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Agregar imágenes (1-5)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mostrar imágenes existentes
                            itemsIndexed(existingImageUrls) { index, url ->
                                Box {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Imagen existente ${index + 1}",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Badge principal
                                    if (index == 0 && newImageUris.isEmpty()) {
                                        Badge(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(8.dp),
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text("Principal", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    // Botón eliminar
                                    IconButton(
                                        onClick = {
                                            existingImageUrls = existingImageUrls.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(32.dp)
                                            .padding(4.dp)
                                            .background(
                                                MaterialTheme.colorScheme.error,
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Mostrar nuevas imágenes
                            itemsIndexed(newImageUris) { index, uri ->
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Nueva imagen ${index + 1}",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Badge "Nueva"
                                    Badge(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp),
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    ) {
                                        Text("Nueva", style = MaterialTheme.typography.labelSmall)
                                    }
                                    // Badge principal si es la primera
                                    if (existingImageUrls.isEmpty() && index == 0) {
                                        Badge(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(8.dp),
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text("Principal", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    // Botón eliminar
                                    IconButton(
                                        onClick = {
                                            newImageUris = newImageUris.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(32.dp)
                                            .padding(4.dp)
                                            .background(
                                                MaterialTheme.colorScheme.error,
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = "La primera imagen será la principal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) price = it },
                        label = { Text("Precio *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Text("$") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = stock,
                        onValueChange = { if (it.all { char -> char.isDigit() }) stock = it },
                        label = { Text("Stock *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Categorías (${selectedCategories.size}) *",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategories.contains(category.id),
                                onClick = {
                                    selectedCategories = if (selectedCategories.contains(category.id)) {
                                        selectedCategories - category.id
                                    } else {
                                        selectedCategories + category.id
                                    }
                                },
                                label = {
                                    Text(
                                        text = category.icon?.let { "$it ${category.name}" } ?: category.name
                                    )
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val priceValue = price.toDoubleOrNull() ?: 0.0
                            val stockValue = stock.toIntOrNull() ?: 0
                            if (name.isNotBlank() && priceValue > 0 && totalImages > 0 && selectedCategories.isNotEmpty()) {
                                onProductUpdated(
                                    name.trim(),
                                    description.takeIf { it.isNotBlank() },
                                    priceValue,
                                    stockValue,
                                    newImageUris,
                                    existingImageUrls,
                                    selectedCategories.toList()
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank() &&
                                price.toDoubleOrNull()?.let { it > 0 } == true &&
                                totalImages > 0 &&
                                selectedCategories.isNotEmpty()
                    ) {
                        Text("Guardar")
                    }
                }

                // Mensajes de validación
                if (totalImages == 0) {
                    Text(
                        text = "⚠️ Debe tener al menos 1 imagen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (selectedCategories.isEmpty()) {
                    Text(
                        text = "⚠️ Debe seleccionar al menos 1 categoría",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}