package com.ammar.wallflow.extensions

import androidx.navigation.NavController
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.ui.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.navigate

fun NavController.search(search: Search) = this.navigate(
    HomeScreenDestination(search = search),
)

fun DestinationsNavigator.search(search: Search) = this.navigate(
    HomeScreenDestination(search = search),
)

// const val KEY_DEEP_LINK_INTENT = "android-support-nav:controller:deepLinkIntent"

// /**
//  * Return a [Map] object with a map of the query params from the [NavBackStackEntry].
//  */
// fun NavBackStackEntry.getQueryArguments(): Map<String, String?> {
//     val intent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//         this.arguments?.getParcelable(KEY_DEEP_LINK_INTENT, Intent::class.java)
//     } else {
//         this.arguments?.getParcelable(KEY_DEEP_LINK_INTENT) as Intent?
//     }
//     val data = intent?.data
//
//     val result = data?.queryParameterNames?.associate { param ->
//         param to data.getQueryParameter(param)
//     }
//
//     return result ?: emptyMap()
// }
