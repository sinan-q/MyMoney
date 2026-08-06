package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun NumpadView(
    onKeyPress: (String) -> Unit,
    onEvaluate: () -> Unit,
    onSave: () -> Unit,
    onNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    hasOperatorInAmount: Boolean = false,
    saveButtonText: String = "Save",
    saveButtonColor: Color = MaterialTheme.colorScheme.primary,
    isSaving: Boolean = false,
    isSaveEnabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val rows = listOf(
            listOf("1", "2", "3", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("7", "8", "9", "-"),
            listOf(".", "0", "⌫", "+")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    CleanNumpadKey(
                        key = key,
                        backgroundColor = if (key in listOf("+", "-", "×", "÷")) {
                            saveButtonColor.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        contentColor = if (key in listOf("+", "-", "×", "÷")) {
                            saveButtonColor
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (key == "⌫") {
                                onKeyPress("BACKSPACE")
                            } else {
                                onKeyPress(key)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (onNext != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Next Button (Hides Numpad)
                Button(
                    onClick = {
                        if (hasOperatorInAmount) {
                            onEvaluate()
                        }
                        onNext()
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = saveButtonColor.copy(alpha = 0.15f),
                        contentColor = saveButtonColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Primary Action Button (Save or Calculate =)
                Button(
                    onClick = {
                        if (hasOperatorInAmount) {
                            onEvaluate()
                        } else {
                            onSave()
                        }
                    },
                    enabled = if (hasOperatorInAmount) !isSaving else (!isSaving && isSaveEnabled),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasOperatorInAmount) MaterialTheme.colorScheme.tertiary else saveButtonColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    if (hasOperatorInAmount) {
                        Text(
                            text = "=",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = saveButtonText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        } else {
            // Clean Single Primary Action Button
            Button(
                onClick = {
                    if (hasOperatorInAmount) {
                        onEvaluate()
                    } else {
                        onSave()
                    }
                },
                enabled = if (hasOperatorInAmount) !isSaving else (!isSaving && isSaveEnabled),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasOperatorInAmount) MaterialTheme.colorScheme.tertiary else saveButtonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (hasOperatorInAmount) {
                    Text(
                        text = "=",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = saveButtonText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanNumpadKey(
    key: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (key == "⌫") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = key,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
        }
    }
}
