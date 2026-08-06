package com.sinxn.mymoney.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.repository.SqlResult
import com.sinxn.mymoney.core.data.repository.SqlRunnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SqlConsoleUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val result: SqlResult? = null,
    val error: String? = null,
    val tables: List<String> = emptyList(),
    val selectedTableSchema: List<String>? = null,
    val selectedTableName: String? = null,
    val queryHistory: List<String> = emptyList()
)

@HiltViewModel
class SqlConsoleViewModel @Inject constructor(
    private val sqlRunnerRepository: SqlRunnerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SqlConsoleUiState())
    val uiState: StateFlow<SqlConsoleUiState> = _uiState.asStateFlow()

    init {
        loadTables()
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun executeQuery() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, result = null) }

            try {
                val result = sqlRunnerRepository.executeQuery(query)

                // Add to history (avoid duplicates at the top)
                val newHistory = _uiState.value.queryHistory.toMutableList()
                newHistory.remove(query)
                newHistory.add(0, query)
                if (newHistory.size > 20) newHistory.removeAt(newHistory.lastIndex)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = result,
                        error = null,
                        queryHistory = newHistory
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = null,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun loadTableSchema(tableName: String) {
        viewModelScope.launch {
            try {
                val schema = sqlRunnerRepository.getTableSchema(tableName)
                _uiState.update {
                    it.copy(
                        selectedTableSchema = schema,
                        selectedTableName = tableName
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load schema")
                }
            }
        }
    }

    fun clearSchemaSelection() {
        _uiState.update { it.copy(selectedTableSchema = null, selectedTableName = null) }
    }

    fun insertTableQuery(tableName: String) {
        _uiState.update {
            it.copy(query = "SELECT * FROM $tableName LIMIT 50")
        }
    }

    fun selectHistoryItem(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun clearResults() {
        _uiState.update { it.copy(result = null, error = null) }
    }

    private fun loadTables() {
        viewModelScope.launch {
            try {
                val tables = sqlRunnerRepository.getTableNames()
                _uiState.update { it.copy(tables = tables) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load tables: ${e.message}") }
            }
        }
    }
}
