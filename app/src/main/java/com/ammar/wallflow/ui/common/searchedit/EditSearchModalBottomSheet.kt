package com.ammar.wallflow.ui.common.searchedit

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.ui.common.AdaptiveBottomSheet
import com.ammar.wallflow.ui.common.AdaptiveBottomSheetState
import com.ammar.wallflow.ui.common.rememberAdaptiveBottomSheetState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSearchModalBottomSheet(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    state: AdaptiveBottomSheetState = rememberAdaptiveBottomSheetState(),
    search: Search = WallhavenSearch(),
    header: @Composable (ColumnScope.() -> Unit)? = null,
    showNSFW: Boolean = false,
    showQueryField: Boolean = true,
    onChange: (Search) -> Unit = {},
    onErrorStateChange: (Boolean) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val imePadding = WindowInsets.ime.getBottom(LocalDensity.current).toDp()
    val scrollState = rememberScrollState()
    var showMinResAddCustomResDialog by rememberSaveable { mutableStateOf(false) }
    var showResolutionsAddCustomResDialog by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun expandSheet() {
        // compose bug lowers the bottom sheet when dialog opens
        // need to re-expand it after dialog closes
        coroutineScope.launch {
            delay(100)
            state.expand()
        }
    }

    AdaptiveBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = state,
    ) {
        header?.invoke(this)
        EditSearchContent(
            modifier = contentModifier
                .verticalScroll(scrollState)
                .padding(
                    top = 22.dp,
                    start = 22.dp,
                    end = 22.dp,
                    bottom = imePadding + 44.dp,
                ),
            search = search,
            showQueryField = showQueryField,
            showNSFW = showNSFW,
            onChange = onChange,
            onErrorStateChange = onErrorStateChange,
            onMinResAddCustomResClick = { showMinResAddCustomResDialog = true },
            onResolutionsAddCustomResClick = { showResolutionsAddCustomResDialog = true },
        )
    }

    if (
        search is WallhavenSearch &&
        showMinResAddCustomResDialog
    ) {
        CustomResolutionDialog(
            onSave = {
                onChange(search.copy(filters = search.filters.copy(atleast = it)))
                showMinResAddCustomResDialog = false
                expandSheet()
            },
            onDismissRequest = {
                showMinResAddCustomResDialog = false
                expandSheet()
            },
        )
    }

    if (
        search is WallhavenSearch &&
        showResolutionsAddCustomResDialog
    ) {
        CustomResolutionDialog(
            onSave = {
                onChange(
                    search.copy(
                        filters = search.filters.copy(
                            resolutions = search.filters.resolutions + it,
                        ),
                    ),
                )
                showResolutionsAddCustomResDialog = false
                expandSheet()
            },
            onDismissRequest = {
                showResolutionsAddCustomResDialog = false
                expandSheet()
            },
        )
    }
}

@Composable
fun EditSearchContent(
    modifier: Modifier = Modifier,
    search: Search = WallhavenSearch(),
    showQueryField: Boolean = true,
    showNSFW: Boolean = false,
    onChange: (Search) -> Unit = {},
    onErrorStateChange: (Boolean) -> Unit = {},
    onMinResAddCustomResClick: () -> Unit = {},
    onResolutionsAddCustomResClick: () -> Unit = {},
) {
    when (search) {
        is WallhavenSearch -> EditWallhavenSearchContent(
            modifier = modifier,
            showQueryField = showQueryField,
            search = search,
            showNSFW = showNSFW,
            onChange = onChange,
            onMinResAddCustomResClick = onMinResAddCustomResClick,
            onResolutionsAddCustomResClick = onResolutionsAddCustomResClick,
        )
        is RedditSearch -> EditRedditSearchContent(
            modifier = modifier,
            search = search,
            showQueryField = showQueryField,
            onChange = onChange,
            onErrorStateChange = onErrorStateChange,
        )
    }
}
