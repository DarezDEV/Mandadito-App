package com.dev.mandadito.presentation.screens.admin.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.presentation.viewmodels.admin.AdminUsersUiState

@Composable
fun AdminUsersTabs(
    uiState: AdminUsersUiState,
    onShowDisabledChange: (Boolean) -> Unit,
    onRoleFilterChange: (Role?) -> Unit
) {
    val selectedTabIndex = when {
        uiState.showDisabledOnly -> 5
        uiState.selectedRoleFilter == null -> 0
        uiState.selectedRoleFilter == Role.CLIENT -> 1
        uiState.selectedRoleFilter == Role.SELLER -> 2
        uiState.selectedRoleFilter == Role.DELIVERY -> 3
        uiState.selectedRoleFilter == Role.ADMIN -> 4
        else -> 0
    }

    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier.fillMaxWidth(),
        edgePadding = 20.dp,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty()) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    height = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        UserTab(
            text = "Todos",
            selected = !uiState.showDisabledOnly && uiState.selectedRoleFilter == null,
            onClick = {
                onShowDisabledChange(false)
                onRoleFilterChange(null)
            }
        )
        UserTab(
            text = "Clientes",
            selected = !uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.CLIENT,
            onClick = {
                onShowDisabledChange(false)
                onRoleFilterChange(Role.CLIENT)
            }
        )
        UserTab(
            text = "Colmados",
            selected = !uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.SELLER,
            onClick = {
                onShowDisabledChange(false)
                onRoleFilterChange(Role.SELLER)
            }
        )
        UserTab(
            text = "Deliveries",
            selected = !uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.DELIVERY,
            onClick = {
                onShowDisabledChange(false)
                onRoleFilterChange(Role.DELIVERY)
            }
        )
        UserTab(
            text = "Admins",
            selected = !uiState.showDisabledOnly && uiState.selectedRoleFilter == Role.ADMIN,
            onClick = {
                onShowDisabledChange(false)
                onRoleFilterChange(Role.ADMIN)
            }
        )
        UserTab(
            text = "Inactivos",
            selected = uiState.showDisabledOnly,
            onClick = {
                onShowDisabledChange(true)
                onRoleFilterChange(null)
            }
        )
    }
}

@Composable
private fun UserTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    )
}