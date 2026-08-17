package com.sinxn.mymoney.feature.place

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.repository.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlaceAddEditEvent {
    object Saved : PlaceAddEditEvent()
    object Deleted : PlaceAddEditEvent()
}

data class PlaceAddEditUiState(
    val placeId: String? = null,
    val name: String = "",
    val icon: String = "ic_place",
    val address: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val tag: String = "",
    val isArchived: Boolean = false,
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false
)

@HiltViewModel
class PlaceAddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placeRepository: PlaceRepository
) : ViewModel() {

    val navPlaceId: String? = savedStateHandle.get<String>("placeId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        PlaceAddEditUiState(
            placeId = navPlaceId,
            isEditMode = navPlaceId != null
        )
    )
    val uiState: StateFlow<PlaceAddEditUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<PlaceAddEditEvent>()
    val eventFlow: SharedFlow<PlaceAddEditEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            if (navPlaceId != null) {
                val existingPlace = placeRepository.getPlaceById(navPlaceId)
                if (existingPlace != null) {
                    _uiState.value = _uiState.value.copy(
                        placeId = existingPlace.id,
                        name = existingPlace.name,
                        icon = existingPlace.icon.ifBlank { "ic_place" },
                        address = existingPlace.address ?: "",
                        latitude = existingPlace.latitude?.toString() ?: "",
                        longitude = existingPlace.longitude?.toString() ?: "",
                        tag = existingPlace.tag ?: "",
                        isArchived = existingPlace.isArchived,
                        isLoading = false,
                        isEditMode = true
                    )
                    return@launch
                }
            }

            // New Place mode
            _uiState.value = _uiState.value.copy(
                name = "",
                icon = "ic_place",
                address = "",
                latitude = "",
                longitude = "",
                tag = "",
                isArchived = false,
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

    fun onAddressChange(address: String) {
        _uiState.value = _uiState.value.copy(address = address)
    }

    fun onLatitudeChange(latitude: String) {
        _uiState.value = _uiState.value.copy(latitude = latitude)
    }

    fun onLongitudeChange(longitude: String) {
        _uiState.value = _uiState.value.copy(longitude = longitude)
    }

    fun onTagChange(tag: String) {
        _uiState.value = _uiState.value.copy(tag = tag)
    }

    fun onArchivedChange(isArchived: Boolean) {
        _uiState.value = _uiState.value.copy(isArchived = isArchived)
    }

    fun clearCoordinates() {
        _uiState.value = _uiState.value.copy(latitude = "", longitude = "")
    }

    fun savePlace() {
        val current = _uiState.value
        val cleanName = current.name.trim()
        if (cleanName.isEmpty()) return

        val lat = current.latitude.trim().toDoubleOrNull()
        val lng = current.longitude.trim().toDoubleOrNull()

        viewModelScope.launch {
            placeRepository.savePlace(
                id = current.placeId,
                name = cleanName,
                icon = current.icon,
                address = current.address.trim().ifBlank { null },
                latitude = lat,
                longitude = lng,
                isArchived = current.isArchived,
                tag = current.tag.trim().ifBlank { null }
            )
            _eventFlow.emit(PlaceAddEditEvent.Saved)
        }
    }

    fun deletePlace() {
        val currentId = _uiState.value.placeId ?: return
        viewModelScope.launch {
            placeRepository.deletePlace(currentId)
            _eventFlow.emit(PlaceAddEditEvent.Deleted)
        }
    }
}
