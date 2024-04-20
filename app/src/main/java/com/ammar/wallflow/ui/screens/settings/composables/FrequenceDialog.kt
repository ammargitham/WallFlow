package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.defaultAutoWallpaperFreq
import com.ammar.wallflow.ui.common.UnpaddedAlertDialogContent
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimePeriod

private fun DateTimePeriod.freqVal() = if (hours != 0 && minutes == 0) {
    hours
} else {
    hours * 60 + minutes
}

private fun DateTimePeriod.chronoUnit() = if (hours != 0 && minutes == 0) {
    FrequencyChronoUnit.HOURS
} else {
    FrequencyChronoUnit.MINUTES
}

private fun TextFieldValue.toDateTimePeriod(
    selectedChronoUnit: FrequencyChronoUnit,
): DateTimePeriod {
    val inputInt = text.toIntOrNull() ?: 0
    return when (selectedChronoUnit) {
        FrequencyChronoUnit.HOURS -> DateTimePeriod(hours = inputInt)
        FrequencyChronoUnit.MINUTES -> DateTimePeriod(minutes = inputInt)
    }
}

private fun validate(
    textFieldValue: TextFieldValue,
    selectedChronoUnit: FrequencyChronoUnit,
): Boolean {
    val freq = textFieldValue.toDateTimePeriod(selectedChronoUnit)
    val minutes = freq.hours * 60 + freq.minutes
    return minutes < 15
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencyDialog(
    modifier: Modifier = Modifier,
    useSameFreq: Boolean = true,
    frequency: DateTimePeriod = defaultAutoWallpaperFreq,
    lsFrequency: DateTimePeriod = defaultAutoWallpaperFreq,
    onSaveClick: (
        useSameFreq: Boolean,
        freq: DateTimePeriod,
        lsFreq: DateTimePeriod,
    ) -> Unit = { _, _, _ -> },
    onDismissRequest: () -> Unit = {},
) {
    var localUseSameFreq by rememberSaveable(useSameFreq) {
        mutableStateOf(useSameFreq)
    }
    var freqVal by rememberSaveable(
        frequency,
        stateSaver = TextFieldValue.Saver,
    ) {
        mutableStateOf(TextFieldValue(frequency.freqVal().toString()))
    }
    var lsFreqVal by rememberSaveable(
        lsFrequency,
        stateSaver = TextFieldValue.Saver,
    ) {
        mutableStateOf(TextFieldValue(lsFrequency.freqVal().toString()))
    }

    var selectedChronoUnit by rememberSaveable(frequency) {
        mutableStateOf(frequency.chronoUnit())
    }
    var selectedLsChronoUnit by rememberSaveable(lsFrequency) {
        mutableStateOf(lsFrequency.chronoUnit())
    }

    val hasMinError by rememberSaveable(freqVal, selectedChronoUnit) {
        mutableStateOf(validate(freqVal, selectedChronoUnit))
    }
    val lsHasMinError by rememberSaveable(lsFreqVal, selectedLsChronoUnit) {
        mutableStateOf(validate(lsFreqVal, selectedLsChronoUnit))
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // put cursor at end of value
        freqVal = freqVal.copy(selection = TextRange(freqVal.text.length))
        delay(200)
        focusRequester.requestFocus()
    }

    fun onSave() {
        if (hasMinError || lsHasMinError) {
            return
        }
        val newFreq = freqVal.toDateTimePeriod(selectedChronoUnit)
        val newLsFreq = lsFreqVal.toDateTimePeriod(selectedLsChronoUnit)
        onSaveClick(localUseSameFreq, newFreq, newLsFreq)
    }

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.frequency)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ListItem(
                        modifier = Modifier.clickable {
                            localUseSameFreq = !localUseSameFreq
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.use_same_freq_homescreen_lockscreen),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = localUseSameFreq,
                                onCheckedChange = { localUseSameFreq = it },
                            )
                        },
                    )
                    FrequencyInputField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        label = stringResource(R.string.home_screen),
                        showLabel = !localUseSameFreq,
                        focusRequester = focusRequester,
                        freqVal = freqVal,
                        selectedChronoUnit = selectedChronoUnit,
                        hasMinError = hasMinError,
                        keyboardActions = KeyboardActions(
                            onDone = { onSave() },
                        ),
                        onValueChange = { freqVal = it },
                        onChronoUnitChange = { selectedChronoUnit = it },
                    )
                    AnimatedVisibility(!localUseSameFreq) {
                        FrequencyInputField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            label = stringResource(R.string.lock_screen),
                            showLabel = true,
                            focusRequester = focusRequester,
                            freqVal = lsFreqVal,
                            selectedChronoUnit = selectedLsChronoUnit,
                            hasMinError = lsHasMinError,
                            keyboardActions = KeyboardActions(
                                onDone = { onSave() },
                            ),
                            onValueChange = { lsFreqVal = it },
                            onChronoUnitChange = { selectedLsChronoUnit = it },
                        )
                    }
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
                        enabled = !hasMinError && !lsHasMinError,
                        onClick = ::onSave,
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FrequencyInputField(
    label: String,
    showLabel: Boolean,
    focusRequester: FocusRequester,
    freqVal: TextFieldValue,
    hasMinError: Boolean,
    selectedChronoUnit: FrequencyChronoUnit,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    onValueChange: (TextFieldValue) -> Unit = {},
    onChronoUnitChange: (FrequencyChronoUnit) -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        modifier = modifier.focusRequester(focusRequester),
        label = if (showLabel) {
            {
                Text(text = label)
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = keyboardActions,
        value = freqVal,
        isError = hasMinError,
        supportingText = if (hasMinError) {
            {
                Text(text = stringResource(R.string.frequency_min_val_error))
            }
        } else {
            null
        },
        onValueChange = { newValue ->
            val newText = newValue.text
            if (newText.isEmpty()) {
                onValueChange(newValue)
                return@OutlinedTextField
            }
            val newTextInt = newText.toIntOrNull() ?: 0
            if (newTextInt >= 0) {
                onValueChange(newValue)
            }
        },
        suffix = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                Row {
                    Text(
                        modifier = Modifier.menuAnchor(
                            type = MenuAnchorType.PrimaryNotEditable,
                        ),
                        text = chronoUnitString(selectedChronoUnit),
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
                ExposedDropdownMenu(
                    modifier = Modifier.width(IntrinsicSize.Min),
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    FrequencyChronoUnit.entries.forEach { chronoUnit ->
                        DropdownMenuItem(
                            text = { Text(text = chronoUnitString(chronoUnit)) },
                            onClick = {
                                onChronoUnitChange(chronoUnit)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
        },
    )
}

private data class FreqDialogParams(
    val useSameFreq: Boolean = true,
    val frequency: DateTimePeriod = defaultAutoWallpaperFreq,
    val lsFrequency: DateTimePeriod = defaultAutoWallpaperFreq,
)

private class FreqDialogPPP : CollectionPreviewParameterProvider<FreqDialogParams>(
    listOf(
        FreqDialogParams(
            frequency = DateTimePeriod.parse("PT60M"),
        ),
        FreqDialogParams(
            useSameFreq = false,
            frequency = DateTimePeriod.parse("PT60M"),
        ),
        FreqDialogParams(
            useSameFreq = false,
            frequency = DateTimePeriod.parse("PT60M"),
            lsFrequency = DateTimePeriod.parse("PT1H20M"),
        ),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFrequencyDialog(
    @PreviewParameter(FreqDialogPPP::class) params: FreqDialogParams,
) {
    WallFlowTheme {
        Surface {
            FrequencyDialog(
                useSameFreq = params.useSameFreq,
                frequency = params.frequency,
                lsFrequency = params.lsFrequency,
                onSaveClick = { useSameFreq, freq, lsFreq ->
                    Log.d(
                        "PreviewFrequencyDialog",
                        "useSameFreq: $useSameFreq, " +
                            "newFreq: $freq, " +
                            "lsNewFreq: $lsFreq",
                    )
                },
            )
        }
    }
}
