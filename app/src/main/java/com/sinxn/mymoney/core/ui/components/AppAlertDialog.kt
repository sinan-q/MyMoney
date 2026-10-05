package com.sinxn.mymoney.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.sinxn.mymoney.R

/**
 * Standard confirmation dialog following Material 3 guidelines and consistent styling.
 */
@Composable
fun AppConfirmDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    confirmText: String = stringResource(R.string.dialog_confirm),
    dismissText: String = stringResource(R.string.dialog_cancel),
    confirmColor: Color = MaterialTheme.colorScheme.primary,
    isDestructive: Boolean = false,
    content: (@Composable () -> Unit)? = null
) {
    val finalConfirmColor = if (isDestructive) MaterialTheme.colorScheme.error else confirmColor

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (content != null) {
                content()
            } else if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = finalConfirmColor)
            ) {
                Text(
                    text = confirmText,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

/**
 * Specialized destructive confirmation dialog (e.g., delete, reset, erase).
 * Highlights the confirm action with error color and bold weight.
 */
@Composable
fun AppDestructiveConfirmDialog(
    title: String,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    confirmText: String = stringResource(R.string.dialog_delete),
    dismissText: String = stringResource(R.string.dialog_cancel),
    content: (@Composable () -> Unit)? = null
) {
    AppConfirmDialog(
        title = title,
        message = message,
        onConfirm = onConfirmDelete,
        onDismiss = onDismiss,
        modifier = modifier,
        confirmText = confirmText,
        dismissText = dismissText,
        isDestructive = true,
        content = content
    )
}

/**
 * Informational dialog with a single acknowledgment button (e.g., warnings or error notices).
 */
@Composable
fun AppInfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = stringResource(R.string.dialog_ok)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
