package com.sinxn.mymoney.core.util

import java.util.Locale

object MathExpressionEvaluator {

    /**
     * Evaluates a simple inline math expression string (supports +, -, *, /, ×, ÷ and unary minus).
     */
    fun evaluateSimpleMath(expr: String): Double? {
        val cleanExpr = expr.replace("×", "*").replace("÷", "/").trim()
        if (cleanExpr.isEmpty()) return null

        val tokens = mutableListOf<String>()
        var sb = StringBuilder()

        for (i in cleanExpr.indices) {
            val ch = cleanExpr[i]
            if (ch in listOf('+', '-', '*', '/')) {
                val isUnaryMinus = ch == '-' && (
                    sb.isEmpty() && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/"))
                )

                if (isUnaryMinus) {
                    sb.append(ch)
                } else {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString().trim())
                        sb = StringBuilder()
                    }
                    tokens.add(ch.toString())
                }
            } else if (ch != ' ') {
                sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) {
            tokens.add(sb.toString().trim())
        }

        if (tokens.isEmpty()) return null
        var currentVal = tokens[0].toDoubleOrNull() ?: return null

        var idx = 1
        while (idx < tokens.size - 1) {
            val op = tokens[idx]
            val nextVal = tokens[idx + 1].toDoubleOrNull() ?: break
            when (op) {
                "+" -> currentVal += nextVal
                "-" -> currentVal -= nextVal
                "*" -> currentVal *= nextVal
                "/" -> if (nextVal != 0.0) currentVal /= nextVal
            }
            idx += 2
        }
        return currentVal
    }

    /**
     * Updates amount string based on numpad key press.
     */
    fun processNumpadKeyPress(currentAmount: String, key: String): String {
        var current = currentAmount
        return when {
            key == "BACKSPACE" -> {
                if (current.isNotEmpty()) {
                    current = current.trimEnd()
                    if (current.isNotEmpty()) {
                        current = current.dropLast(1).trimEnd()
                    }
                    current.ifEmpty { "0" }
                } else "0"
            }
            key in listOf("+", "-", "×", "÷") -> {
                val trimmed = current.trim()
                val base = if (trimmed.endsWith("+") || trimmed.endsWith("-") || trimmed.endsWith("×") || trimmed.endsWith("÷")) {
                    trimmed.dropLast(1).trim()
                } else {
                    trimmed
                }
                "$base $key "
            }
            key == "." -> {
                val lastToken = current.split(" ").lastOrNull() ?: ""
                if (!lastToken.contains(".")) {
                    "$current."
                } else {
                    current
                }
            }
            else -> {
                val trimmed = current.trim()
                if (trimmed == "0" || trimmed == "0.0" || trimmed == "0.00") {
                    key
                } else {
                    current + key
                }
            }
        }
    }

    /**
     * Calculates instant preview result of an input expression string.
     */
    fun getImmediateResult(editAmount: String, decimals: Int): String {
        val trimmed = editAmount.trim()
        val rest = if (trimmed.startsWith("-")) trimmed.substring(1) else trimmed
        if (!rest.any { it in listOf('+', '-', '×', '÷', '*', '/') }) {
            return if (trimmed.isEmpty()) "0" else trimmed
        }
        val expr = trimmed.replace("×", "*").replace("÷", "/")
        val result = evaluateSimpleMath(expr) ?: return if (trimmed.isEmpty()) "0" else trimmed
        return formatMathResult(result, decimals)
    }

    /**
     * Evaluates math expression and formats string result.
     */
    fun evaluateMathExpression(editAmount: String, decimals: Int): String {
        val expr = editAmount.replace("×", "*").replace("÷", "/")
        val result = evaluateSimpleMath(expr) ?: return editAmount
        return formatMathResult(result, decimals)
    }

    private fun formatMathResult(result: Double, decimals: Int): String {
        return if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            "%.${decimals}f".format(Locale.US, result)
        }
    }
}
