package com.sinxn.mymoney.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToSqlConsole: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.formattingSettings.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Formatting",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            SettingsSwitchItem(
                title = "Show Currency Symbol",
                checked = settings.showCurrency,
                onCheckedChange = viewModel::updateShowCurrency
            )

            SettingsSwitchItem(
                title = "Group Digits (e.g. 1,000)",
                checked = settings.groupDigits,
                onCheckedChange = viewModel::updateGroupDigits
            )

            SettingsSwitchItem(
                title = "Round Decimals (No cents)",
                checked = settings.roundDecimals,
                onCheckedChange = viewModel::updateRoundDecimals
            )

            SettingsSwitchItem(
                title = "Show +/- Signs",
                checked = settings.showPlusMinus,
                onCheckedChange = viewModel::updateShowPlusMinus
            )

            SettingsSwitchItem(
                title = "Hide Time",
                checked = settings.hideTime,
                onCheckedChange = viewModel::updateHideTime
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Transactions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingsSwitchItem(
                title = "Show Future Transactions",
                checked = settings.includeFutureTransactions,
                onCheckedChange = viewModel::updateIncludeFutureTransactions
            )
            SettingsSwitchItem(
                title = "Exclude Archived from Total",
                checked = settings.excludeArchivedFromTotal,
                onCheckedChange = viewModel::updateExcludeArchivedFromTotal
            )

            SettingsSwitchItem(
                title = "Hide Status & Impact",
                checked = settings.hideStatusAndImpact,
                onCheckedChange = viewModel::updateHideStatusAndImpact
            )
            
            SettingsItem(
                title = "Global Balance Currency",
                value = settings.globalCurrency,
                onValueChange = viewModel::updateGlobalCurrency
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Developer Tools",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSqlConsole() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SQL Console",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Run custom SELECT queries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var textValue by remember(value) { mutableStateOf(value) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        
        if (isEditing) {
            androidx.compose.material3.TextField(
                value = textValue,
                onValueChange = { textValue = it.uppercase() },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { 
                        onValueChange(textValue)
                        isEditing = false 
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        } else {
            TextButton(onClick = { isEditing = true }) {
                Text(text = value)
            }
        }
    }
}
