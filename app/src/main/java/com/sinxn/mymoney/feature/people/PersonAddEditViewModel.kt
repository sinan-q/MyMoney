package com.sinxn.mymoney.feature.people

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PersonAddEditEvent {
    object Saved : PersonAddEditEvent()
    object Deleted : PersonAddEditEvent()
}

data class PersonAddEditUiState(
    val personId: String? = null,
    val name: String = "",
    val note: String = "",
    val tag: String = "",
    val icon: String = "ic_person",
    val isArchived: Boolean = false,
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false
)

@HiltViewModel
class PersonAddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personRepository: PersonRepository
) : ViewModel() {

    val navPersonId: String? = savedStateHandle.get<String>("personId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        PersonAddEditUiState(
            personId = navPersonId,
            isEditMode = navPersonId != null
        )
    )
    val uiState: StateFlow<PersonAddEditUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<PersonAddEditEvent>()
    val eventFlow: SharedFlow<PersonAddEditEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            if (navPersonId != null) {
                val existingPerson = personRepository.getPersonById(navPersonId)
                if (existingPerson != null) {
                    _uiState.value = _uiState.value.copy(
                        personId = existingPerson.id,
                        name = existingPerson.name,
                        note = existingPerson.note ?: "",
                        tag = existingPerson.tag ?: "",
                        icon = existingPerson.icon.ifBlank { "ic_person" },
                        isArchived = existingPerson.isArchived,
                        isLoading = false,
                        isEditMode = true
                    )
                    return@launch
                }
            }

            // New Person mode
            _uiState.value = _uiState.value.copy(
                name = "",
                note = "",
                tag = "",
                icon = "ic_person",
                isArchived = false,
                isLoading = false,
                isEditMode = false
            )
        }
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onNoteChange(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun onTagChange(tag: String) {
        _uiState.value = _uiState.value.copy(tag = tag)
    }

    fun onIconChange(icon: String) {
        _uiState.value = _uiState.value.copy(icon = icon)
    }

    fun savePerson() {
        val current = _uiState.value
        val cleanName = current.name.trim()
        if (cleanName.isEmpty()) return

        viewModelScope.launch {
            personRepository.savePerson(
                id = current.personId,
                name = cleanName,
                icon = current.icon,
                note = current.note.ifBlank { null },
                isArchived = current.isArchived,
                tag = current.tag.ifBlank { null }
            )
            _eventFlow.emit(PersonAddEditEvent.Saved)
        }
    }

    fun deletePerson() {
        val currentId = _uiState.value.personId ?: return

        viewModelScope.launch {
            personRepository.deletePerson(currentId)
            _eventFlow.emit(PersonAddEditEvent.Deleted)
        }
    }
}
