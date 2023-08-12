package com.ammar.wallflow.ui.common.taginput

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.ui.common.ClearableChip
import com.ammar.wallflow.ui.theme.WallFlowTheme

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun <T> TagInputField(
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    tags: Set<T> = emptySet(),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    showTagClearAction: Boolean = true,
    tagFromInputString: (String) -> T,
    getTagString: (tag: T) -> String = { "#$it" },
    onAddTag: (tag: T) -> Unit = {},
    onRemoveTag: (tag: T) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val localStyle = LocalTextStyle.current
    val mergedStyle = localStyle.merge(TextStyle(color = LocalContentColor.current))
    var fieldValue by rememberSaveable { mutableStateOf("") }
    val boxValue = remember(tags, fieldValue) {
        if (tags.isEmpty()) {
            return@remember fieldValue
        }
        tags.joinToString(",")
    }
    val regex = remember { "[,\\r\\n]+".toRegex() }
    var selectLastTag by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
    ) {
        BasicTextField(
            modifier = Modifier
                .onPreviewKeyEvent {
                    if (it.key != Key.Backspace ||
                        fieldValue.isNotEmpty() ||
                        tags.isEmpty() ||
                        // ACTION_UP not called for physical keyboard
                        // hence we perform action on DOWN and ignore UP events
                        it.nativeKeyEvent.action == NativeKeyEvent.ACTION_UP
                    ) {
                        return@onPreviewKeyEvent false
                    }
                    if (!selectLastTag) {
                        selectLastTag = true
                        return@onPreviewKeyEvent true
                    }
                    onRemoveTag(tags.last())
                    selectLastTag = false
                    return@onPreviewKeyEvent true
                }
                .semantics(mergeDescendants = true) {}
                .padding(top = 8.dp),
            value = fieldValue,
            onValueChange = {
                if (!it.contains(regex)) {
                    fieldValue = it
                    return@BasicTextField
                }
                val parts = it.split(regex)
                val tagString = parts[0].trimAll()
                if (tagString.isBlank()) return@BasicTextField
                onAddTag(tagFromInputString(tagString))
                fieldValue = ""
            },
            readOnly = readOnly,
            textStyle = mergedStyle,
            cursorBrush = SolidColor(
                if (selectLastTag) Color.Transparent else MaterialTheme.colorScheme.primary,
            ),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = boxValue,
                    innerTextField = {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            tags.mapIndexed { i, tag ->
                                val isLast = i == tags.size - 1
                                val selected = isLast && selectLastTag

                                ClearableChip(
                                    label = { Text(text = getTagString(tag)) },
                                    selected = selected,
                                    showClearIcon = showTagClearAction,
                                    onClear = {
                                        onRemoveTag(tag)
                                        if (selected) {
                                            selectLastTag = false
                                        }
                                    },
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .padding(
                                        top = if (tags.isEmpty()) 0.dp else 12.dp,
                                        bottom = if (tags.isEmpty()) 0.dp else 12.dp,
                                    )
                                    .width(IntrinsicSize.Min)
                                    .widthIn(min = 5.dp),
                            ) {
                                innerTextField()
                            }
                        }
                    },
                    enabled = enabled,
                    singleLine = false,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    label = label,
                    placeholder = placeholder,
                    trailingIcon = trailingIcon,
                    contentPadding = OutlinedTextFieldDefaults.contentPadding(
                        top = if (tags.isNotEmpty()) 0.dp else 16.dp,
                        bottom = if (tags.isNotEmpty()) 0.dp else 16.dp,
                    ),
                    container = {
                        OutlinedTextFieldDefaults.ContainerBox(
                            enabled = enabled,
                            isError = false,
                            interactionSource = interactionSource,
                            colors = colors,
                        )
                    },
                )
            },
        )
    }
}

private class TagsParameterProvider : CollectionPreviewParameterProvider<Set<String>>(
    listOf(
        emptySet(),
        setOf("test", "test1"),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTagInputField(
    @PreviewParameter(TagsParameterProvider::class) tags: Set<String>,
) {
    var localTags by remember { mutableStateOf(tags) }

    WallFlowTheme {
        Surface {
            TagInputField(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                label = { Text(text = "Chip Input") },
                placeholder = { Text(text = "Placeholder") },
                tags = localTags,
                tagFromInputString = { it },
                onAddTag = { localTags = localTags + it },
                onRemoveTag = { localTags = localTags - it },
            )
        }
    }
}
