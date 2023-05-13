package com.ammar.havenwalls.ui.common.mainsearch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ammar.havenwalls.R
import com.ammar.havenwalls.extensions.produceState
import com.ammar.havenwalls.extensions.trimAll
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.TagSearchMeta
import com.ammar.havenwalls.model.UploaderSearchMeta
import com.ammar.havenwalls.ui.common.LocalSystemBarsController
import com.ammar.havenwalls.ui.common.SearchBar
import com.ammar.havenwalls.ui.common.TagChip
import com.ammar.havenwalls.ui.common.UploaderChip
import com.ammar.havenwalls.ui.common.WallpaperFiltersDialogContent
import com.ammar.havenwalls.ui.common.bottombar.BottomBarState
import com.ammar.havenwalls.ui.common.bottombar.LocalBottomBarController
import com.ammar.havenwalls.ui.home.SearchBarFiltersToggle

@Composable
fun MainSearchBar(
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
) {
    val viewModel: MainSearchViewModel = hiltViewModel()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val uiState = lifecycle.produceState(
        viewModel = viewModel,
        initialValue = MainSearchUiState()
    )
    val controller = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val systemBarsController = LocalSystemBarsController.current

    val controllerState by controller.state
    val statusBarSemiTransparentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    val doSearch = remember {
        fun(s: Search) {
            viewModel.onSearch(s)
            controllerState.onSearch(s)
        }
    }
    val placeholder: @Composable () -> Unit = remember { { Text(text = "Search") } }

    LaunchedEffect(controllerState.search) {
        viewModel.setSearch(controllerState.search)
    }

    val query = when (uiState.search.meta) {
        is TagSearchMeta, is UploaderSearchMeta -> {
            if (uiState.active) uiState.search.query else ""
        }
        else -> uiState.search.query
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = controllerState.visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        SearchBar(
            placeholder = when {
                uiState.active -> placeholder
                else -> when (uiState.search.meta) {
                    is TagSearchMeta, is UploaderSearchMeta -> null
                    else -> placeholder
                }
            },
            query = query,
            suggestions = uiState.suggestions,
            onQueryChange = viewModel::onQueryChange,
            onBackClick = if (showBackButton) onBackClick else null,
            onSearch = {
                if (it.isBlank()) {
                    return@SearchBar
                }
                val search = if (it.trimAll() == query) {
                    // keep current search data if query hasn't changed
                    // this allows to keep meta data if only filters were changed
                    uiState.search.copy(
                        filters = uiState.search.filters,
                    )
                } else {
                    Search(
                        query = it,
                        filters = uiState.search.filters,
                    )
                }
                doSearch(search)
            },
            onSuggestionClick = { doSearch(it.value) },
            onSuggestionInsert = { viewModel.setSearch(it.value) },
            onSuggestionDeleteRequest = { viewModel.setShowDeleteRequest(it.value) },
            onActiveChange = { a ->
                viewModel.setActive(a)
                viewModel.setShowFilters(false)
                systemBarsController.update {
                    it.copy(
                        statusBarColor = if (a) statusBarSemiTransparentColor else Color.Unspecified,
                    )
                }
                bottomBarController.update {
                    BottomBarState(
                        visible = !a,
                    )
                }
                controllerState.onActiveChange(a)
            },
            extraLeadingContent = when {
                uiState.active -> null
                else -> when (uiState.search.meta) {
                    is TagSearchMeta -> {
                        { TagChip(tag = uiState.search.meta.tag) }
                    }
                    is UploaderSearchMeta -> {
                        { UploaderChip(uploader = uiState.search.meta.uploader) }
                    }
                    else -> null
                }
            },
            trailingIcon = {
                Crossfade(uiState.active) {
                    if (it) {
                        SearchBarFiltersToggle(
                            checked = uiState.showFilters,
                            onCheckedChange = viewModel::setShowFilters,
                        )
                        return@Crossfade
                    }
                    controllerState.overflowIcon?.invoke()
                }
            },
            extraContent = {
                AnimatedVisibility(
                    modifier = Modifier.clipToBounds(),
                    visible = uiState.showFilters,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it }),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        WallpaperFiltersDialogContent(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .windowInsetsPadding(WindowInsets.ime)
                                .padding(16.dp),
                            searchQuery = uiState.search.filters,
                            onChange = {
                                viewModel.setSearch(
                                    uiState.search.copy(
                                        filters = it,
                                    )
                                )
                            },
                        )
                    }
                }
            },
        )
    }
    uiState.showDeleteRequestConfirmation?.run {
        AlertDialog(
            title = { Text(text = this.query) },
            text = { Text(text = "Remove from history?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteSearch(uiState.showDeleteRequestConfirmation) },
                ) {
                    Text(text = "Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.setShowDeleteRequest(null) },
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            onDismissRequest = { viewModel.setShowDeleteRequest(null) },
        )
    }
}
