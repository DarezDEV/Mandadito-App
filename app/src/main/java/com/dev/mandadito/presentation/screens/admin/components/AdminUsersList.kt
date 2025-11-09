package com.dev.mandadito.presentation.screens.admin.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.presentation.screens.admin.components.SkeletonUserCard
import com.dev.mandadito.presentation.screens.admin.components.UserCard

@Composable
fun AdminUsersList(
    filteredUsers: List<UserProfile>,
    isLoading: Boolean,
    isInternetError: Boolean,
    showInternetAlert: Boolean,
    showDisabledOnly: Boolean,
    onEdit: (UserProfile) -> Unit,
    onDisable: (UserProfile) -> Unit,
    onActivate: (UserProfile) -> Unit,
    onDelete: (UserProfile) -> Unit
) {
    val showSkeleton = isLoading || (isInternetError && !showInternetAlert)

    when {
        showSkeleton && filteredUsers.isEmpty() -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) {
                    SkeletonUserCard()
                }
            }
        }
        filteredUsers.isEmpty() -> {
            EmptyUsersState()
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredUsers,
                    key = { it.id }
                ) { usuario ->
                    UserCard(
                        usuario = usuario,
                        isDisabledSection = showDisabledOnly,
                        onEdit = { onEdit(usuario) },
                        onDisable = { onDisable(usuario) },
                        onActivate = { onActivate(usuario) },
                        onDelete = { onDelete(usuario) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyUsersState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "No se encontraron usuarios",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Intenta con otros filtros",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
