package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dev.mandadito.presentation.components.RatingBar
import com.dev.mandadito.presentation.components.ReviewCard
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.presentation.viewmodels.client.ClientProductDetailViewModel
import io.github.jan.supabase.gotrue.auth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProductDetailScreen(
    productoId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel = remember { ClientProductDetailViewModel(context, productoId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ CORREGIDO: Obtener userId de forma reactiva directamente
    val userId = remember {
        derivedStateOf {
            try {
                SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
            } catch (_: Exception) {
                null
            }
        }
    }.value

    // Estado para mostrar el slider de imágenes
    var showImageSlider by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Recargar reseñas cuando volvamos de las pantallas de reseñas
    LaunchedEffect(Unit) {
        viewModel.refreshReviews(userId)
    }

    // Mostrar mensajes de éxito
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccess()
        }
    }

    // Mostrar mensajes de error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Producto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Volver")
                        }
                    }
                }
            }

            uiState.product != null -> {
                val producto = uiState.product!!
                val imageUrls = producto.allImageUrls.filterNotNull().ifEmpty {
                    listOf(producto.imageUrl).filterNotNull()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Imagen principal clickeable
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrls.getOrNull(selectedImageIndex))
                                .crossfade(300)
                                .build(),
                            contentDescription = producto.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    showImageSlider = true
                                },
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                                        ),
                                        startY = 250f
                                    )
                                )
                        )

                        if (imageUrls.size > 1) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        text = "${selectedImageIndex + 1}/${imageUrls.size}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (producto.stock < 10) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = if (producto.stock == 0)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (producto.stock == 0) Icons.Default.Block else Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (producto.stock == 0)
                                            MaterialTheme.colorScheme.onError
                                        else
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = if (producto.stock == 0) "Agotado" else "¡Solo ${producto.stock}!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (producto.stock == 0)
                                            MaterialTheme.colorScheme.onError
                                        else
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = producto.name,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 32.sp
                        )

                        if (producto.categories.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                producto.categories.take(3).forEach { category ->
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                text = category.icon?.let { "$it ${category.name}" }
                                                    ?: category.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        // ===== SECCIÓN DE RESEÑAS =====
                        if (uiState.reviewStats.totalReviews > 0) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                            "product_name",
                                            producto.name
                                        )
                                        navController.navigate("product_reviews/$productoId")
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        RatingBar(
                                            rating = uiState.reviewStats.averageRating,
                                            starSize = 22.dp,
                                            showRatingNumber = true
                                        )
                                        Text(
                                            text = "${uiState.reviewStats.totalReviews} reseñas",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Ver reseñas",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // ✅ CORREGIDO: Lógica mejorada para el card sin reseñas
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when {
                                            userId == null -> {
                                                // Usuario no autenticado
                                                navController.navigate("login")
                                            }
                                            !uiState.userHasReviewed -> {
                                                // Usuario autenticado y no ha reseñado
                                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                                    "product_name",
                                                    producto.name
                                                )
                                                navController.navigate("add_review/$productoId")
                                            }
                                            // Usuario ya ha reseñado, no hacer nada
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Sin reseñas aún",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Sé el primero en opinar",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }

                        if (uiState.recentReviews.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Reseñas recientes",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(
                                        onClick = {
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                "product_name",
                                                producto.name
                                            )
                                            navController.navigate("product_reviews/$productoId")
                                        }
                                    ) {
                                        Text("Ver todas")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                uiState.recentReviews.take(2).forEach { review ->
                                    ReviewCard(
                                        review = review,
                                        currentUserId = userId
                                    )
                                }
                            }
                        }

                        // Precio
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Precio",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "RD$${String.format(Locale.US, "%.2f", producto.price)}",
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            }
                        }

                        // Descripción
                        producto.description?.let { desc ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Descripción",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        // Cantidad
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Cantidad",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${producto.stock} disponibles",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.decrementQuantity() },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Restar",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    Text(
                                        text = uiState.quantity.toString(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.incrementQuantity() },
                                        enabled = uiState.quantity < producto.stock,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (uiState.quantity < producto.stock)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Sumar",
                                            tint = if (uiState.quantity < producto.stock)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botones
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { viewModel.addToCart() },
                                enabled = producto.stock > 0 && !uiState.isAddingToCart,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 6.dp
                                )
                            ) {
                                if (uiState.isAddingToCart) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            "Agregar al carrito",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { navController.navigate("client_cart") },
                                enabled = producto.stock > 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Ver carrito",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                if (showImageSlider) {
                    ImageSliderDialog(
                        imageUrls = imageUrls,
                        initialPage = selectedImageIndex,
                        productName = producto.name,
                        onDismiss = { showImageSlider = false },
                        onPageChanged = { selectedImageIndex = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSliderDialog(
    imageUrls: List<String>,
    initialPage: Int,
    productName: String,
    onDismiss: () -> Unit,
    onPageChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { imageUrls.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrls[page])
                        .crossfade(300)
                        .build(),
                    contentDescription = "$productName - Imagen ${page + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (imageUrls.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(imageUrls.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = if (index == pagerState.currentPage) 28.dp else 8.dp,
                                        height = 8.dp
                                    )
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (index == pagerState.currentPage)
                                            Color.White
                                        else
                                            Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}