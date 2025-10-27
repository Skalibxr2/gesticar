package com.hans.gesticar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp


@Composable
fun DropdownTextField(
    value: String,
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: (@Composable () -> Unit)? = null,
    dropdownContent: @Composable ColumnScope.(closeMenu: () -> Unit) -> Unit
) {
    var menuWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = placeholder,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded && enabled) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            },
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    menuWidth = with(density) { coordinates.size.width.toFloat().toDp() }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    if (enabled) {
                        onExpandedChange(!expanded)
                    }
                }
        )
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.width(menuWidth)
        ) {
            dropdownContent(onDismissRequest)
        }
    }
}
