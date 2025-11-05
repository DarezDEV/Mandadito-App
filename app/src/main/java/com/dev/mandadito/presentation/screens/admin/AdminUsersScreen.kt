package com.dev.mandadito.presentation.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.dev.mandadito.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.presentation.viewmodels.admin.AdminUsersViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: AdminUsersViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<UserProfile?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<UserProfile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Gestión de Usuarios",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(16.dp)
        )

        // Mostrar mensajes de éxito/error
        uiState.successMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearSuccess()
            }
        }
        
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                kotlinx.coroutines.delay(5000)
                viewModel.clearError()
            }
        }

        Button(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar",
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nuevo Usuario")
        }

        // Mensajes de estado
        val successMessage = uiState.successMessage
        val errorMessage = uiState.error
        
        if (successMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = successMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        if (showDialog) {
            AddUserDialog(
                onDismiss = { showDialog = false },
                onUserAdded = { email, password, nombre, telefono, role ->
                    viewModel.createUser(email, password, nombre, telefono, role)
                    showDialog = false
                }
            )
        }

        showEditDialog?.let { user ->
            EditUserDialog(
                user = user,
                onDismiss = { showEditDialog = null },
                onUserUpdated = { nombre, telefono ->
                    viewModel.updateUser(user.id, nombre, telefono)
                    showEditDialog = null
                }
            )
        }

        showDeleteConfirm?.let { user ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("Confirmar eliminación") },
                text = { Text("¿Estás seguro de que deseas eliminar permanentemente a ${user.nombre}? Esta acción no se puede deshacer.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteUser(user.id)
                            showDeleteConfirm = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    "Buscar usuario...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        ScrollableTabRow(
            selectedTabIndex = when {
                uiState.showDisabledOnly -> 5
                uiState.selectedRoleFilter == null -> 0
                uiState.selectedRoleFilter == Role.CLIENT -> 1
                uiState.selectedRoleFilter == Role.SELLER -> 2
                uiState.selectedRoleFilter == Role.DELIVERY -> 3
                uiState.selectedRoleFilter == Role.ADMIN -> 4
                else -> 0
            },
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = !uiState.showDisabledOnly && uiState.selectedRoleFilter == null,
                onClick = {
                    viewModel.setShowDisabledOnly(false)
                    viewModel.setRoleFilter(null)
                },
                text = {
                    Text(
                        "Todos",
                        color = if (!uiState.showDisabledOnly && uiState.selectedRoleFilter == null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Tab(
                selected = !uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.CLIENT,
                onClick = {
                    viewModel.setShowDisabledOnly(false)
                    viewModel.setRoleFilter(Role.CLIENT)
                },
                text = {
                    Text(
                        "Clientes",
                        color = if (!uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.CLIENT)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Tab(
                selected = !uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.SELLER,
                onClick = {
                    viewModel.setShowDisabledOnly(false)
                    viewModel.setRoleFilter(Role.SELLER)
                },
                text = {
                    Text(
                        "Colmados",
                        color = if (!uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.SELLER)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Tab(
                selected = !uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.DELIVERY,
                onClick = {
                    viewModel.setShowDisabledOnly(false)
                    viewModel.setRoleFilter(Role.DELIVERY)
                },
                text = {
                    Text(
                        "Deliveries",
                        color = if (!uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.DELIVERY)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Tab(
                selected = !uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.ADMIN,
                onClick = {
                    viewModel.setShowDisabledOnly(false)
                    viewModel.setRoleFilter(Role.ADMIN)
                },
                text = {
                    Text(
                        "Admin",
                        color = if (!uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.ADMIN)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Tab(
                selected = uiState.showDisabledOnly,
                onClick = {
                    viewModel.setShowDisabledOnly(true)
                },
                text = {
                    Text(
                        "Deshabilitados",
                        color = if (uiState.showDisabledOnly)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.filteredUsers.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No se encontraron usuarios",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            if (uiState.isLoading && uiState.filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.filteredUsers,
                        key = { it.id }
                    ) { usuario ->
                        UserCard(
                            usuario = usuario,
                            isDisabledSection = uiState.showDisabledOnly,
                            onEdit = { showEditDialog = usuario },
                            onDisable = {
                                viewModel.disableUser(usuario.id)
                            },
                            onActivate = {
                                viewModel.enableUser(usuario.id)
                            },
                            onDelete = {
                                showDeleteConfirm = usuario
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(
    usuario: UserProfile,
    isDisabledSection: Boolean,
    onEdit: () -> Unit,
    onDisable: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabledSection)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box {
                if (usuario.fotoUrl != null) {
                    AsyncImage(
                        model = usuario.fotoUrl,
                        contentDescription = "Foto de ${usuario.nombre}",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                if (isDisabledSection)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary,
                                CircleShape
                            ),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.profile_default)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.profile_default),
                        contentDescription = "Foto de perfil por defecto",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                if (isDisabledSection)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary,
                                CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                }

                if (isDisabledSection) {
                    Surface(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Deshabilitado",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier
                                .padding(2.dp)
                                .size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = usuario.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = getUserRoleColor(usuario.role),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = getUserRoleLabel(usuario.role),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = usuario.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isDisabledSection) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onActivate,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Activar",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDisable,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Deshabilitar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onUserAdded: (String, String, String, String?, Role) -> Unit
) {
    val context = LocalContext.current

    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(Role.CLIENT) }
    var expanded by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImageOptions by remember { mutableStateOf(false) }

    // Excluir DELIVERY ya que solo los vendedores pueden crear deliveries
    val roles = listOf(
        Role.CLIENT to "Cliente",
        Role.SELLER to "Colmado",
        Role.ADMIN to "Administrador"
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            imageUri = cameraUri
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Foto de perfil mejorada
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                            .padding(4.dp)
                            .clickable { showImageOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.profile_default),
                                    contentDescription = "Foto de perfil por defecto",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Cambiar foto",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Toca para cambiar la foto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Diálogo de opciones mejorado
                if (showImageOptions) {
                    AlertDialog(
                        onDismissRequest = { showImageOptions = false },
                        title = {
                            Text(
                                "Seleccionar foto",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            galleryLauncher.launch("image/*")
                                            showImageOptions = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                "Seleccionar de la galería",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "Elegir una foto existente",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val photoFile = java.io.File(
                                                context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
                                                "profile_${System.currentTimeMillis()}.jpg"
                                            )
                                            cameraUri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                photoFile
                                            )
                                            cameraLauncher.launch(cameraUri!!)
                                            showImageOptions = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondary,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                "Tomar una foto",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "Usar la cámara",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { showImageOptions = false },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Cancelar")
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Campo Nombre con diseño premium
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = {
                        Text(
                            "Nombre Completo",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Campo Email con diseño premium
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = {
                        Text(
                            "Correo Electrónico",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Campo Contraseña con diseño premium
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(
                            "Contraseña",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar contraseña"
                                else "Mostrar contraseña",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Campo Role con diseño premium
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = roles.find { it.first == selectedRole }?.second ?: "Cliente",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                "Rol del Usuario",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingIcon = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { (role, displayName) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        displayName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                onClick = {
                                    selectedRole = role
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Botones con diseño mejorado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            "Cancelar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            if (nombre.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                                onUserAdded(email, password, nombre, null, selectedRole)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = nombre.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Guardar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onUserUpdated: (String, String?) -> Unit
) {
    var nombre by remember { mutableStateOf(user.nombre) }
    var telefono by remember { mutableStateOf(user.telefono ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Editar Usuario",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre Completo") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

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
                            onUserUpdated(
                                nombre,
                                telefono.ifBlank { null }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = nombre.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
private fun getUserRoleColor(role: Role?): Color {
    return when (role) {
        Role.CLIENT -> MaterialTheme.colorScheme.secondary
        Role.SELLER -> MaterialTheme.colorScheme.tertiary
        Role.DELIVERY -> Color(0xFFFF9800)
        Role.ADMIN -> MaterialTheme.colorScheme.primary
        null -> MaterialTheme.colorScheme.outline
    }
}

private fun getUserRoleLabel(role: Role?): String {
    return when (role) {
        Role.CLIENT -> "Cliente"
        Role.SELLER -> "Colmado"
        Role.DELIVERY -> "Delivery"
        Role.ADMIN -> "Admin"
        null -> "Sin rol"
    }
}