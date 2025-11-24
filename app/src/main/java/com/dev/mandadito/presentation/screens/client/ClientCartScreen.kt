package com.dev.mandadito.presentation.screens.client

import com.dev.mandadito.data.models.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

data class ItemCarrito(
    val id: String,
    val colmado: Colmado,
    val producto: ProductWithCategories,
    val cantidad: Int,
    val precioUnitario: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientCartScreen(
    navController: NavController,
    onCheckout: () -> Unit = {}
) {
    var expandirColmado by remember { mutableStateOf<String?>(null) }

    val categoriaBebidas = remember {
        Category(
            id = "2",
            name = "Bebidas",
            description = "Bebidas refrescantes",
            icon = "drink_icon",
            color = null,
            isActive = true,
            createdAt = "",
            updatedAt = ""
        )
    }

    val itemsCarrito = remember {
        listOf(
            ItemCarrito(
                id = "1",
                colmado = Colmado(
                    id = "2",
                    sellerId = "seller2",
                    name = "Colmado El Men",
                    address = "Avenida Central #456",
                    phone = "987654321",
                    description = "Colmado con entrega rápida",
                    createdAt = "2023-01-02T00:00:00Z",
                    updatedAt = "2023-01-02T00:00:00Z"
                ),
                producto = ProductWithCategories(
                    id = "1",
                    name = "Coca Cola",
                    description = "Refresco de cola clásico 2L",
                    price = 25.0,
                    stock = 10,
                    imageUrl = "https://example.com/coca.jpg",
                    images = listOf(ProductImage(url = "https://example.com/coca.jpg", order = 0, isPrimary = true)),
                    isActive = true,
                    createdAt = "",
                    updatedAt = "",
                    categories = listOf(categoriaBebidas)
                ),
                cantidad = 2,
                precioUnitario = 25.0
            ),
            ItemCarrito(
                id = "2",
                colmado = Colmado(
                    id = "2",
                    sellerId = "seller2",
                    name = "Colmado El Men",
                    address = "Avenida Central #456",
                    phone = "987654321",
                    description = "Colmado con entrega rápida",
                    createdAt = "2023-01-02T00:00:00Z",
                    updatedAt = "2023-01-02T00:00:00Z"
                ),
                producto = ProductWithCategories(
                    id = "2",
                    name = "Pepsi",
                    description = "Refresco de cola 2L",
                    price = 24.0,
                    stock = 15,
                    imageUrl = "https://example.com/pepsi.jpg",
                    images = listOf(ProductImage(url = "https://example.com/pepsi.jpg", order = 0, isPrimary = true)),
                    isActive = true,
                    createdAt = "",
                    updatedAt = "",
                    categories = listOf(categoriaBebidas)
                ),
                cantidad = 1,
                precioUnitario = 24.0
            ),
            ItemCarrito(
                id = "3",
                colmado = Colmado(
                    id = "1",
                    sellerId = "seller1",
                    name = "Colmado Rey",
                    address = "Calle Principal #123",
                    phone = "123456789",
                    description = "Colmado local con variedad de productos",
                    createdAt = "2023-01-01T00:00:00Z",
                    updatedAt = "2023-01-01T00:00:00Z"
                ),
                producto = ProductWithCategories(
                    id = "3",
                    name = "Jugo Sprite",
                    description = "Refresco de limón 2L",
                    price = 22.0,
                    stock = 12,
                    imageUrl = "https://example.com/sprite.jpg",
                    images = listOf(ProductImage(url = "https://example.com/sprite.jpg", order = 0, isPrimary = true)),
                    isActive = true,
                    createdAt = "",
                    updatedAt = "",
                    categories = listOf(categoriaBebidas)
                ),
                cantidad = 3,
                precioUnitario = 22.0
            )
        )
    }

    val subtotal = itemsCarrito.sumOf { it.cantidad * it.precioUnitario }
    val total = subtotal * 1.12

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mi Carrito",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                if (itemsCarrito.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Tu carrito está vacío",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "¡Agrega productos para comenzar!",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(
                            items = itemsCarrito.groupBy { it.colmado.id }.entries.toList(),
                            key = { it.key }
                        ) { entry ->
                            val colmadoId = entry.key
                            val itemsList = entry.value
                            val colmado = itemsList.first().colmado

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandirColmado = if (expandirColmado == colmadoId) null else colmadoId
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .shadow(4.dp, CircleShape)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Store,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = colmado.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = colmado.address,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = if (expandirColmado == colmadoId) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (expandirColmado == colmadoId) {
                                        Column {
                                            itemsList.forEach { item ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                                    shape = RoundedCornerShape(16.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        AsyncImage(
                                                            model = item.producto.primaryImage ?: item.producto.imageUrl,
                                                            contentDescription = item.producto.name,
                                                            modifier = Modifier
                                                                .size(60.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = item.producto.name,
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            item.producto.description?.let { desc ->
                                                                Text(
                                                                    text = desc,
                                                                    fontSize = 14.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                        Column(
                                                            horizontalAlignment = Alignment.End
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(20.dp),
                                                                color = MaterialTheme.colorScheme.primaryContainer
                                                            ) {
                                                                Text(
                                                                    text = "x${item.cantidad}",
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                            Text(
                                                                text = "$${item.cantidad * item.precioUnitario}",
                                                                fontSize = 17.sp,
                                                                fontWeight = FontWeight.Bold
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

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .shadow(12.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                    )
                                )
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal", fontSize = 16.sp)
                                Text(
                                    "$${subtotal}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("IVA (12%)", fontSize = 16.sp)
                                Text(
                                    "$${subtotal * 0.12}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            HorizontalDivider(
                                thickness = 2.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$${total}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onCheckout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null
                            )
                            Text(
                                "Ir al pago",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}