package com.ammar.havenwalls.data.preferences

import com.ammar.havenwalls.data.common.SearchQuery
import com.ammar.havenwalls.data.common.Sorting
import com.ammar.havenwalls.data.common.TopRange
import org.tensorflow.lite.task.core.ComputeSettings.Delegate

data class AppPreferences(
    val wallhavenApiKey: String = "",
    val homeSearchQuery: SearchQuery = SearchQuery(
        sorting = Sorting.TOPLIST,
        topRange = TopRange.ONE_DAY,
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
