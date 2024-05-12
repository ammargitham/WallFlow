package com.ammar.wallflow.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainDestinationBox(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    hasSearchBar: Boolean = false,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    Box(
        modifier = modifier
            .then(
                if (!hasSearchBar) {
                    Modifier.windowInsetsPadding(topWindowInsets)
                } else {
                    Modifier
                },
            )
            .then(
                if (isExpanded) {
                    Modifier.windowInsetsPadding(bottomWindowInsets)
                } else {
                    Modifier
                },
            )
            .fillMaxSize(),
        content = {
            this.content(
                getPaddingValues(
                    isExpanded = isExpanded,
                    hasSearchBar = hasSearchBar,
                ),
            )
        },
    )
}

@Composable
private fun getPaddingValues(
    isExpanded: Boolean = false,
    hasSearchBar: Boolean = false,
) = PaddingValues(
    start = if (isExpanded) 0.dp else 8.dp,
    end = if (isExpanded) 0.dp else 8.dp,
    top = if (hasSearchBar) {
        SearchBar.Defaults.height
    } else {
        if (isExpanded) {
            16.dp
        } else {
            8.dp
        }
    },
    bottom = if (isExpanded) 16.dp else 8.dp,
)
