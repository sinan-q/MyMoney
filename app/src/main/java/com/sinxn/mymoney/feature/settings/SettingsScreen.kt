package com.sinxn.mymoney.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.formattingSettings.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Date Format could be a dropdown, keeping plain for now or adding later
            // The request focused on boolean toggles mostly from sharedPrefs list
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
