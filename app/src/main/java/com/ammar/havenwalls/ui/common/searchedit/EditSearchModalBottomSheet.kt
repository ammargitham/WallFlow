package com.ammar.havenwalls.ui.common.searchedit

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.havenwalls.R
import com.ammar.havenwalls.extensions.toDp
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.SearchQuery
import com.ammar.havenwalls.model.Sorting
import com.ammar.havenwalls.ui.theme.HavenWallsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSearchModalBottomSheet(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    state: SheetState = rememberModalBottomSheetState(),
    search: Search = Search(),
    header: @Composable (ColumnScope.() -> Unit)? = null,
    onChange: (Search) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val imePadding = WindowInsets.ime.getBottom(LocalDensity.current).toDp()
    val scrollState = rememberScrollState()

    ModalBottomSheet(
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
            onChange = onChange,
        )
    }
}

@Composable
fun EditSearchContent(
    modifier: Modifier = Modifier,
    search: Search = Search(),
    showQueryField: Boolean = true,
    onChange: (Search) -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showQueryField) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.query)) },
                value = search.query,
                onValueChange = { onChange(search.copy(query = it)) },
            )
        }
        IncludedTagsFilter(
            tags = search.filters.includedTags,
            onChange = { onChange(search.copy(filters = search.filters.copy(includedTags = it))) },
        )
        ExcludedTagsFilter(
            tags = search.filters.excludedTags,
            onChange = { onChange(search.copy(filters = search.filters.copy(excludedTags = it))) },
        )
        CategoriesFilter(
            categories = search.filters.categories,
            onChange = { onChange(search.copy(filters = search.filters.copy(categories = it))) },
        )
        PurityFilter(
            purities = search.filters.purity,
            onChange = { onChange(search.copy(filters = search.filters.copy(purity = it))) },
        )
        SortingFilter(
            sorting = search.filters.sorting,
            onChange = { onChange(search.copy(filters = search.filters.copy(sorting = it))) },
        )
        AnimatedVisibility(search.filters.sorting == Sorting.TOPLIST) {
            TopRangeFilter(
                topRange = search.filters.topRange,
                onChange = { onChange(search.copy(filters = search.filters.copy(topRange = it))) },
            )
        }
        OrderFilter(
            order = search.filters.order,
            onChange = { onChange(search.copy(filters = search.filters.copy(order = it))) },
        )
        ResolutionsFilter(
            resolutions = search.filters.resolutions,
            onChange = { onChange(search.copy(filters = search.filters.copy(resolutions = it))) },
        )
        RatioFilter(
            ratios = search.filters.ratios,
            onChange = { onChange(search.copy(filters = search.filters.copy(ratios = it))) }
        )
    }
}

@Preview(device = "spec:width=1080px,height=3500px,dpi=440")
@Preview(
    device = "spec:width=1080px,height=3500px,dpi=440",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewFiltersContent() {
    HavenWallsTheme {
        Surface {
            EditSearchContent(
                modifier = Modifier.padding(16.dp),
                search = Search(
                    filters = SearchQuery(
                        sorting = Sorting.TOPLIST,
                    )
                )
            )
        }
    }
}
