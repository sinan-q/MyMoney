package com.sinxn.mymoney.core.data.importBackup

object EmbeddedBlockParser {
    
    private val BLOCK_REGEX = Regex(""" ##CF_V\d+\[(.*?)\]$""")
    private const val PREFIX = "##CF_V1["
    private const val SUFFIX = "]"

    data class ParsedNote(
        val originalNote: String?, // The note stripped of the block
        val values: Map<String, String>, // key to value mapping
        val malformed: Boolean
    )

    fun parseAndStrip(note: String?): ParsedNote {
        if (note.isNullOrBlank()) {
            return ParsedNote(note, emptyMap(), false)
        }

        val match = BLOCK_REGEX.find(note)
        if (match == null) {
            return ParsedNote(note, emptyMap(), false)
        }

        val rawBlock = match.groupValues[1]
        val strippedNote = note.substring(0, match.range.first).let {
            if (it.isEmpty()) null else it
        }
        
        val values = mutableMapOf<String, String>()
        var malformed = false
        
        try {
            // Unescape logic
            // Walk through characters to split by unescaped ';' and '='
            var currentKey = StringBuilder()
            var currentValue = StringBuilder()
            var isKey = true
            var escapeNext = false
            
            for (char in rawBlock) {
                if (escapeNext) {
                    if (isKey) currentKey.append(char) else currentValue.append(char)
                    escapeNext = false
                } else if (char == '\\') {
                    escapeNext = true
                } else if (char == '=') {
                    if (!isKey) malformed = true // = inside value without escape
                    isKey = false
                } else if (char == ';') {
                    if (currentKey.isNotEmpty()) {
                        values[currentKey.toString()] = currentValue.toString()
                    }
                    currentKey.clear()
                    currentValue.clear()
                    isKey = true
                } else {
                    if (isKey) currentKey.append(char) else currentValue.append(char)
                }
            }
            
            // Add the last key-value pair if exists
            if (currentKey.isNotEmpty() && !isKey) {
                values[currentKey.toString()] = currentValue.toString()
            } else if (currentKey.isNotEmpty() && isKey) {
                malformed = true // key without value
            }
            
        } catch (e: Exception) {
            malformed = true
        }

        // If block is totally unparsable but matched regex, we still return the stripped note 
        // with empty values and malformed=true, but spec says:
        // "Malformed or truncated blocks -> ignored entirely, original text left intact"
        if (malformed) {
            return ParsedNote(note, emptyMap(), true)
        }

        return ParsedNote(strippedNote, values, false)
    }
}
