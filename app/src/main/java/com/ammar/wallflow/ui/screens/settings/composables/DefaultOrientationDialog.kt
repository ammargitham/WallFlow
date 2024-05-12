package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.DeviceOrientation
import com.ammar.wallflow.model.MutableDeviceOrientationSaver
import com.ammar.wallflow.ui.common.RadioButtonListItem
import com.ammar.wallflow.ui.common.UnpaddedAlertDialogContent
import com.ammar.wallflow.ui.theme.WallFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultOrientationDialog(
    modifier: Modifier = Modifier,
    defaultOrientation: DeviceOrientation = DeviceOrientation.Vertical,
    currentOrientation: DeviceOrientation = DeviceOrientation.Vertical,
    onSaveClick: (DeviceOrientation) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var localOrientation by rememberSaveable(
        saver = MutableDeviceOrientationSaver,
    ) { mutableStateOf(defaultOrientation) }

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = {
                Text(text = stringResource(R.string.default_orientation))
            },
            text = {
                Column {
                    RadioButtonListItem(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        selected = localOrientation == DeviceOrientation.Vertical,
                        label = {
                            Text(
                                text = getOrientationString(
                                    orientation = DeviceOrientation.Vertical,
                                    current = currentOrientation,
                                ),
                            )
                        },
                        onClick = { localOrientation = DeviceOrientation.Vertical },
                    )
                    RadioButtonListItem(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        selected = localOrientation == DeviceOrientation.Horizontal,
                        label = {
                            Text(
                                text = getOrientationString(
                                    orientation = DeviceOrientation.Horizontal,
                                    current = currentOrientation,
                                ),
                            )
                        },
                        onClick = { localOrientation = DeviceOrientation.Horizontal },
                    )
                }
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(
                        enabled = localOrientation != defaultOrientation,
                        onClick = { onSaveClick(localOrientation) },
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDefaultOrientationDialog() {
    WallFlowTheme {
        Surface {
            DefaultOrientationDialog()
        }
    }
}
