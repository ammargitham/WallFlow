package com.ammar.wallflow.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry

@Composable
inline fun <reified VM : ViewModel> hiltViewModel(key: String): VM {
    val viewModelStoreOwner =
        if (checkNotNull(LocalViewModelStoreOwner.current) is NavBackStackEntry) {
            checkNotNull(LocalViewModelStoreOwner.current) { "ViewModelStoreOwner is null" }
        } else {
            null
        }

    return viewModel(
        key = key,
        factory = if (viewModelStoreOwner is NavBackStackEntry) {
            HiltViewModelFactory(
                LocalContext.current,
                viewModelStoreOwner,
            )
        } else {
            null
        },
    )
}
