package com.sinxn.mymoney.core.data.importBackup

import com.sinxn.mymoney.core.data.local.entity.CustomFieldExtractionRuleEntity
import org.json.JSONObject

object ExtractionEngine {

    /**
     * Runs extraction rules against description and note.
     * Returns a map of fieldId to extracted value.
     */
    fun extract(
        description: String?,
        note: String?,
        rules: List<CustomFieldExtractionRuleEntity>
    ): Map<String, String> {
        val extractedValues = mutableMapOf<String, String>()
        
        for (rule in rules.sortedBy { it.ruleOrder }) {
            // Target columns check
            val targets = rule.targetColumns.split(",")
            var matched = false

            for (target in targets) {
                val textToMatch = when (target.trim().lowercase()) {
                    "description" -> description
                    "note" -> note
                    else -> null
                }
                
                if (!textToMatch.isNullOrBlank()) {
                    val result = applyRule(textToMatch, rule)
                    if (result.isNotEmpty()) {
                        // Merge results without overwriting already matched higher priority rules
                        for ((fieldKey, value) in result) {
                            if (!extractedValues.containsKey(fieldKey)) {
                                extractedValues[fieldKey] = value
                            }
                        }
                        matched = true
                        break // First target match wins for this rule
                    }
                }
            }
        }
        
        return extractedValues
    }

    private fun applyRule(text: String, rule: CustomFieldExtractionRuleEntity): Map<String, String> {
        return try {
            if (rule.mode == "template") {
                applyTemplateRule(text, rule.pattern)
            } else {
                applyRegexRule(text, rule.pattern, rule.fieldMappings)
            }
        } catch (e: Exception) {
            emptyMap() // Safe fallback on regex timeout or invalid pattern
        }
    }

    private fun applyTemplateRule(text: String, pattern: String): Map<String, String> {
        // Example template: "Uber {amount} {date}"
        // We will compile this into a regex pattern dynamically.
        val placeholders = mutableListOf<String>()
        val placeholderRegex = Regex("\\{([a-zA-Z0-9_]+)\\}")
        
        var regexString = Regex.escape(pattern)
        
        val matches = placeholderRegex.findAll(pattern)
        for (match in matches) {
            val placeholder = match.groupValues[1]
            placeholders.add(placeholder)
            // Replace escaped \{placeholder\} in the escaped regexString with a capture group
            regexString = regexString.replace("\\{${placeholder}\\}", "(.*?)")
        }
        
        // Literal exact match (case-insensitive) but allow matching within string
        // The spec says "Literal text is matched exactly (case-insensitive)"
        val compiledRegex = Regex(regexString, RegexOption.IGNORE_CASE)
        val matchResult = compiledRegex.find(text) ?: return emptyMap()

        val results = mutableMapOf<String, String>()
        // group 0 is full match, groups 1..N are captures
        for (i in placeholders.indices) {
            if (i + 1 <= matchResult.groupValues.lastIndex) {
                val extracted = matchResult.groupValues[i + 1].trim()
                if (extracted.isNotEmpty()) {
                    results[placeholders[i]] = extracted
                }
            }
        }
        
        return results
    }

    private fun applyRegexRule(text: String, pattern: String, fieldMappingsJson: String): Map<String, String> {
        val compiledRegex = Regex(pattern, RegexOption.IGNORE_CASE)
        val matchResult = compiledRegex.find(text) ?: return emptyMap()
        
        val mappings = try {
            JSONObject(fieldMappingsJson)
        } catch (e: Exception) {
            JSONObject()
        }

        val results = mutableMapOf<String, String>()
        
        // Java regex API exposes groups by name if named groups are used
        val groups = matchResult.groups
        if (groups != null) {
            val iterator = mappings.keys()
            while (iterator.hasNext()) {
                val groupName = iterator.next() as String
                val fieldKey = mappings.getString(groupName)
                
                // For Android API < 26 named groups are tricky, but Kotlin Regex in modern versions supports it.
                // We'll use a hack to get it safely or require API 26+
                try {
                    val groupMatch = groups as MatchNamedGroupCollection
                    val value = groupMatch[groupName]?.value
                    if (!value.isNullOrBlank()) {
                        results[fieldKey] = value.trim()
                    }
                } catch (e: Exception) {
                    // Fallback or ignore if named groups are unsupported
                }
            }
        }
        
        return results
    }
}
