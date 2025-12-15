package com.dev.mandadito.presentation.screens.admin

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.dev.mandadito.data.models.Notificacion
import com.dev.mandadito.data.repository.AuthRepository
import com.dev.mandadito.presentation.viewmodels.admin.AdminColmadosViewModel
import com.dev.mandadito.presentation.viewmodels.admin.AdminUsersViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScaffold(navController: NavController) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val adminUsersViewModel = remember(context) { AdminUsersViewModel(context) }
    val adminColmadosViewModel = remember(context) { AdminColmadosViewModel(context) }

    val notificaciones = remember {
        mutableStateListOf(
            Notificacion(1, "Nueva actualización", "Hay una nueva versión disponible", "Hace 5 min"),
            Notificacion(2, "Recordatorio", "Tienes una tarea pendiente", "Hace 1 hora"),
            Notificacion(3, "Mensaje recibido", "Juan te ha enviado un mensaje", "Hace 2 horas", true),
            Notificacion(4, "Alerta del sistema", "Mantenimiento programado mañana", "Hace 1 día", true)
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(24.dp)
                ) {
                    Column {
                        Text("Mandadito", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        Text("Panel de Administración", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary.copy(0.8f))
                    }
                }

                Spacer(Modifier.height(16.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Inicio") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        coroutineScope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Group, null) },
                    label = { Text("Usuarios") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        coroutineScope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Store, null) },
                    label = { Text("Colmados") },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        coroutineScope.launch { drawerState.close() }
                    }
                )

                Spacer(Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            authRepository.logout()
                            navController.navigate("welcome") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopBarConNotificaciones(
                    notificaciones = notificaciones,
                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                    onMarcarLeida = { id ->
                        val i = notificaciones.indexOfFirst { it.id == id }
                        if (i != -1) notificaciones[i] = notificaciones[i].copy(leida = true)
                    }
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(300),
                    label = "tabs"
                ) {
                    when (it) {
                        0 -> AdminHomeScreen()
                        1 -> AdminUsersScreen(adminUsersViewModel)
                        2 -> AdminColmadoScreen(adminColmadosViewModel)
                        3 -> AdminProfileScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarConNotificaciones(
    notificaciones: List<Notificacion>,
    onMenuClick: () -> Unit,
    onMarcarLeida: (Int) -> Unit
) {
    var mostrarModal by remember { mutableStateOf(false) }
    val noLeidas by remember { derivedStateOf { notificaciones.count { !it.leida } } }

    TopAppBar(
        title = { Text("Panel de Control", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, null)
            }
        },
        actions = {
            Box {
                IconButton(onClick = { mostrarModal = true }) {
                    Icon(Icons.Default.Notifications, null)
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = noLeidas > 0,
                    modifier = Modifier.align(Alignment.TopEnd),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Badge { Text(noLeidas.toString()) }
                }
            }
        }
    )

    if (mostrarModal) {
        ModalNotificaciones(notificaciones, { mostrarModal = false }, onMarcarLeida)
    }
}

@Composable
fun ModalNotificaciones(
    notificaciones: List<Notificacion>,
    onDismiss: () -> Unit,
    onMarcarLeida: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f),
                shape = RoundedCornerShape(16.dp)
            ) {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(notificaciones, key = { it.id }) {
                        ItemNotificacion(it) { onMarcarLeida(it.id) }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemNotificacion(notificacion: Notificacion, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !notificacion.leida,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(Modifier.width(8.dp))

            Column {
                Text(notificacion.titulo, fontWeight = FontWeight.Bold)
                Text(notificacion.mensaje)
                Text(notificacion.tiempo, fontSize = 12.sp)
            }
        }
    }
}
