package com.ammar.havenwalls.data.db.converters

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt
import androidx.room.TypeConverter
import com.ammar.havenwalls.extensions.toHexString
import com.ammar.havenwalls.model.Category
import com.ammar.havenwalls.model.Order
import com.ammar.havenwalls.model.Purity
import com.ammar.havenwalls.model.Sorting
import com.ammar.havenwalls.model.TopRange
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
    fun fromIntSizeStr(value: String): IntSize {
        val parts = value.split("x")
        return IntSize(parts[0].toInt(), parts[1].toInt())
    }

    @TypeConverter
    fun intSizeToString(intSize: IntSize) = intSize.toString()

    @TypeConverter
    fun fromIntSizesStr(value: String) = value
        .split(",")
        .filter { it.isNotBlank() }
        .map { fromIntSizeStr(it) }
        .sortedBy { it.width }
        .toSet()

    @TypeConverter
    fun intSizesToString(intSizes: Set<IntSize>) = intSizes
        .sortedBy { it.width }
        .joinToString(",")

    @TypeConverter
    fun fromColorStr(value: String) = Color(value.toColorInt())

    @TypeConverter
    fun colorToString(color: Color) = color.toHexString()
}
