package com.sinxn.mymoney.core.data.importBackup

import com.sinxn.mymoney.core.data.local.entity.CustomFieldExtractionRuleEntity
import org.json.JSONObject

object ExtractionEngine {

    /**
     * Runs extraction rules against description and note.
     * Returns a map of fieldId to extracted value.
     */
    /**
     * Runs extraction rules against description and note.
     * Returns a map of fieldId to extracted value.
     */
    fun extract(
        description: String?,
        note: String?,
        rules: List<CustomFieldExtractionRuleEntity>,
        fieldKeyById: Map<String, String> = emptyMap()
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
                    val result = applyRule(textToMatch, rule).toMutableMap()
                    if (result.isNotEmpty()) {
                        // If the rule is associated with a specific fieldId and the template didn't explicitly capture it,
                        // map that field as matched ("true" for boolean/flag fields)
                        val associatedKey = fieldKeyById[rule.fieldId]
                        if (associatedKey != null && !result.containsKey(associatedKey)) {
                            result[associatedKey] = "true"
                        }

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
        // Example template: "Uber {amount} {date}" or "{food} @ {restaurant} {online=false}"
        val placeholders = mutableListOf<String>()
        val hardcodedValues = mutableMapOf<String, String>()
        
        // Matches {field} or {field=value}
        val placeholderRegex = Regex("\\{([a-zA-Z0-9_]+)(?:=([^}]+))?\\}")
        
        var regexString = ""
        var lastMatchEnd = 0
        
        val matches = placeholderRegex.findAll(pattern).toList()
        
        // We need to keep track of whether we've added any capturing groups
        var capturingGroupCount = 0
        
        for (i in matches.indices) {
            val match = matches[i]
            val placeholder = match.groupValues[1]
            val hardcodedValue = match.groupValues[2]
            
            if (hardcodedValue.isNotEmpty()) {
                // It's a hardcoded value, e.g., {online=false}
                hardcodedValues[placeholder] = hardcodedValue
                
                // If there's trailing space before a hardcoded value, we should trim it
                // so we don't accidentally require that space in the actual text.
                // We use \s* to make any spacing optional.
                val staticPart = pattern.substring(lastMatchEnd, match.range.first).trimEnd()
                regexString += Regex.escape(staticPart) + "\\s*"
                
            } else {
                placeholders.add(placeholder)
                capturingGroupCount++
                
                // Append escaped static text before the placeholder
                val staticPart = pattern.substring(lastMatchEnd, match.range.first)
                regexString += Regex.escape(staticPart)
                
                // If the placeholder is the last capturing one in the template and there's no trailing static text,
                // use greedy (.*) so it doesn't match empty string.
                val noMoreCapturing = matches.subList(i + 1, matches.size).all { it.groupValues[2].isNotEmpty() }
                val remainingStatic = pattern.substring(match.range.last + 1).replace(placeholderRegex, "").trimEnd()
                val isAtEnd = noMoreCapturing && remainingStatic.isEmpty()
                val replacement = if (isAtEnd) "(.*)" else "(.*?)"
                regexString += replacement
            }
            
            lastMatchEnd = match.range.last + 1
        }
        
        // Append any remaining escaped static text
        if (lastMatchEnd < pattern.length) {
            regexString += Regex.escape(pattern.substring(lastMatchEnd))
        }
        
        val results = mutableMapOf<String, String>()
        
        // If there is no regex part (only hardcoded), we don't need to match against text
        if (regexString.isNotBlank() && regexString != "\\s*") {
            // Literal exact match (case-insensitive) but allow matching within string
            val compiledRegex = Regex(regexString, RegexOption.IGNORE_CASE)
            val matchResult = compiledRegex.find(text) ?: return emptyMap()
    
            // group 0 is full match, groups 1..N are captures
            var captureIndex = 1
            for (placeholder in placeholders) {
                if (captureIndex <= matchResult.groupValues.lastIndex) {
                    val extracted = matchResult.groupValues[captureIndex].trim()
                    if (extracted.isNotEmpty()) {
                        results[placeholder] = extracted
                    }
                }
                captureIndex++
            }
        }
        
        // Add hardcoded values
        results.putAll(hardcodedValues)
        
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
