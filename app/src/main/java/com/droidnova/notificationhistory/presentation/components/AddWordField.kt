package com.droidnova.notificationhistory.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R

/**
 * A single rounded field with its own "add" button; Enter on the keyboard adds too. While
 * [locked] (a Premium limit), the button shows a lock and adding calls [onLocked] instead.
 */
@Composable
fun AddWordField(
    placeholder: String,
    leadingIcon: ImageVector,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    var input by remember { mutableStateOf("") }
    val submit = {
        when {
            locked -> onLocked()
            input.isNotBlank() -> {
                onAdd(input.trim())
                input = ""
            }
        }
    }
    TextField(
        value = input,
        onValueChange = { input = it },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(50),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = tintedCardColor(),
            unfocusedContainerColor = tintedCardColor(),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        trailingIcon = {
            FilledIconButton(
                onClick = submit,
                enabled = locked || input.isNotBlank(),
                modifier = Modifier.padding(end = 4.dp),
                colors = IconButtonDefaults.filledIconButtonColors()
            ) {
                Icon(
                    if (locked) Icons.Filled.Lock else Icons.Filled.Add,
                    contentDescription = stringResource(R.string.btn_add)
                )
            }
        }
    )
}
