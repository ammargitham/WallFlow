package com.ammar.havenwalls.extensions

import androidx.navigation.NavController
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.ui.destinations.SearchScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.navigate

fun NavController.search(search: Search) {
    this.navigate(SearchScreenDestination(search = search))
}

fun DestinationsNavigator.search(search: Search) {
    this.navigate(SearchScreenDestination(search = search))
}
