package com.dev.mandadito.presentation.screens.client

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dev.mandadito.data.models.CartItemDetail
import com.dev.mandadito.data.models.CartWithItems
import com.dev.mandadito.presentation.viewmodels.client.ClientCartViewModel
import com.dev.mandadito.presentation.viewmodels.common.UiState
import com.dev.mandadito.presentation.components.connectivity.GlobalConnectivityBar
import com.dev.mandadito.presentation.components.connectivity.CacheBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientCartScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel = remember { ClientCartViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Mi Carrito",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("client_home") }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1C49C0),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F7FA),
        snackbarHost = {
            com.dev.mandadito.presentation.components.CustomSnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                GlobalConnectivityBar(isConnected = uiState.isConnected)

                when (val state = uiState.cartsState) {
                    is UiState.Idle, is UiState.Loading -> {
                        LoadingCartState()
                    }

                    is UiState.Success -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (state.isFromCache && state.cacheTimestamp != null) {
                                CacheBadge(
                                    isFromCache = state.isFromCache,
                                    cacheTimestamp = state.cacheTimestamp
                                )
                            }

                            if (state.data.isEmpty()) {
                                EmptyCartState(navController)
                            } else {
                                CartList(
                                    carts = state.data,
                                    viewModel = viewModel,
                                    navController = navController
                                )
                            }
                        }
                    }

                    is UiState.Retrying -> {
                        LoadingCartState()
                    }

                    is UiState.Error -> {
                        ErrorCartState(
                            message = state.message,
                            onRetry = { viewModel.loadCarts() }
                        )
                    }

                    is UiState.Offline -> {
                        if (state.cachedData != null && state.cachedData.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                CacheBadge(
                                    isFromCache = true,
                                    cacheTimestamp = System.currentTimeMillis()
                                )

                                CartList(
                                    carts = state.cachedData,
                                    viewModel = viewModel,
                                    navController = navController
                                )
                            }
                        } else {
                            OfflineCartState(message = state.message)
                        }
                    }
                }
            }
        }
    }

    // Manejar notificaciones de error y éxito
    com.dev.mandadito.presentation.components.SnackbarHandler(
        snackbarHostState = snackbarHostState,
        errorMessage = uiState.errorMessage,
        successMessage = uiState.successMessage,
        onErrorDismiss = { viewModel.clearError() },
        onSuccessDismiss = { viewModel.clearSuccess() }
    )
}

@Composable
private fun LoadingCartState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(50.dp),
                strokeWidth = 4.dp,
                color = Color(0xFF1C49C0)
            )
            Text(
                text = "Cargando carrito...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF44464F)
            )
        }
    }
}

@Composable
private fun EmptyCartState(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD8E2FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color(0xFF1C49C0)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tu carrito está vacío",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1A1B1F)
                )
                Text(
                    text = "Explora colmados y agrega productos que te gusten",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF44464F)
                )
            }

            Button(
                onClick = { navController.navigate("client_home") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C49C0)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Explorar colmados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ErrorCartState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFDAD6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xFFBA1A1A)
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF44464F),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C49C0)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reintentar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OfflineCartState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFDAD6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xFFBA1A1A)
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF44464F),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CartList(
    carts: List<CartWithItems>,
    viewModel: ClientCartViewModel,
    navController: NavHostController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = carts,
            key = { it.summary.cartId }
        ) { cartWithItems ->
            ColmadoCartCard(
                cartWithItems = cartWithItems,
                onIncrementQuantity = { itemId, currentQty ->
                    viewModel.incrementQuantity(itemId, currentQty)
                },
                onDecrementQuantity = { itemId, currentQty ->
                    viewModel.decrementQuantity(itemId, currentQty)
                },
                onRemoveProduct = { itemId ->
                    viewModel.removeItem(itemId)
                },
                onClearCart = { cartId ->
                    viewModel.clearCart(cartId)
                },
                navController = navController
            )
        }
    }
}

@Composable
fun ColmadoCartCard(
    cartWithItems: CartWithItems,
    onIncrementQuantity: (String, Int) -> Unit,
    onDecrementQuantity: (String, Int) -> Unit,
    onRemoveProduct: (String) -> Unit,
    onClearCart: (String) -> Unit,
    navController: NavHostController
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "rotation"
    )

    val summary = cartWithItems.summary
    val items = cartWithItems.items

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Header - Clickeable para expandir
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícono del colmado
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFD8E2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color(0xFF1C49C0)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Info del colmado
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = summary.colmadoName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1B1F)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF5558A3)
                            )
                            Text(
                                text = summary.colmadoAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF44464F)
                            )
                        }
                    }

                    // Badge de items y botón expandir
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD8E2FF)
                        ) {
                            Text(
                                text = "${items.size} ${if (items.size == 1) "ítem" else "ítems"}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C49C0)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Contraer" else "Expandir",
                            modifier = Modifier
                                .size(26.dp)
                                .rotate(rotationAngle),
                            tint = Color(0xFF1C49C0)
                        )
                    }
                }
            }

            // Lista de productos (expandible)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color(0xFFE1E2EC)
                    )

                    // Productos
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items.forEach { item ->
                            ProductCartItem(
                                item = item,
                                onIncrement = {
                                    onIncrementQuantity(item.cartItemId, item.quantity)
                                },
                                onDecrement = {
                                    onDecrementQuantity(item.cartItemId, item.quantity)
                                },
                                onRemove = {
                                    onRemoveProduct(item.cartItemId)
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color(0xFFE1E2EC)
                    )

                    // Resumen de precios
                    PriceSummarySection(cartWithItems)

                    // Botones de acción
                    ActionButtons(
                        cartId = summary.cartId,
                        total = cartWithItems.total,
                        subtotal = summary.subtotal,
                        onClearCart = onClearCart,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceSummarySection(cartWithItems: CartWithItems) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PriceDetailRow(
            label = "Subtotal",
            value = cartWithItems.subtotal,
            isHighlighted = false
        )

        if (cartWithItems.discount > 0) {
            PriceDetailRow(
                label = "Descuento",
                value = -cartWithItems.discount,
                color = Color(0xFF4CAF50)
            )
        }

        if (cartWithItems.deliveryFee > 0) {
            PriceDetailRow(
                label = "Envío",
                value = cartWithItems.deliveryFee,
                isHighlighted = false
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Envío",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF44464F)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Gratis",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = Color(0xFFE1E2EC)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1B1F)
            )
            Text(
                text = "RD$%.2f".format(cartWithItems.total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C49C0)
            )
        }
    }
}

@Composable
private fun ActionButtons(
    cartId: String,
    total: Double,
    subtotal: Double,
    onClearCart: (String) -> Unit,
    navController: NavHostController
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { onClearCart(cartId) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFBA1A1A)
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 1.5.dp
            )
        ) {
            Text(
                text = "Vaciar",
                fontWeight = FontWeight.SemiBold
            )
        }

        Button(
            onClick = {
                navController.navigate(
                    "select_address/${cartId}/${subtotal}"
                )
            },
            modifier = Modifier.weight(2f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1C49C0)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Continuar",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• RD$%.2f".format(total),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProductCartItem(
    item: CartItemDetail,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Imagen del producto
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F7FA)),
            contentAlignment = Alignment.Center
        ) {
            val imageUrl = item.imageUrls?.firstOrNull()
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.productName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = item.productName,
                    modifier = Modifier.size(35.dp),
                    tint = Color(0xFF75777F)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Info del producto
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.productName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1B1F)
            )
            Text(
                text = "RD$%.2f".format(item.price),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C49C0)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Controles
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón eliminar
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Eliminar",
                    tint = Color(0xFF75777F),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Controles de cantidad
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color(0xFFD8E2FF),
                    onClick = onDecrement
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Disminuir",
                            tint = Color(0xFF1C49C0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1B1F),
                    modifier = Modifier.widthIn(min = 24.dp),
                    textAlign = TextAlign.Center
                )

                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color(0xFF1C49C0),
                    onClick = onIncrement
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aumentar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriceDetailRow(
    label: String,
    value: Double,
    color: Color = Color(0xFF1A1B1F),
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF44464F)
        )
        Text(
            text = "${if (value < 0) "-" else ""}RD$%.2f".format(kotlin.math.abs(value)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}