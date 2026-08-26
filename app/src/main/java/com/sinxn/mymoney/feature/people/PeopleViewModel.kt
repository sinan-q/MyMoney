package com.sinxn.mymoney.feature.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.PersonRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.SortOption
import com.sinxn.mymoney.core.ui.components.parseIconData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PersonSortOption(override val title: String) : SortOption {
    LAST_USED("Recently Used"),
    LAST_EDIT("Recently Edited"),
    ALPHABETICAL("Alphabetical (A-Z)")
}

data class PersonItemUi(
    val id: String,
    val name: String,
    val note: String? = null,
    val tag: String? = null,
    val iconData: IconData,
    val lastUsed: Long = 0L,
    val lastEdit: Long = 0L
)

data class PeopleUiState(
    val people: List<PersonItemUi> = emptyList(),
    val totalPeopleCount: Int = 0,
    val searchQuery: String = "",
    val sortOption: PersonSortOption = PersonSortOption.LAST_USED,
    val isLoading: Boolean = false
)

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val personRepository: PersonRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<PeopleUiState> = combine(
        personRepository.getPeople(),
        settingsRepository.peopleSortOption,
        _searchQuery
    ) { people, sortOptionName, searchQuery ->
        val sortOption = try {
            PersonSortOption.valueOf(sortOptionName)
        } catch (e: Exception) {
            PersonSortOption.LAST_USED
        }

        val personItems = people.map { person ->
            PersonItemUi(
                id = person.id,
                name = person.name,
                note = person.note,
                tag = person.tag,
                iconData = parseIconData(person.icon, person.name),
                lastUsed = person.lastUsed,
                lastEdit = person.lastEdit
            )
        }

        val sortedPeople = when (sortOption) {
            PersonSortOption.LAST_USED -> personItems.sortedWith(
                compareByDescending<PersonItemUi> { it.lastUsed }
                    .thenByDescending { it.lastEdit }
            )
            PersonSortOption.LAST_EDIT -> personItems.sortedByDescending { it.lastEdit }
            PersonSortOption.ALPHABETICAL -> personItems.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
        }

        val filteredPeople = if (searchQuery.isBlank()) {
            sortedPeople
        } else {
            sortedPeople.filter { person ->
                person.name.contains(searchQuery, ignoreCase = true) ||
                (!person.note.isNullOrBlank() && person.note.contains(searchQuery, ignoreCase = true)) ||
                (!person.tag.isNullOrBlank() && person.tag.contains(searchQuery, ignoreCase = true))
            }
        }

        PeopleUiState(
            people = filteredPeople,
            totalPeopleCount = sortedPeople.size,
            searchQuery = searchQuery,
            sortOption = sortOption,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PeopleUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sortOption: PersonSortOption) {
        viewModelScope.launch {
            settingsRepository.setPeopleSortOption(sortOption.name)
        }
    }
}
