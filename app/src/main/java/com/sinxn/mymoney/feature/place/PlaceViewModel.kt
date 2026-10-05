package com.sinxn.mymoney.feature.place

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.preferences.toFormatterConfig
import com.sinxn.mymoney.core.data.repository.PlaceRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.SortOption
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    val id: String,
    val name: String,
    val subtitle: String,
    val tag: String? = null,
    val address: String? = null,
    val iconData: IconData,
    val amountText: String = "",
    val totalAmount: Long = 0L,
    val isPositive: Boolean = false,
    val isNegative: Boolean = false,
    val transactionCount: Int = 0,
    val lastEdit: Long = 0L
)

data class PlaceUiState(
    val places: List<PlaceItemUi> = emptyList(),
    val totalPlacesCount: Int = 0,
    val searchQuery: String = "",
    val sortOption: PlaceSortOption = PlaceSortOption.LAST_EDIT,
    val isLoading: Boolean = false
)

@HiltViewModel
class PlaceViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<PlaceUiState> = combine(
        placeRepository.getPlaces(),
        placeRepository.getAllPlaceTransactions(),
        settingsRepository.placesSortOption,
        settingsRepository.formattingSettings,
        _searchQuery
    ) { places, allTransactions, sortOptionName, formattingSettings, searchQuery ->
        val sortOption = try {
            PlaceSortOption.valueOf(sortOptionName)
        } catch (e: Exception) {
            PlaceSortOption.LAST_EDIT
        }

        val txGrouped = allTransactions.groupBy { it.transaction.placeId }

        val formatterConfig = formattingSettings.toFormatterConfig()

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

            val subtitle = when {
                !place.address.isNullOrBlank() && !place.tag.isNullOrBlank() -> "${place.address} • ${place.tag}"
                !place.address.isNullOrBlank() -> place.address
                !place.tag.isNullOrBlank() -> place.tag
                place.latitude != null && place.longitude != null -> String.format("%.4f, %.4f", place.latitude, place.longitude)
                else -> "No address specified"
            }

            val formattedMoney = MoneyFormatter.format(
                amount = total,
                currencyCode = currencyCode,
                decimals = decimals,
                config = formatterConfig
            )
            val isPositive = total > 0
            val isNegative = total < 0
            val amountText = (if (isPositive && !formattedMoney.startsWith("+")) "+" else "") + formattedMoney

            PlaceItemUi(
                id = place.id,
                name = place.name,
                subtitle = subtitle,
                tag = place.tag,
                address = place.address,
                iconData = parseIconData(place.icon.ifBlank { "ic_place" }, place.name),
                amountText = amountText,
                totalAmount = total,
                isPositive = isPositive,
                isNegative = isNegative,
                transactionCount = txs.size,
                lastEdit = place.lastEdit
            )
        }

        val sortedPlaces = when (sortOption) {
            PlaceSortOption.LAST_EDIT -> placeItems.sortedByDescending { it.lastEdit }
            PlaceSortOption.ALPHABETICAL -> placeItems.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
        }

        val filteredPlaces = if (searchQuery.isBlank()) {
            sortedPlaces
        } else {
            sortedPlaces.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                (!item.address.isNullOrBlank() && item.address.contains(searchQuery, ignoreCase = true)) ||
                (!item.tag.isNullOrBlank() && item.tag.contains(searchQuery, ignoreCase = true))
            }
        }

        PlaceUiState(
            places = filteredPlaces,
            totalPlacesCount = sortedPlaces.size,
            searchQuery = searchQuery,
            sortOption = sortOption,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaceUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sortOption: PlaceSortOption) {
        viewModelScope.launch {
            settingsRepository.setPlacesSortOption(sortOption.name)
        }
    }
}

