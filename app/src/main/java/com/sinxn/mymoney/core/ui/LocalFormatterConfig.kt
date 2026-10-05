package com.sinxn.mymoney.core.ui

import androidx.compose.runtime.compositionLocalOf
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.util.MoneyFormatter

val LocalFormatterConfig = compositionLocalOf { MoneyFormatter.Config() }
val LocalFormattingSettings = compositionLocalOf { FormattingSettings() }
