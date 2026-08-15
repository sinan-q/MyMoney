package com.sinxn.mymoney.core.ui.components

/**
 * Types of pickers/dialogs that can be actively displayed in input forms.
 * Using a single active picker avoids managing multiple boolean flags.
 */
sealed interface FormPicker {
    data object Category : FormPicker
    data object Wallet : FormPicker
    data object TargetWallet : FormPicker
    data object Place : FormPicker
    data object Event : FormPicker
    data object People : FormPicker
    data object Date : FormPicker
    data object DueDate : FormPicker
}
