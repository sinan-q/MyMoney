package com.sinxn.mymoney.feature.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.EventRepository
import com.sinxn.mymoney.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

sealed class EventAddEditEvent {
    object Saved : EventAddEditEvent()
    object Deleted : EventAddEditEvent()
}

data class EventAddEditUiState(
    val eventId: String? = null,
    val name: String = "",
    val icon: String = "ic_event",
    val startDate: String = "",
    val endDate: String = "",
    val note: String = "",
    val tag: String = "",
    val isArchived: Boolean = false,
    val dateFormat: Int = 0,
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false
)

@HiltViewModel
class EventAddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val navEventId: String? = savedStateHandle.get<String>("eventId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        EventAddEditUiState(
            eventId = navEventId,
            isEditMode = navEventId != null
        )
    )
    val uiState: StateFlow<EventAddEditUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EventAddEditEvent>()
    val eventFlow: SharedFlow<EventAddEditEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val formattingSettings = settingsRepository.formattingSettings.first()
            val dateFormat = formattingSettings.dateFormat

            if (navEventId != null) {
                val existingEvent = eventRepository.getEventById(navEventId)
                if (existingEvent != null) {
                    _uiState.value = _uiState.value.copy(
                        eventId = existingEvent.id,
                        name = existingEvent.name,
                        icon = existingEvent.icon.ifBlank { "ic_event" },
                        startDate = existingEvent.startDate,
                        endDate = existingEvent.endDate,
                        note = existingEvent.note ?: "",
                        tag = existingEvent.tag ?: "",
                        isArchived = existingEvent.isArchived,
                        dateFormat = dateFormat,
                        isLoading = false,
                        isEditMode = true
                    )
                    return@launch
                }
            }

            // New Event mode
            val cal = java.util.Calendar.getInstance()
            val startDateStr = DateUtils.getSQLDateTimeString(cal.time)
            cal.add(java.util.Calendar.MONTH, 1)
            val endDateStr = DateUtils.getSQLDateTimeString(cal.time)
            _uiState.value = _uiState.value.copy(
                name = "",
                icon = "ic_event",
                startDate = startDateStr,
                endDate = endDateStr,
                note = "",
                tag = "",
                isArchived = false,
                dateFormat = dateFormat,
                isLoading = false,
                isEditMode = false
            )
        }
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onIconChange(icon: String) {
        _uiState.value = _uiState.value.copy(icon = icon)
    }

    fun onStartDateChange(startDate: String) {
        val startD = DateUtils.parseDate(startDate)
        val endD = DateUtils.parseDate(_uiState.value.endDate)
        val updatedEndDate = if (startD != null && endD != null && endD.before(startD)) {
            startDate
        } else {
            _uiState.value.endDate
        }
        _uiState.value = _uiState.value.copy(startDate = startDate, endDate = updatedEndDate)
    }

    fun onEndDateChange(endDate: String) {
        val startD = DateUtils.parseDate(_uiState.value.startDate)
        val endD = DateUtils.parseDate(endDate)
        val updatedStartDate = if (startD != null && endD != null && startD.after(endD)) {
            endDate
        } else {
            _uiState.value.startDate
        }
        _uiState.value = _uiState.value.copy(startDate = updatedStartDate, endDate = endDate)
    }

    fun onNoteChange(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun onTagChange(tag: String) {
        _uiState.value = _uiState.value.copy(tag = tag)
    }

    fun onArchivedChange(isArchived: Boolean) {
        _uiState.value = _uiState.value.copy(isArchived = isArchived)
    }

    fun saveEvent() {
        val current = _uiState.value
        val cleanName = current.name.trim()
        if (cleanName.isEmpty()) return

        var finalStartDate = current.startDate
        var finalEndDate = current.endDate
        val startD = DateUtils.parseDate(finalStartDate)
        val endD = DateUtils.parseDate(finalEndDate)
        if (startD != null && endD != null && startD.after(endD)) {
            finalEndDate = finalStartDate
        }

        viewModelScope.launch {
            eventRepository.saveEvent(
                id = current.eventId,
                name = cleanName,
                icon = current.icon,
                startDate = finalStartDate,
                endDate = finalEndDate,
                note = current.note.ifBlank { null },
                isArchived = current.isArchived,
                tag = current.tag.ifBlank { null }
            )
            _eventFlow.emit(EventAddEditEvent.Saved)
        }
    }

    fun deleteEvent() {
        val currentId = _uiState.value.eventId ?: return

        viewModelScope.launch {
            eventRepository.deleteEvent(currentId)
            _eventFlow.emit(EventAddEditEvent.Deleted)
        }
    }
}
