package com.sinxn.mymoney.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState = _uiState.asStateFlow()

    val importState = _uiState.asStateFlow()

    private val _analysisReport = MutableStateFlow<com.sinxn.mymoney.core.data.importBackup.model.BackupAnalysisReport?>(null)
    val analysisReport = _analysisReport.asStateFlow()

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            try {
                repository.importBackup(uri)
                _uiState.value = BackupUiState.Success("Backup imported successfully.")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = BackupUiState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            try {
                repository.exportBackup(uri)
                _uiState.value = BackupUiState.Success("Backup exported successfully.")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = BackupUiState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun analyzeBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            try {
                val report = repository.analyzeBackup(uri)
                _analysisReport.value = report
                _uiState.value = BackupUiState.Idle
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error(e.message ?: "Analysis failed")
            }
        }
    }

    fun clearAnalysis() {
        _analysisReport.value = null
    }

    fun resetState() {
        _uiState.value = BackupUiState.Idle
    }
}

sealed class BackupUiState {
    object Idle : BackupUiState()
    object Loading : BackupUiState()
    data class Success(val message: String) : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}
