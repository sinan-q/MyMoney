package com.sinxn.mymoney.feature.transfer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.TransactionFormRowItem
import com.sinxn.mymoney.core.ui.components.parseIconData

@Composable
fun EditTransferWallets(
    availableWallets: List<WalletEntity>,
    fromWalletId: String,
    toWalletId: String?,
    targetAmount: String,
    transferFee: String,
    sourceCurrency: String,
    targetCurrency: String,
    accentColor: Color,
    onFromWalletClick: () -> Unit,
    onToWalletClick: () -> Unit,
    onSwapWallets: () -> Unit,
    onTargetAmountChange: (String) -> Unit,
    onTransferFeeChange: (String) -> Unit
) {
    val activeFromWallet = availableWallets.find { it.id == fromWalletId }
    val activeToWallet = availableWallets.find { it.id == toWalletId }

    // From Wallet Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        val activeFromWalletIconData = remember(activeFromWallet?.icon, activeFromWallet?.name) {
            if (activeFromWallet != null) parseIconData(activeFromWallet.icon, activeFromWallet.name) else null
        }
        TransactionFormRowItem(
            icon = Icons.Default.AccountBalanceWallet,
            accentColor = accentColor,
            label = "From Wallet",
            value = activeFromWallet?.name ?: "Select Source Wallet",
            trailingIconData = activeFromWalletIconData,
            onClick = onFromWalletClick
        )
    }

    // Centered Swap Button
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        )
        Surface(
            onClick = onSwapWallets,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Swap Wallets",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // To Wallet Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        val activeToWalletIconData = remember(activeToWallet?.icon, activeToWallet?.name) {
            if (activeToWallet != null) parseIconData(activeToWallet.icon, activeToWallet.name) else null
        }
        TransactionFormRowItem(
            icon = Icons.Default.AccountBalanceWallet,
            accentColor = accentColor,
            label = "To Wallet",
            value = activeToWallet?.name ?: "Select Target Wallet",
            trailingIconData = activeToWalletIconData,
            onClick = onToWalletClick
        )
    }

    // Multi-currency Destination Amount Field
    if (activeFromWallet != null && activeToWallet != null && !activeFromWallet.currency.equals(activeToWallet.currency, ignoreCase = true)) {
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = onTargetAmountChange,
                    label = { Text("Destination Amount ($targetCurrency)") },
                    placeholder = { Text("Received in $targetCurrency") },
                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
    }

    // Transfer Fee / Tax Field
    Spacer(modifier = Modifier.height(10.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = transferFee,
                onValueChange = onTransferFeeChange,
                label = { Text("Transfer Fee / Tax ($sourceCurrency)") },
                placeholder = { Text("0.00") },
                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}
