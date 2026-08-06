package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SqlResult(
    val columns: List<String>,
    val rows: List<List<String>>,
    val rowCount: Int,
    val executionTimeMs: Long
)

@Singleton
class SqlRunnerRepository @Inject constructor(
    private val database: AppDatabase
) {

    companion object {
        private const val MAX_ROWS = 500
        private val FORBIDDEN_KEYWORDS = listOf(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE",
            "REPLACE", "TRUNCATE", "ATTACH", "DETACH", "REINDEX",
            "VACUUM", "PRAGMA"
        )
    }

    /**
     * Executes a read-only SQL query and returns structured results.
     * Only SELECT statements are allowed.
     */
    suspend fun executeQuery(query: String): SqlResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()

        // Validate: must start with SELECT
        if (!trimmed.startsWith("SELECT", ignoreCase = true)) {
            throw IllegalArgumentException("Only SELECT queries are allowed.")
        }

        // Validate: block dangerous keywords anywhere in the query
        val upperQuery = trimmed.uppercase()
        for (keyword in FORBIDDEN_KEYWORDS) {
            // Check for keyword as a standalone word (not part of a column name)
            if (Regex("\\b$keyword\\b").containsMatchIn(upperQuery)) {
                throw IllegalArgumentException("Forbidden keyword detected: $keyword. Only SELECT queries are allowed.")
            }
        }

        val startTime = System.currentTimeMillis()

        val db = database.openHelper.readableDatabase
        val cursor = db.query(trimmed)

        cursor.use { c ->
            val columns = c.columnNames.toList()
            val rows = mutableListOf<List<String>>()
            var totalRows = 0

            while (c.moveToNext()) {
                totalRows++
                if (rows.size < MAX_ROWS) {
                    val row = columns.indices.map { index ->
                        try {
                            when (c.getType(index)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> "NULL"
                                android.database.Cursor.FIELD_TYPE_BLOB -> "[BLOB ${c.getBlob(index).size} bytes]"
                                else -> c.getString(index) ?: "NULL"
                            }
                        } catch (e: Exception) {
                            "[Error]"
                        }
                    }
                    rows.add(row)
                }
            }

            val executionTime = System.currentTimeMillis() - startTime

            SqlResult(
                columns = columns,
                rows = rows,
                rowCount = totalRows,
                executionTimeMs = executionTime
            )
        }
    }

    /**
     * Returns all user table names in the database.
     */
    suspend fun getTableNames(): List<String> = withContext(Dispatchers.IO) {
        val db = database.openHelper.readableDatabase
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' AND name NOT LIKE 'android_%' ORDER BY name")

        cursor.use { c ->
            val tables = mutableListOf<String>()
            while (c.moveToNext()) {
                tables.add(c.getString(0))
            }
            tables
        }
    }

    /**
     * Returns column info for a given table.
     */
    suspend fun getTableSchema(tableName: String): List<String> = withContext(Dispatchers.IO) {
        // Validate table name to prevent injection
        if (!tableName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
            throw IllegalArgumentException("Invalid table name.")
        }

        val db = database.openHelper.readableDatabase
        val cursor = db.query("PRAGMA table_info($tableName)")

        cursor.use { c ->
            val columns = mutableListOf<String>()
            while (c.moveToNext()) {
                val name = c.getString(1) // column name
                val type = c.getString(2) // column type
                val notNull = c.getInt(3) == 1
                val pk = c.getInt(5) == 1
                val suffix = buildString {
                    if (pk) append(" PK")
                    if (notNull) append(" NOT NULL")
                }
                columns.add("$name ($type$suffix)")
            }
            columns
        }
    }
}
