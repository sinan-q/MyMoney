package com.sinxn.mymoney.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.sinxn.mymoney.core.util.MathExpressionEvaluator

/**
 * State holder for coordinating numpad visibility, math expression evaluation,
 * focus management, and software keyboard interactions across financial input forms.
 */
@Stable
class NumpadFormState(
    initialNumpadVisible: Boolean = true,
    val focusRequester: FocusRequester,
    val focusManager: FocusManager,
    val keyboardController: SoftwareKeyboardController?
) {
    var isNumpadVisible by mutableStateOf(initialNumpadVisible)

    /**
     * Checks if the given amount string contains mathematical operators (+, -, ×, ÷).
     */
    fun hasOperator(amountText: String): Boolean {
        val trimmed = amountText.trim()
        val rest = if (trimmed.startsWith("-")) trimmed.substring(1) else trimmed
        return rest.contains("+") || rest.contains("-") || rest.contains("×") || rest.contains("÷")
    }

    /**
     * Evaluates immediate math expression result for the given amount string.
     */
    fun getImmediateResult(amountText: String, decimals: Int = 2): String {
        return MathExpressionEvaluator.getImmediateResult(amountText, decimals)
    }

    /**
     * Opens the numpad while hiding the system software keyboard and clearing focus.
     */
    fun showNumpad() {
        focusManager.clearFocus()
        keyboardController?.hide()
        isNumpadVisible = true
    }

    /**
     * Dismisses the numpad as well as the system keyboard.
     */
    fun dismissKeyboardAndNumpad() {
        focusManager.clearFocus()
        keyboardController?.hide()
        isNumpadVisible = false
    }

    /**
     * Alias for [dismissKeyboardAndNumpad].
     */
    fun dismiss() = dismissKeyboardAndNumpad()

    /**
     * Called when a text input field (e.g., Description or Note) gains focus.
     */
    fun onFocusField() {
        isNumpadVisible = false
    }

    /**
     * Called when the 'Next' action is triggered on the numpad.
     * Hides the numpad, requests focus on [targetFocusRequester] (defaulting to [focusRequester]),
     * and shows the software keyboard.
     */
    fun onNext(targetFocusRequester: FocusRequester = focusRequester) {
        isNumpadVisible = false
        targetFocusRequester.requestFocus()
        keyboardController?.show()
    }
}

/**
 * Creates and remembers a [NumpadFormState].
 *
 * @param initialNumpadVisible Whether the numpad is initially visible (e.g., true for new transactions/debts).
 * @param focusRequester FocusRequester for the primary next field (e.g. description input).
 * @param focusManager FocusManager to clear active focus when opening numpad.
 * @param keyboardController SoftwareKeyboardController to show/hide software keyboard.
 */
@Composable
fun rememberNumpadFormState(
    initialNumpadVisible: Boolean = true,
    focusRequester: FocusRequester = remember { FocusRequester() },
    focusManager: FocusManager = LocalFocusManager.current,
    keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
): NumpadFormState {
    return remember(focusRequester, focusManager, keyboardController) {
        NumpadFormState(
            initialNumpadVisible = initialNumpadVisible,
            focusRequester = focusRequester,
            focusManager = focusManager,
            keyboardController = keyboardController
        )
    }
}
