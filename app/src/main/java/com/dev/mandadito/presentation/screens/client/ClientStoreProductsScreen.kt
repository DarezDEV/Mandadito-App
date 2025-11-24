package com.dev.mandadito.presentation.screens.client

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
    onProductSelected: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categorias = remember {
        listOf(
            Category(
                id = "1",
                name = "Panadería",
                description = "Productos de panadería",
                icon = "pan_icon",
                color = null,
                isActive = true,
                createdAt = "",
                updatedAt = ""
            ),
            Category(
                id = "2",
                name = "Bebidas",
                description = "Bebidas refrescantes",
                icon = "drink_icon",
                color = null,
                isActive = true,
                createdAt = "",
                updatedAt = ""
            ),
            Category(
                id = "3",
                name = "Lácteos",
                description = "Productos lácteos",
                icon = "milk_icon",
                color = null,
                isActive = true,
                createdAt = "",
                updatedAt = ""
            ),
            Category(
                id = "4",
                name = "Frutas",
                description = "Frutas frescas",
                icon = "fruit_icon",
                color = null,
                isActive = true,
                createdAt = "",
                updatedAt = ""
            )
        )
    }

    val categoriasPorId = remember(categorias) { categorias.associateBy { it.id } }

    val productos = remember {
        listOf(
            ProductWithCategories(
                id = "1",
                name = "Coca Cola",
                description = "Refresco de cola clásico 2L",
                price = 25.0,
                stock = 10,
                imageUrl = "https://example.com/coca.jpg",
                images = listOf(ProductImage(url = "https://example.com/coca.jpg", order = 0, isPrimary = true)),
                createdAt = "",
                updatedAt = "",
                categories = listOfNotNull(categoriasPorId["2"])
            ),
            ProductWithCategories(
                id = "2",
                name = "Pepsi",
                description = "Refresco de cola 2L",
                price = 24.0,
                stock = 15,
                imageUrl = "https://example.com/pepsi.jpg",
                images = listOf(ProductImage(url = "https://example.com/pepsi.jpg", order = 0, isPrimary = true)),
                createdAt = "",
                updatedAt = "",
                categories = listOfNotNull(categoriasPorId["2"])
            ),
            ProductWithCategories(
                id = "3",
                name = "Jugo Sprite",
                description = "Refresco de limón 2L",
                price = 22.0,
                stock = 8,
                imageUrl = "https://example.com/sprite.jpg",
                images = listOf(ProductImage(url = "https://example.com/sprite.jpg", order = 0, isPrimary = true)),
                createdAt = "",
                updatedAt = "",
                categories = listOfNotNull(categoriasPorId["2"])
            ),
            ProductWithCategories(
                id = "4",
                name = "Jugo 100%",
                description = "Jugo natural 1L",
                price = 30.0,
                stock = 12,
                imageUrl = "https://example.com/jugo.jpg",
                images = listOf(ProductImage(url = "https://example.com/jugo.jpg", order = 0, isPrimary = true)),
                createdAt = "",
                updatedAt = "",
                categories = listOfNotNull(categoriasPorId["3"])
            ),
            ProductWithCategories(
                id = "5",
                name = "Agua Cristal",
                description = "Agua mineral 1.5L",
                price = 10.0,
                stock = 20,
                imageUrl = "https://example.com/agua.jpg",
                images = listOf(ProductImage(url = "https://example.com/agua.jpg", order = 0, isPrimary = true)),
                createdAt = "",
                updatedAt = "",
                categories = listOfNotNull(categoriasPorId["2"])
            ),
            ProductWithCategories(
                id = "6",
                name = "Fanta",
                description = "Refresco de naranja 2L",
                price = 23.0,
                stock = 18,
                imageUrl = "https://example.com/fanta.jpg",
                images = listOf(ProductImage(url = "https://example.com/fanta.jpg", order = 0, isPrimary = true)),
                createdAt = "",
                updatedAt = "",
                categories = listOfNotNull(categoriasPorId["2"])
            )
        )
    }

    val productosFiltrados = remember(searchQuery, selectedCategory) {
        productos.filter {
            it.name.contains(searchQuery, ignoreCase = true) &&
                    (selectedCategory == null || it.categories.any { cat -> cat.id == selectedCategory })
        }
    }

    val colmadoNombre = "Colmado El Men"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(colmadoNombre, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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

                // Categorías
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categorias) { categoria ->
                        FilterChip(
                            selected = selectedCategory == categoria.id,
                            onClick = {
                                selectedCategory = if (selectedCategory == categoria.id) null else categoria.id
                            },
                            label = { Text(categoria.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (selectedCategory == categoria.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                // Productos en grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    gridItems(productosFiltrados) { producto ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(20.dp))
                                .clickable { onProductSelected(producto.id) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Box {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(producto.primaryImage ?: producto.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = producto.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.ic_launcher_foreground)
                                )
                            }

                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = producto.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                producto.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$${producto.price}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    IconButton(
                                        onClick = { onProductSelected(producto.id) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Agregar",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
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
}