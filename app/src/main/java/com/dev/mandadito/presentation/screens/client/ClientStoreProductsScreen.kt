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
            Categoria(
                id = "1",
                nombre = "Panadería",
                descripcion = "Productos de panadería",
                icono = "pan_icon"
            ),
            Categoria(
                id = "2",
                nombre = "Bebidas",
                descripcion = "Bebidas refrescantes",
                icono = "drink_icon"
            ),
            Categoria(
                id = "3",
                nombre = "Lácteos",
                descripcion = "Productos lácteos",
                icono = "milk_icon"
            ),
            Categoria(
                id = "4",
                nombre = "Frutas",
                descripcion = "Frutas frescas",
                icono = "fruit_icon"
            )
        )
    }

    val productos = remember {
        listOf(
            Producto(
                id = "1",
                nombre = "Coca Cola",
                descripcion = "Refresco de cola clásico 2L",
                precio = 25.0,
                calificacion = 4.5,
                imageUrl = "https://example.com/coca.jpg",
                categoriaId = "2",
                colmadoId = colmadoId
            ),
            Producto(
                id = "2",
                nombre = "Pepsi",
                descripcion = "Refresco de cola 2L",
                precio = 24.0,
                calificacion = 4.2,
                imageUrl = "https://example.com/pepsi.jpg",
                categoriaId = "2",
                colmadoId = colmadoId
            ),
            Producto(
                id = "3",
                nombre = "Jugo Sprite",
                descripcion = "Refresco de limón 2L",
                precio = 22.0,
                calificacion = 4.0,
                imageUrl = "https://example.com/sprite.jpg",
                categoriaId = "2",
                colmadoId = colmadoId
            ),
            Producto(
                id = "4",
                nombre = "Jugo 100%",
                descripcion = "Jugo natural 1L",
                precio = 30.0,
                calificacion = 4.7,
                imageUrl = "https://example.com/jugo.jpg",
                categoriaId = "3",
                colmadoId = colmadoId
            ),
            Producto(
                id = "5",
                nombre = "Agua Cristal",
                descripcion = "Agua mineral 1.5L",
                precio = 10.0,
                calificacion = 5.0,
                imageUrl = "https://example.com/agua.jpg",
                categoriaId = "2",
                colmadoId = colmadoId
            ),
            Producto(
                id = "6",
                nombre = "Fanta",
                descripcion = "Refresco de naranja 2L",
                precio = 23.0,
                calificacion = 4.1,
                imageUrl = "https://example.com/fanta.jpg",
                categoriaId = "2",
                colmadoId = colmadoId
            )
        )
    }

    val productosFiltrados = remember(searchQuery, selectedCategory) {
        productos.filter {
            it.nombre.contains(searchQuery, ignoreCase = true) &&
                    (selectedCategory == null || it.categoriaId == selectedCategory)
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
                            label = { Text(categoria.nombre) },
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
                                        .data(producto.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = producto.nombre,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.ic_launcher_foreground)
                                )

                                // Badge de rating
                                producto.calificacion?.let { rating ->
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFFB300)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = rating.toString(),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = producto.nombre,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                producto.descripcion?.let { desc ->
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
                                        text = "$${producto.precio}",
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