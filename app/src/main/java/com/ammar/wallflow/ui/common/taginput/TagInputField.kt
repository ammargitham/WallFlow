package com.ammar.wallflow.ui.common.taginput

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
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
    tagFromInputString: ((String) -> T)? = null,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    tags: Set<T> = emptySet(),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    showTagClearAction: Boolean = true,
    isError: Boolean = false,
    separatorRegex: Regex = TAG_INPUT_DEFAULT_SEP_REGEX,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    getTagString: (tag: T) -> String = { "#$it" },
    onAddTag: (tag: T) -> Unit = {},
    onRemoveTag: (tag: T) -> Unit = {},
    validateTag: (tag: T) -> Boolean = { true },
    getLeadingIcon: (tag: T) -> @Composable (() -> Unit)? = { null },
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
    var selectLastTag by remember { mutableStateOf(false) }
    val localIsError = remember(isError, tags) {
        isError || !tags.all(validateTag)
    }
    val flowRowScrollState = rememberScrollState()

    LaunchedEffect(flowRowScrollState.maxValue) {
        flowRowScrollState.animateScrollTo(flowRowScrollState.maxValue)
    }

    Box(
        modifier = modifier,
    ) {
        BasicTextField(
            modifier = Modifier
                .onPreviewKeyEvent {
                    if (!showTagClearAction) {
                        return@onPreviewKeyEvent true
                    }
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
                if (!it.contains(separatorRegex)) {
                    fieldValue = it
                    return@BasicTextField
                }
                val parts = it.split(separatorRegex)
                val tagString = parts[0].trimAll()
                if (tagString.isBlank()) return@BasicTextField
                if (tagFromInputString != null) {
                    onAddTag(tagFromInputString(tagString))
                }
                fieldValue = ""
            },
            singleLine = singleLine,
            maxLines = maxLines,
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .let {
                                    if (maxLines != Int.MAX_VALUE) {
                                        it
                                            .heightIn(max = 200.dp)
                                            .verticalScroll(state = flowRowScrollState)
                                    } else {
                                        it
                                    }
                                },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            tags.mapIndexed { i, tag ->
                                val isLast = i == tags.size - 1
                                val selected = isLast && selectLastTag
                                val isValid = validateTag(tag)
                                ClearableChip(
                                    isError = !isValid,
                                    leadingIcon = if (isValid) {
                                        getLeadingIcon(tag)
                                    } else {
                                        {
                                            Icon(
                                                painter = painterResource(
                                                    R.drawable.baseline_error_24,
                                                ),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = getTagString(tag),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
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
                            if (!readOnly) {
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
                        }
                    },
                    enabled = enabled,
                    singleLine = false,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    label = label,
                    placeholder = placeholder,
                    trailingIcon = trailingIcon,
                    isError = localIsError,
                    contentPadding = OutlinedTextFieldDefaults.contentPadding(
                        top = if (tags.isNotEmpty()) 0.dp else 16.dp,
                        bottom = if (tags.isNotEmpty()) 0.dp else 16.dp,
                    ),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = enabled,
                            isError = localIsError,
                            interactionSource = interactionSource,
                            colors = colors,
                        )
                    },
                )
            },
        )
    }
}

private val TAG_INPUT_DEFAULT_SEP_REGEX = "[,\\r\\n]+".toRegex()

private class TagsParameterProvider : CollectionPreviewParameterProvider<
    Set<Pair<String, Boolean>>,
    >(
    listOf(
        emptySet(),
        setOf("test" to true, "test1" to false),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTagInputField(
    @PreviewParameter(TagsParameterProvider::class) tags: Set<Pair<String, Boolean>>,
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
                getTagString = { "#${it.first}" },
                tagFromInputString = { it to true },
                validateTag = { it.second },
                onAddTag = { localTags = localTags + it },
                onRemoveTag = { localTags = localTags - it },
            )
        }
    }
}
