package com.dev.mandadito.presentation.screens.seller

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.data.repository.UserRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val repository = remember { UserRepository() }

    var users by remember { mutableStateOf(emptyList<UserProfile>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterRole by remember { mutableStateOf<String?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<UserProfile?>(null) }
    var userToDelete by remember { mutableStateOf<UserProfile?>(null) }
    var selectedUser by remember { mutableStateOf<UserProfile?>(null) }

    // Cargar usuarios
    LaunchedEffect(Unit) {
        isLoading = true
        repository.getAllUsers().onSuccess {
            users = it
            isLoading = false
        }
    }

    // Filtrar usuarios
    val filteredUsers = remember(users, searchQuery, filterRole) {
        users.filter { user ->
            val matchesSearch = searchQuery.isEmpty() ||
                    user.nombre.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true)
            val matchesRole = filterRole == null || user.role?.name?.lowercase() == filterRole
            matchesSearch && matchesRole
        }
    }

    val roles = listOf("client", "delivery_user", "seller", "admin")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gestión de Usuarios")
                        Text(
                            text = "${users.size} usuarios registrados",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Filtro por rol
                    IconButton(onClick = { showFilterMenu = true }) {
                        Badge(
                            containerColor = if (filterRole != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(Icons.Default.FilterList, "Filtrar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Estadísticas rápidas
            UserStatsCard(users = users)

            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar usuarios...") },
                leadingIcon = { Icon(Icons.Default.Search, "Buscar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Chips de filtro
            if (filterRole != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { filterRole = null },
                        label = { Text(getRoleName(filterRole!!)) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            // Lista de usuarios
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No se encontraron usuarios",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredUsers) { user ->
                        UserManagementCard(
                            user = user,
                            onViewDetails = { selectedUser = user },
                            onBlock = { userToBlock = user },
                            onDelete = { userToDelete = user }
                        )
                    }
                }
            }
        }
    }

    // Menú de filtros
    if (showFilterMenu) {
        AlertDialog(
            onDismissRequest = { showFilterMenu = false },
            icon = { Icon(Icons.Default.FilterList, null) },
            title = { Text("Filtrar por Rol") },
            text = {
                Column {
                    roles.forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = filterRole == role,
                                onClick = {
                                    filterRole = role
                                    showFilterMenu = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getRoleName(role))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        filterRole = null
                        showFilterMenu = false
                    }
                ) {
                    Text("Limpiar filtro")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFilterMenu = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de detalles de usuario
    selectedUser?.let { user ->
        UserDetailsDialog(
            user = user,
            onDismiss = { selectedUser = null }
        )
    }

    // Diálogo de bloqueo/desbloqueo
    userToBlock?.let { user ->
        val isBlocked = !user.activo
        AlertDialog(
            onDismissRequest = { userToBlock = null },
            icon = { Icon(Icons.Default.Block, null) },
            title = { Text("${if (isBlocked) "Desbloquear" else "Bloquear"} Usuario") },
            text = {
                Text("¿Estás seguro de ${if (isBlocked) "desbloquear" else "bloquear"} a ${user.nombre}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.updateUser(
                                user.id,
                                user.copy(activo = !isBlocked) // Invierte el estado
                            ).onSuccess {
                                users = users.map {
                                    if (it.id == user.id) it.copy(activo = !isBlocked) else it
                                }
                                userToBlock = null
                            }
                        }
                    }
                ) {
                    Text(if (isBlocked) "Desbloquear" else "Bloquear")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToBlock = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de eliminación
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Eliminar Usuario") },
            text = {
                Text("¿Estás seguro de eliminar a ${user.nombre}? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteUser(user.id).onSuccess {
                                users = users.filter { it.id != user.id }
                                userToDelete = null
                            }
                        }
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun UserStatsCard(users: List<UserProfile>) {
    val clientCount = users.count { it.role?.name?.lowercase() == "client" }
    val deliveryCount = users.count { it.role?.name?.lowercase() == "delivery_user" }
    val sellerCount = users.count { it.role?.name?.lowercase() == "seller" }
    val blockedCount = users.count { !it.activo }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            UserStatItem(
                label = "Clientes",
                value = clientCount.toString(),
                icon = Icons.Default.Person,
                color = Color(0xFF2196F3)
            )
            UserStatItem(
                label = "Repartidores",
                value = deliveryCount.toString(),
                icon = Icons.Default.DeliveryDining,
                color = Color(0xFF4CAF50)
            )
            UserStatItem(
                label = "Vendedores",
                value = sellerCount.toString(),
                icon = Icons.Default.Store,
                color = Color(0xFFFF9800)
            )
            UserStatItem(
                label = "Bloqueados",
                value = blockedCount.toString(),
                icon = Icons.Default.Block,
                color = Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun UserStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun UserManagementCard(
    user: UserProfile,
    onViewDetails: () -> Unit,
    onBlock: () -> Unit,
    onDelete: () -> Unit
) {
    val isBlocked = !user.activo

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier.size(60.dp)
                ) {
                    if (!user.avatar_url.isNullOrEmpty()) {
                        AsyncImage(
                            model = user.avatar_url,
                            contentDescription = user.nombre,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.nombre.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Indicador de bloqueado
                    if (isBlocked) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = "Bloqueado",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }

                // Información
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )

                        RoleBadge(role = user.role?.name?.lowercase() ?: "client")
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Botones de acción
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Detalles")
                }

                TextButton(
                    onClick = onBlock,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBlocked) "Desbloquear" else "Bloquear")
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}

@Composable
fun RoleBadge(role: String) {
    val (color, text) = when (role) {
        "client" -> Color(0xFF2196F3) to "Cliente"
        "delivery_user" -> Color(0xFF4CAF50) to "Repartidor"
        "seller" -> Color(0xFFFF9800) to "Vendedor"
        "admin" -> Color(0xFF9C27B0) to "Admin"
        else -> Color(0xFF9E9E9E) to role
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun UserDetailsDialog(
    user: UserProfile,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Person, null) },
        title = { Text("Detalles del Usuario") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserDetailRow("Nombre", user.nombre)
                UserDetailRow("Email", user.email)
                UserDetailRow("Rol", getRoleName(user.role?.name?.lowercase() ?: "client"))
                UserDetailRow("Estado", if (!user.activo) "Bloqueado" else "Activo")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun UserDetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun getRoleName(role: String): String = when (role) {
    "client" -> "Cliente"
    "delivery_user" -> "Repartidor"
    "seller" -> "Vendedor"
    "admin" -> "Administrador"
    else -> role
}