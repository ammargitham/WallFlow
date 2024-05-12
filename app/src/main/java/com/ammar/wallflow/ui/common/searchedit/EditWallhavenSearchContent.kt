package com.ammar.wallflow.ui.common.searchedit

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenSorting
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
internal fun EditWallhavenSearchContent(
    modifier: Modifier = Modifier,
    search: WallhavenSearch = WallhavenSearch(),
    showQueryField: Boolean = true,
    showNSFW: Boolean = false,
    localResolution: IntSize = IntSize(1, 1),
    onChange: (WallhavenSearch) -> Unit = {},
    onMinResAddCustomResClick: () -> Unit = {},
    onResolutionsAddCustomResClick: () -> Unit = {},
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
            showNSFW = showNSFW,
            onChange = { onChange(search.copy(filters = search.filters.copy(purity = it))) },
        )
        WallhavenSortingFilter(
            sorting = search.filters.sorting,
            onChange = { onChange(search.copy(filters = search.filters.copy(sorting = it))) },
        )
        AnimatedVisibility(search.filters.sorting == WallhavenSorting.TOPLIST) {
            TopRangeFilter(
                topRange = search.filters.topRange,
                onChange = { onChange(search.copy(filters = search.filters.copy(topRange = it))) },
            )
        }
        OrderFilter(
            order = search.filters.order,
            onChange = { onChange(search.copy(filters = search.filters.copy(order = it))) },
        )
        MinResolutionFilter(
            modifier = Modifier.wrapContentHeight(),
            localResolution = localResolution,
            resolution = search.filters.atleast,
            onChange = { onChange(search.copy(filters = search.filters.copy(atleast = it))) },
            onAddCustomResolutionClick = onMinResAddCustomResClick,
        )
        ResolutionsFilter(
            modifier = Modifier.wrapContentHeight(),
            localResolution = localResolution,
            resolutions = search.filters.resolutions,
            onChange = { onChange(search.copy(filters = search.filters.copy(resolutions = it))) },
            onAddCustomResolutionClick = onResolutionsAddCustomResClick,
        )
        RatioFilter(
            ratios = search.filters.ratios,
            onChange = { onChange(search.copy(filters = search.filters.copy(ratios = it))) },
        )
    }
}

@Preview(device = "spec:width=1080px,height=3500px,dpi=440")
@Preview(
    device = "spec:width=1080px,height=3500px,dpi=440",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewEditWallhavenSearchContent() {
    WallFlowTheme {
        Surface {
            EditSearchContent(
                modifier = Modifier.padding(16.dp),
                localResolution = IntSize(0, 0),
                search = WallhavenSearch(
                    filters = WallhavenFilters(
                        sorting = WallhavenSorting.TOPLIST,
                    ),
                ),
            )
        }
    }
}
