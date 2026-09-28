package com.sinxn.mymoney.core.data.importBackup

object EmbeddedBlockWriter {
    
    private const val PREFIX = " ##CF_V1["
    private const val SUFFIX = "]"
    
    fun appendBlock(note: String?, values: Map<String, String>): String? {
        if (values.isEmpty()) {
            return note
        }
        
        val block = buildString {
            append(PREFIX)
            var first = true
            for ((key, value) in values) {
                if (!first) {
                    append(";")
                }
                append(escape(key))
                append("=")
                append(escape(value))
                first = false
            }
            append(SUFFIX)
        }
        
        return if (note.isNullOrEmpty()) {
            block.trimStart() // If there was no note, we still need the block but maybe no leading space
        } else {
            note + block
        }
    }
    
    private fun escape(raw: String): String {
        return raw.replace("\\", "\\\\")
                  .replace("[", "\\[")
                  .replace("]", "\\]")
                  .replace(";", "\\;")
                  .replace("=", "\\=")
    }
}
