package com.sinxn.mymoney.feature.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.repository.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaceFormState(
    val isOpen: Boolean = false,
    val editingPlace: PlaceEntity? = null,
    val name: String = "",
    val address: String = "",
    val latitude: String = "",
    val longitude: String = ""
)

data class PlaceUiState(
    val places: List<PlaceEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isEditDialogOpen: Boolean = false,
    val editingPlace: PlaceEntity? = null,
    val editName: String = "",
    val editAddress: String = "",
    val editLatitude: String = "",
    val editLongitude: String = ""
)

@HiltViewModel
class PlaceViewModel @Inject constructor(
    private val placeRepository: PlaceRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(PlaceFormState())

    val uiState: StateFlow<PlaceUiState> = combine(
        placeRepository.getPlaces(),
        _formState
    ) { places, form ->
        PlaceUiState(
            places = places,
            isLoading = false,
            isEditDialogOpen = form.isOpen,
            editingPlace = form.editingPlace,
            editName = form.name,
            editAddress = form.address,
            editLatitude = form.latitude,
            editLongitude = form.longitude
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaceUiState()
    )

    fun openCreatePlaceDialog() {
        _formState.value = PlaceFormState(isOpen = true)
    }

    fun openEditPlaceDialog(place: PlaceEntity) {
        _formState.value = PlaceFormState(
            isOpen = true,
            editingPlace = place,
            name = place.name,
            address = place.address ?: "",
            latitude = place.latitude?.toString() ?: "",
            longitude = place.longitude?.toString() ?: ""
        )
    }

    fun closeDialog() {
        _formState.value = _formState.value.copy(isOpen = false)
    }

    fun onNameChange(name: String) {
        _formState.value = _formState.value.copy(name = name)
    }

    fun onAddressChange(address: String) {
        _formState.value = _formState.value.copy(address = address)
    }

    fun onLatitudeChange(lat: String) {
        _formState.value = _formState.value.copy(latitude = lat)
    }

    fun onLongitudeChange(lng: String) {
        _formState.value = _formState.value.copy(longitude = lng)
    }

    fun savePlace() {
        viewModelScope.launch {
            val form = _formState.value
            val name = form.name.trim()
            if (name.isEmpty()) return@launch

            val lat = form.latitude.toDoubleOrNull()
            val lng = form.longitude.toDoubleOrNull()

            placeRepository.savePlace(
                id = form.editingPlace?.id,
                name = name,
                address = form.address.takeIf { it.isNotBlank() },
                latitude = lat,
                longitude = lng
            )
            closeDialog()
        }
    }

    fun deletePlace(place: PlaceEntity) {
        viewModelScope.launch {
            placeRepository.deletePlace(place.id)
        }
    }
}
