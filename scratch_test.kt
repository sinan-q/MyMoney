fun main() {
    val pattern = "{food} @ {online} @ {restaurant}"
    val placeholderRegex = Regex("\\{([a-zA-Z0-9_]+)\\}")
    var regexString = Regex.escape(pattern)
    println("Escaped: " + regexString)
    
    val matches = placeholderRegex.findAll(pattern).toList()
    for (i in matches.indices) {
        val placeholder = matches[i].groupValues[1]
        val isAtEnd = (i == matches.lastIndex) && pattern.trimEnd().endsWith("}")
        val replacement = if (isAtEnd) "(.*)" else "(.*?)"
        regexString = regexString.replaceFirst("\\{${placeholder}\\}", replacement)
    }
    println("Final Regex: " + regexString)
    
    val compiledRegex = Regex(regexString, RegexOption.IGNORE_CASE)
    val text = "Burger @ Swiggy @ McD"
    val matchResult = compiledRegex.find(text)
    if (matchResult != null) {
        println("Matched!")
    } else {
        println("No match.")
    }
}
