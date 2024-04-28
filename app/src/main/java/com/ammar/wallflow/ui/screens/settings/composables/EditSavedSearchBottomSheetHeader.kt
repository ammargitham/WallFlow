package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun EditSavedSearchBottomSheetHeader(
    name: String = "",
    saveEnabled: Boolean = true,
    nameHasError: Boolean = false,
    onSaveClick: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 22.dp,
                end = 22.dp,
                bottom = 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.edit_saved_search),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.requiredWidth(8.dp))
        Button(
            enabled = saveEnabled,
            onClick = onSaveClick,
        ) {
            Text(stringResource(R.string.save))
        }
    }
    HorizontalDivider(modifier = Modifier.fillMaxWidth())
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 16.dp,
                start = 22.dp,
                end = 22.dp,
            ),
        label = { Text(text = stringResource(R.string.name)) },
        supportingText = if (nameHasError) {
            {
                Text(
                    text = stringResource(
                        if (name.isBlank()) {
                            R.string.name_cannot_be_empty
                        } else {
                            R.string.name_already_used
                        },
                    ),
                )
            }
        } else {
            null
        },
        value = name,
        singleLine = true,
        isError = nameHasError,
        onValueChange = onNameChange,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEditSearchBottomSheetHeader() {
    WallFlowTheme {
        Surface {
            Column {
                EditSavedSearchBottomSheetHeader()
            }
        }
    }
}
