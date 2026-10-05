package com.sinxn.mymoney.feature.template

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.AppDestructiveConfirmDialog
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.util.MoneyFormatter
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailsScreen(
    onNavigateBack: () -> Unit,
    onEditTemplateClick: (String, Boolean) -> Unit,
    onUseInTransactionClick: (String, Boolean) -> Unit,
    viewModel: TemplateDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                TemplateDetailsEvent.Deleted -> onNavigateBack()
                is TemplateDetailsEvent.Applied -> {
                    Toast.makeText(context, "Template applied successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showDeleteDialog) {
        AppDestructiveConfirmDialog(
            title = "Delete Template",
            message = "Are you sure you want to delete this template?",
            onConfirmDelete = {
                showDeleteDialog = false
                viewModel.deleteTemplate()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Template Details",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditTemplateClick(uiState.templateId, uiState.isTransfer) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Template"
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Template",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (!uiState.isTransfer && uiState.transactionModel == null) {
                Text(
                    text = "Template not found",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else if (uiState.isTransfer && uiState.transferModel == null) {
                Text(
                    text = "Template not found",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                val tx = uiState.transactionModel
                val tr = uiState.transferModel

                val isExpense = tx?.model?.direction == 0
                val isIncome = tx?.model?.direction == 1
                val isTransfer = uiState.isTransfer

                val title = if (isTransfer) {
                    tr?.model?.description?.takeIf { it.isNotBlank() } ?: "Transfer Template"
                } else {
                    tx?.model?.description?.takeIf { it.isNotBlank() } ?: tx?.categoryName ?: "Template"
                }

                val typeLabel = if (isTransfer) {
                    "Transfer"
                } else if (isIncome) {
                    "Income"
                } else {
                    "Expense"
                }

                val bannerColor = if (isTransfer) {
                    MaterialTheme.colorScheme.primaryContainer
                } else if (isIncome) {
                    Color(0xFFE8F5E9)
                } else {
                    Color(0xFFFFEBEE)
                }

                val amountColor = if (isTransfer) {
                    MaterialTheme.colorScheme.primary
                } else if (isIncome) {
                    Color(0xFF2E7D32)
                } else {
                    Color(0xFFC62828)
                }

                val formattedAmount = if (isTransfer) {
                    val fromStr = MoneyFormatter.format(tr?.model?.moneyFrom ?: 0L, tr?.walletFromCurrency ?: "USD", tr?.walletFromDecimals ?: 2)
                    val toStr = MoneyFormatter.format(tr?.model?.moneyTo ?: 0L, tr?.walletToCurrency ?: "USD", tr?.walletToDecimals ?: 2)
                    if (tr?.walletFromCurrency == tr?.walletToCurrency) fromStr else "$fromStr -> $toStr"
                } else {
                    MoneyFormatter.format(tx?.model?.money ?: 0L, tx?.walletCurrency ?: "USD", tx?.walletDecimals ?: 2)
                }

                val placeName = if (isTransfer) tr?.placeName else tx?.placeName
                val eventName = if (isTransfer) tr?.eventName else tx?.eventName
                val note = if (isTransfer) tr?.model?.note else tx?.model?.note
                val tag = if (isTransfer) tr?.model?.tag else tx?.model?.tag
                val confirmed = if (isTransfer) tr?.model?.confirmed ?: true else tx?.model?.confirmed ?: true
                val countInTotal = if (isTransfer) tr?.model?.countInTotal ?: true else tx?.model?.countInTotal ?: true

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Banner Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = bannerColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isTransfer) {
                                            Icon(
                                                imageVector = Icons.Default.SwapHoriz,
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            CategoryIcon(
                                                iconString = tx?.categoryIcon ?: "ic_category_other",
                                                categoryName = tx?.categoryName ?: "Category",
                                                modifier = Modifier.size(44.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = formattedAmount,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Black,
                                            color = amountColor
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                            ) {
                                                Text(
                                                    text = typeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            if (!tag.isNullOrBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                                ) {
                                                    Text(
                                                        text = tag,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = viewModel::applyTemplate,
                                enabled = !uiState.isApplying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isApplying) "Applying..." else "Apply Template Now",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { onUseInTransactionClick(uiState.templateId, uiState.isTransfer) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(if (uiState.isTransfer) Icons.Default.SwapHoriz else Icons.Default.EditNote, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isTransfer) "Create Transfer from Template" else "Create Transaction from Template",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Details Breakdown Card
                    item {
                        Text(
                            text = "Template Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        FormCardContainer(horizontalPadding = 16.dp) {
                            Column {
                                if (!isTransfer) {
                                    CleanListRow(
                                        icon = {
                                            CategoryIcon(
                                                iconString = tx?.categoryIcon ?: "ic_category_other",
                                                categoryName = tx?.categoryName ?: "Category",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = "Category",
                                        value = tx?.categoryName ?: "Unassigned"
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    CleanListRow(
                                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        label = "Wallet",
                                        value = tx?.walletName ?: "Unassigned"
                                    )
                                } else {
                                    CleanListRow(
                                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        label = "From Wallet",
                                        value = tr?.walletFromName ?: "Unassigned"
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    CleanListRow(
                                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                        label = "To Wallet",
                                        value = tr?.walletToName ?: "Unassigned"
                                    )

                                    if (tr?.model?.moneyTax != null && tr.model.moneyTax > 0) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )

                                        CleanListRow(
                                            icon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            label = "Tax Amount",
                                            value = MoneyFormatter.format(tr.model.moneyTax, tr.walletFromCurrency, tr.walletFromDecimals)
                                        )
                                    }
                                }

                                if (!placeName.isNullOrBlank()) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    CleanListRow(
                                        icon = { Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        label = "Place",
                                        value = placeName
                                    )
                                }

                                if (!eventName.isNullOrBlank()) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    CleanListRow(
                                        icon = { Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        label = "Event",
                                        value = eventName
                                    )
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                CleanListRow(
                                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    label = "Confirmed",
                                    value = if (confirmed) "Yes" else "No"
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                CleanListRow(
                                    icon = { Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    label = "Count in Total",
                                    value = if (countInTotal) "Yes" else "No"
                                )

                                if (!note.isNullOrBlank()) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    CleanListRow(
                                        icon = { Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        label = "Note",
                                        value = note
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
