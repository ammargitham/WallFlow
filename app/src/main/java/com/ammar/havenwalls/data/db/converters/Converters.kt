package com.ammar.havenwalls.data.db.converters

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.room.TypeConverter
import com.ammar.havenwalls.data.common.Category
import com.ammar.havenwalls.data.common.Order
import com.ammar.havenwalls.data.common.Purity
import com.ammar.havenwalls.data.common.Resolution
import com.ammar.havenwalls.data.common.Sorting
import com.ammar.havenwalls.data.common.TopRange
import com.ammar.havenwalls.extensions.toHexString
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

object Converters {
    @TypeConverter
    fun fromTimestamp(value: Long) = Instant.fromEpochMilliseconds(value)

    @TypeConverter
    fun instantToTimestamp(instant: Instant) = instant.toEpochMilliseconds()

    @TypeConverter
    fun fromJsonToStringList(value: String) =
        Json.parseToJsonElement(value).jsonArray.map { it.jsonPrimitive.content }

    @TypeConverter
    fun stringListToJson(strings: List<String>) =
        JsonArray(strings.map { JsonPrimitive(it) }).toString()

    @TypeConverter
    fun fromPurityStr(value: String) = Purity.fromName(value)

    @TypeConverter
    fun purityToString(purity: Purity) = purity.purityName

    @TypeConverter
    fun fromCategoriesStr(value: String): Set<Category> = value
        .split(",")
        .filter { it.isNotBlank() }
        .sorted()
        .mapTo(HashSet()) { Category.fromValue(it) }

    @TypeConverter
    fun categoriesToString(categories: Set<Category>) = categories
        .map { it.value }
        .sorted()
        .joinToString(",")

    @TypeConverter
    fun fromPuritiesStr(value: String): Set<Purity> = value
        .split(",")
        .filter { it.isNotBlank() }
        .sorted()
        .mapTo(HashSet()) { Purity.fromName(it) }

    @TypeConverter
    fun puritiesToString(purities: Set<Purity>) = purities
        .map { it.purityName }
        .sorted()
        .joinToString(",")

    @TypeConverter
    fun fromSortingStr(value: String) = Sorting.fromValue(value)

    @TypeConverter
    fun sortingToString(sorting: Sorting) = sorting.value

    @TypeConverter
    fun fromOrderStr(value: String) = Order.fromValue(value)

    @TypeConverter
    fun orderToString(order: Order) = order.value

    @TypeConverter
    fun fromTopRangeStr(value: String) = TopRange.fromValue(value)

    @TypeConverter
    fun topRangeToString(topRange: TopRange) = topRange.value

    @TypeConverter
    fun fromResolutionStr(value: String): Resolution {
        val parts = value.split("x")
        return Resolution(parts[0].toInt(), parts[1].toInt())
    }

    @TypeConverter
    fun resolutionToString(resolution: Resolution) = resolution.toString()

    @TypeConverter
    fun fromResolutionsStr(value: String) = value
        .split(",")
        .filter { it.isNotBlank() }
        .map { fromResolutionStr(it) }
        .sortedBy { it.width }
        .toSet()

    @TypeConverter
    fun resolutionsToString(resolutions: Set<Resolution>) = resolutions
        .sortedBy { it.width }
        .joinToString(",")

    @TypeConverter
    fun fromColorStr(value: String) = Color(value.toColorInt())

    @TypeConverter
    fun colorToString(color: Color) = color.toHexString()
}
