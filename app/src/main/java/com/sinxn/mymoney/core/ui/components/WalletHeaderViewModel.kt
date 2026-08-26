package com.sinxn.mymoney.core.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class WalletHeaderViewModel @Inject constructor(
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val allWallets: StateFlow<List<WalletWithBalance>> = moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(Date()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentWalletId: StateFlow<String> = settingsRepository.currentWalletId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Constants.TOTAL_WALLET_ID
        )

    val activeWallets: StateFlow<List<WalletWithBalance>> = allWallets
        .map { list -> list.filter { !it.wallet.isArchived } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val archivedWallets: StateFlow<List<WalletWithBalance>> = allWallets
        .map { list -> list.filter { it.wallet.isArchived } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalWallet: StateFlow<WalletWithBalance?> = combine(
        allWallets,
        settingsRepository.formattingSettings
    ) { wallets, settings ->
        createTotalWallet(wallets, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = createDefaultTotalWallet()
    )

    val currentWallet: StateFlow<WalletWithBalance?> = combine(
        allWallets,
        currentWalletId,
        settingsRepository.formattingSettings
    ) { wallets, id, settings ->
        if (id == Constants.TOTAL_WALLET_ID) {
            createTotalWallet(wallets, settings)
        } else {
            wallets.find { it.wallet.id == id } ?: wallets.firstOrNull()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = createDefaultTotalWallet()
    )

    val formatterConfig: StateFlow<MoneyFormatter.Config> = settingsRepository.formattingSettings
        .map { settings ->
            MoneyFormatter.Config(
                showCurrency = settings.showCurrency,
                groupDigits = settings.groupDigits,
                roundDecimals = settings.roundDecimals,
                showPlusMinus = settings.showPlusMinus
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MoneyFormatter.Config()
        )

    private fun createTotalWallet(
        wallets: List<WalletWithBalance>,
        settings: com.sinxn.mymoney.core.data.preferences.FormattingSettings
    ): WalletWithBalance {
        val walletsInTotal = wallets.filter {
            it.wallet.countInTotal && (!settings.excludeArchivedFromTotal || !it.wallet.isArchived)
        }
        val totalBalance = walletsInTotal.sumOf { it.currentBalance }
        val distinctCurrencies = walletsInTotal
            .map { it.wallet.currency.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val isTotalValid = distinctCurrencies.size <= 1
        val effectiveCurrency = if (distinctCurrencies.size == 1) {
            distinctCurrencies.first()
        } else {
            settings.globalCurrency.ifEmpty { "USD" }
        }
        val currency = try {
            java.util.Currency.getInstance(effectiveCurrency)
        } catch (e: Exception) {
            null
        }
        val decimals = if (distinctCurrencies.size == 1) {
            walletsInTotal.firstOrNull()?.decimals ?: (currency?.defaultFractionDigits ?: 2)
        } else {
            currency?.defaultFractionDigits ?: 2
        }
        val currencySymbol = if (distinctCurrencies.size == 1) {
            walletsInTotal.firstOrNull()?.currencySymbol ?: (currency?.symbol ?: effectiveCurrency)
        } else {
            currency?.symbol ?: effectiveCurrency
        }

        val breakdown = if (!isTotalValid && walletsInTotal.isNotEmpty()) {
            walletsInTotal
                .groupBy { it.wallet.currency }
                .map { (currency, group) ->
                    val sum = group.sumOf { it.currentBalance }
                    val groupDecimals = group.firstOrNull()?.decimals ?: 2
                    MoneyFormatter.format(
                        amount = sum,
                        currencyCode = currency,
                        decimals = groupDecimals
                    )
                }.joinToString(", ")
        } else null

        return WalletWithBalance(
            wallet = com.sinxn.mymoney.core.data.local.entity.WalletEntity(
                id = Constants.TOTAL_WALLET_ID,
                name = "Total",
                icon = "sigma",
                currency = effectiveCurrency,
                startMoney = 0,
                isArchived = false,
                note = null,
                countInTotal = false,
                index = -1,
                isDeleted = false,
                lastEdit = 0,
                tag = null
            ),
            currentBalance = totalBalance,
            decimals = decimals,
            currencySymbol = currencySymbol,
            isTotalValid = isTotalValid,
            balanceBreakdown = breakdown
        )
    }

    private fun createDefaultTotalWallet(): WalletWithBalance {
        return WalletWithBalance(
            wallet = com.sinxn.mymoney.core.data.local.entity.WalletEntity(
                id = Constants.TOTAL_WALLET_ID,
                name = "Total",
                icon = "sigma",
                currency = "USD",
                startMoney = 0,
                isArchived = false,
                note = null,
                countInTotal = false,
                index = -1,
                isDeleted = false,
                lastEdit = 0,
                tag = null
            ),
            currentBalance = 0L,
            decimals = 2,
            currencySymbol = "$",
            isTotalValid = true,
            balanceBreakdown = null
        )
    }
}
