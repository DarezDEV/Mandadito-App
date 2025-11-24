package com.dev.mandadito.presentation.screens.admin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dev.mandadito.R
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.UserProfile

@Composable
fun UserCard(
    usuario: UserProfile,
    isDisabledSection: Boolean,
    onEdit: () -> Unit,
    onDisable: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    val isDisabled = !usuario.activo

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDisabled) 0.7f else 1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabled)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDisabled) 0.dp else 2.dp),
        border = if (isDisabled) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserAvatar(
                usuario = usuario,
                isDisabled = isDisabled
            )

            UserInfo(
                usuario = usuario,
                isDisabled = isDisabled,
                modifier = Modifier.weight(1f)
            )

            UserActions(
                isDisabledSection = isDisabledSection,
                onEdit = onEdit,
                onDisable = onDisable,
                onActivate = onActivate,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun UserAvatar(
    usuario: UserProfile,
    isDisabled: Boolean
) {
    Box {
        if (usuario.avatar_url != null) {
            AsyncImage(
                model = usuario.avatar_url,
                contentDescription = "Foto de ${usuario.nombre}",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isDisabled)
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.profile_default)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.profile_default),
                contentDescription = "Foto de perfil por defecto",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isDisabled)
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop
            )
        }

        // Indicador de estado
        if (isDisabled) {
            Surface(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF9E9E9E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Inactivo",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = Color(0xFF4CAF50),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
            ) {}
        }
    }
}

@Composable
private fun UserInfo(
    usuario: UserProfile,
    isDisabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = usuario.nombre,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDisabled)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Badge de rol
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = getUserRoleColor(usuario.role).copy(alpha = 0.15f)
        ) {
            Text(
                text = getUserRoleLabel(usuario.role),
                style = MaterialTheme.typography.labelMedium,
                color = getUserRoleColor(usuario.role),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = usuario.email,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDisabled)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UserActions(
    isDisabledSection: Boolean,
    onEdit: () -> Unit,
    onDisable: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    if (isDisabledSection) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onActivate,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Activar",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDisable,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = "Deshabilitar",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun getUserRoleColor(role: Role?): Color {
    return when (role) {
        Role.CLIENT -> Color(0xFF2196F3)
        Role.SELLER -> Color(0xFF9C27B0)
        Role.DELIVERY -> Color(0xFFFF9800)
        Role.ADMIN -> Color(0xFFE91E63)
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