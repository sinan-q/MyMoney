package com.sinxn.mymoney.feature.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PersonSortOption(val title: String) {
    LAST_USED("Recently Used"),
    LAST_EDIT("Recently Edited"),
    ALPHABETICAL("Alphabetical (A-Z)")
}

data class PersonFormState(
    val isOpen: Boolean = false,
    val editingPerson: PersonEntity? = null,
    val name: String = "",
    val note: String = ""
)

data class PeopleUiState(
    val people: List<PersonEntity> = emptyList(),
    val sortOption: PersonSortOption = PersonSortOption.LAST_USED,
    val isLoading: Boolean = false,
    val isEditDialogOpen: Boolean = false,
    val editingPerson: PersonEntity? = null,
    val editName: String = "",
    val editNote: String = ""
)

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val personRepository: PersonRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(PersonFormState())

    val uiState: StateFlow<PeopleUiState> = combine(
        personRepository.getPeople(),
        settingsRepository.peopleSortOption,
        _formState
    ) { people, sortOptionName, form ->
        val sortOption = try {
            PersonSortOption.valueOf(sortOptionName)
        } catch (e: Exception) {
            PersonSortOption.LAST_USED
        }

        val sortedPeople = when (sortOption) {
            PersonSortOption.LAST_USED -> people.sortedWith(
                compareByDescending<PersonEntity> { it.lastUsed }
                    .thenByDescending { it.lastEdit }
            )
            PersonSortOption.LAST_EDIT -> people.sortedByDescending { it.lastEdit }
            PersonSortOption.ALPHABETICAL -> people.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
        }

        PeopleUiState(
            people = sortedPeople,
            sortOption = sortOption,
            isLoading = false,
            isEditDialogOpen = form.isOpen,
            editingPerson = form.editingPerson,
            editName = form.name,
            editNote = form.note
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PeopleUiState()
    )

    fun setSortOption(sortOption: PersonSortOption) {
        viewModelScope.launch {
            settingsRepository.setPeopleSortOption(sortOption.name)
        }
    }

    fun openCreatePersonDialog() {
        _formState.value = PersonFormState(isOpen = true)
    }

    fun openEditPersonDialog(person: PersonEntity) {
        _formState.value = PersonFormState(
            isOpen = true,
            editingPerson = person,
            name = person.name,
            note = person.note ?: ""
        )
    }

    fun closeDialog() {
        _formState.value = _formState.value.copy(isOpen = false)
    }

    fun onNameChange(name: String) {
        _formState.value = _formState.value.copy(name = name)
    }

    fun onNoteChange(note: String) {
        _formState.value = _formState.value.copy(note = note)
    }

    fun savePerson() {
        viewModelScope.launch {
            val form = _formState.value
            val name = form.name.trim()
            if (name.isEmpty()) return@launch

            personRepository.savePerson(
                id = form.editingPerson?.id,
                name = name,
                note = form.note.takeIf { it.isNotBlank() }
            )
            closeDialog()
        }
    }

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch {
            personRepository.deletePerson(person.id)
        }
    }
}
