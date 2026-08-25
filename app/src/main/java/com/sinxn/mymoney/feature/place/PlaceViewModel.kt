package com.sinxn.mymoney.feature.place

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.PlaceRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.SortOption
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlaceSortOption(override val title: String) : SortOption {
    LAST_EDIT("Recently Edited"),
    ALPHABETICAL("Alphabetical (A-Z)")
}

@Immutable
data class PlaceItemUi(
    val place: PlaceEntity,
    val totalAmount: Long = 0L,
    val income: Long = 0L,
    val expense: Long = 0L,
    val transactionCount: Int = 0,
    val currencyCode: String = "USD",
    val decimals: Int = 2,
    val iconData: IconData = parseIconData(place.icon.ifBlank { "ic_place" }, place.name)
)

data class PlaceUiState(
    val places: List<PlaceItemUi> = emptyList(),
    val sortOption: PlaceSortOption = PlaceSortOption.LAST_EDIT,
    val isLoading: Boolean = false,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0
)

@HiltViewModel
class PlaceViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<PlaceUiState> = combine(
        placeRepository.getPlaces(),
        placeRepository.getAllPlaceTransactions(),
        settingsRepository.placesSortOption,
        settingsRepository.formattingSettings
    ) { places, allTransactions, sortOptionName, formattingSettings ->
        val sortOption = try {
            PlaceSortOption.valueOf(sortOptionName)
        } catch (e: Exception) {
            PlaceSortOption.LAST_EDIT
        }

        val txGrouped = allTransactions.groupBy { it.transaction.placeId }

        val placeItems = places.map { place ->
            val txs = txGrouped[place.id] ?: emptyList()
            var total = 0L
            var income = 0L
            var expense = 0L

            txs.forEach { item ->
                if (item.transaction.direction == 1) {
                    total += item.transaction.money
                    income += item.transaction.money
                } else {
                    total -= item.transaction.money
                    expense += item.transaction.money
                }
            }

            val distinctCurrencies = txs.mapNotNull { it.currencySymbol ?: it.currencyCode }.distinct()
            val currencyCode = if (distinctCurrencies.size == 1) distinctCurrencies.first() else formattingSettings.globalCurrency
            val decimals = txs.firstOrNull()?.decimals ?: 2

            PlaceItemUi(
                place = place,
                totalAmount = total,
                income = income,
                expense = expense,
                transactionCount = txs.size,
                currencyCode = currencyCode,
                decimals = decimals,
                iconData = parseIconData(place.icon.ifBlank { "ic_place" }, place.name)
            )
        }

        val sortedPlaces = when (sortOption) {
            PlaceSortOption.LAST_EDIT -> placeItems.sortedByDescending { it.place.lastEdit }
            PlaceSortOption.ALPHABETICAL -> placeItems.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.place.name }
            )
        }

        val formatterConfig = MoneyFormatter.Config(
            showCurrency = formattingSettings.showCurrency,
            groupDigits = formattingSettings.groupDigits,
            roundDecimals = formattingSettings.roundDecimals,
            showPlusMinus = formattingSettings.showPlusMinus
        )

        PlaceUiState(
            places = sortedPlaces,
            sortOption = sortOption,
            isLoading = false,
            formatterConfig = formatterConfig,
            dateFormat = formattingSettings.dateFormat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaceUiState(isLoading = true)
    )

    fun setSortOption(sortOption: PlaceSortOption) {
        viewModelScope.launch {
            settingsRepository.setPlacesSortOption(sortOption.name)
        }
    }
}

