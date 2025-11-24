package com.dev.mandadito.presentation.screens.admin

import com.dev.mandadito.data.models.Notificacion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.dev.mandadito.data.network.AuthRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScaffold(navController: NavController) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }

    val notificaciones = remember {
        mutableStateListOf(
            Notificacion(1, "Nueva actualización", "Hay una nueva versión disponible", "Hace 5 min"),
            Notificacion(2, "Recordatorio", "Tienes una tarea pendiente", "Hace 1 hora"),
            Notificacion(3, "Mensaje recibido", "Juan te ha enviado un mensaje", "Hace 2 horas", true),
            Notificacion(4, "Alerta del sistema", "Mantenimiento programado mañana", "Hace 1 día", true)
        )
    }

    Scaffold(
        topBar = {
            TopBarConNotificaciones(
                notificaciones = notificaciones,
                onMarcarLeida = { id ->
                    val index = notificaciones.indexOfFirst { it.id == id }
                    if (index != -1) {
                        notificaciones[index] = notificaciones[index].copy(leida = true)
                    }
                },
                onLogout = {
                    coroutineScope.launch {
                        authRepository.logout()
                        navController.navigate("welcome") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Inicio"
                            )
                        },
                        label = { Text("Inicio") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = "Usuarios"
                            )
                        },
                        label = { Text("Usuarios") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Store,
                                contentDescription = "Colmados"
                            )
                        },
                        label = { Text("Colmados") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Yo"
                            )
                        },
                        label = { Text("Yo") },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Crossfade para transiciones suaves entre pestañas
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                ),
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    0 -> AdminHomeScreen()
                    1 -> AdminUsersScreen()
                    2 -> AdminColmadoScreen()
                    3 -> AdminProfileScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarConNotificaciones(
    notificaciones: List<Notificacion>,
    onMarcarLeida: (Int) -> Unit,
    onLogout: () -> Unit = {}
) {
    var mostrarModal by remember { mutableStateOf(false) }

    val notificacionesNoLeidas by remember {
        derivedStateOf { notificaciones.count { !it.leida } }
    }

    TopAppBar(
        title = {
            Text(
                text = "Panel de Control",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            Box {
                IconButton(onClick = { mostrarModal = true }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = if (notificacionesNoLeidas > 0)
                            "$notificacionesNoLeidas notificaciones no leídas"
                        else "Sin notificaciones nuevas"
                    )
                }

                // Badge animado con transición suave
                androidx.compose.animation.AnimatedVisibility(
                    visible = notificacionesNoLeidas > 0,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp),
                    enter = scaleIn(
                        initialScale = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = scaleOut(targetScale = 0f)
                ) {
                    Badge {
                        Text(
                            text = notificacionesNoLeidas.toString(),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Cerrar sesión"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )

    // Modal con animación
    if (mostrarModal) {
        ModalNotificaciones(
            notificaciones = notificaciones,
            onDismiss = { mostrarModal = false },
            onMarcarLeida = onMarcarLeida
        )
    }
}

@Composable
fun ModalNotificaciones(
    notificaciones: List<Notificacion>,
    onDismiss: () -> Unit,
    onMarcarLeida: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        // Animación para toda la superficie
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(200)) +
                    scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)) +
                    scaleOut(targetScale = 0.8f, animationSpec = tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notificaciones",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar"
                            )
                        }
                    }

                    HorizontalDivider()

                    if (notificaciones.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay notificaciones",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = notificaciones,
                                key = { it.id }
                            ) { notificacion ->
                                ItemNotificacion(
                                    notificacion = notificacion,
                                    onClick = { onMarcarLeida(notificacion.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemNotificacion(
    notificacion: Notificacion,
    onClick: () -> Unit
) {
    // Variable para controlar la animación del indicador
    val isUnread = !notificacion.leida

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(), // Anima cambios de tamaño
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notificacion.leida)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Indicador no leída con animación
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.Top)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isUnread,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    ) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notificacion.titulo,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = notificacion.mensaje,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = notificacion.tiempo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}