package com.ammar.havenwalls.data.preferences

import com.ammar.havenwalls.model.SearchQuery
import com.ammar.havenwalls.model.Sorting
import com.ammar.havenwalls.model.TopRange
import com.ammar.havenwalls.model.Search
import org.tensorflow.lite.task.core.ComputeSettings.Delegate

data class AppPreferences(
    val wallhavenApiKey: String = "",
    val homeSearch: Search = Search(
        filters = SearchQuery(
            sorting = Sorting.TOPLIST,
            topRange = TopRange.ONE_DAY,
        )
    ),
    val blurSketchy: Boolean = false,
    val blurNsfw: Boolean = false,
    val objectDetectionPreferences: ObjectDetectionPreferences = ObjectDetectionPreferences(),
)

data class ObjectDetectionPreferences(
    val enabled: Boolean = true,
    val delegate: Delegate = Delegate.GPU,
    val modelId: Long = 0,
)
