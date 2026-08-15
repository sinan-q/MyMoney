package com.sinxn.mymoney.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.repository.EventRepository
import com.sinxn.mymoney.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class EventFormState(
    val isOpen: Boolean = false,
    val editingEvent: EventEntity? = null,
    val name: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val note: String = ""
)

data class EventUiState(
    val events: List<EventEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isEditDialogOpen: Boolean = false,
    val editingEvent: EventEntity? = null,
    val editName: String = "",
    val editStartDate: String = "",
    val editEndDate: String = "",
    val editNote: String = ""
)

@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(EventFormState())

    val uiState: StateFlow<EventUiState> = combine(
        eventRepository.getEvents(),
        _formState
    ) { events, form ->
        EventUiState(
            events = events,
            isLoading = false,
            isEditDialogOpen = form.isOpen,
            editingEvent = form.editingEvent,
            editName = form.name,
            editStartDate = form.startDate,
            editEndDate = form.endDate,
            editNote = form.note
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventUiState()
    )

    fun openCreateEventDialog() {
        val nowStr = DateUtils.getSQLDateTimeString(Date())
        _formState.value = EventFormState(
            isOpen = true,
            editingEvent = null,
            name = "",
            startDate = nowStr,
            endDate = nowStr,
            note = ""
        )
    }

    fun openEditEventDialog(event: EventEntity) {
        _formState.value = EventFormState(
            isOpen = true,
            editingEvent = event,
            name = event.name,
            startDate = event.startDate,
            endDate = event.endDate,
            note = event.note ?: ""
        )
    }

    fun closeDialog() {
        _formState.value = _formState.value.copy(isOpen = false)
    }

    fun onNameChange(name: String) {
        _formState.value = _formState.value.copy(name = name)
    }

    fun onStartDateChange(date: String) {
        _formState.value = _formState.value.copy(startDate = date)
    }

    fun onEndDateChange(date: String) {
        _formState.value = _formState.value.copy(endDate = date)
    }

    fun onNoteChange(note: String) {
        _formState.value = _formState.value.copy(note = note)
    }

    fun saveEvent() {
        viewModelScope.launch {
            val form = _formState.value
            val name = form.name.trim()
            if (name.isEmpty()) return@launch

            eventRepository.saveEvent(
                id = form.editingEvent?.id,
                name = name,
                startDate = form.startDate,
                endDate = form.endDate,
                note = form.note.takeIf { it.isNotBlank() }
            )
            closeDialog()
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            eventRepository.deleteEvent(event.id)
        }
    }
}
