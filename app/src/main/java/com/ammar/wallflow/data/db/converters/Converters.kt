package com.ammar.wallflow.data.db.converters

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt
import androidx.room.TypeConverter
import com.ammar.wallflow.extensions.toHexString
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.json
import com.ammar.wallflow.model.Order
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.model.search.WallhavenCategory
import com.ammar.wallflow.model.search.WallhavenSorting
import com.ammar.wallflow.model.search.WallhavenTopRange
import kotlinx.datetime.Instant
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
    fun fromJsonToStringList(value: String) = json.parseToJsonElement(value)
        .jsonArray.map { it.jsonPrimitive.content }

    @TypeConverter
    fun stringListToJson(strings: List<String>) =
        JsonArray(strings.map { JsonPrimitive(it) }).toString()

    @TypeConverter
    fun fromPurityStr(value: String) = Purity.fromName(value)

    @TypeConverter
    fun purityToString(purity: Purity) = purity.purityName

    @TypeConverter
    fun fromCategoriesStr(value: String): Set<WallhavenCategory> = value
        .split(",")
        .filter { it.isNotBlank() }
        .sorted()
        .mapTo(HashSet()) { WallhavenCategory.fromValue(it) }

    @TypeConverter
    fun categoriesToString(categories: Set<WallhavenCategory>) = categories
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
    fun fromSortingStr(value: String) = WallhavenSorting.fromValue(value)

    @TypeConverter
    fun sortingToString(sorting: WallhavenSorting) = sorting.value

    @TypeConverter
    fun fromOrderStr(value: String) = Order.fromValue(value)

    @TypeConverter
    fun orderToString(order: Order) = order.value

    @TypeConverter
    fun fromTopRangeStr(value: String) = WallhavenTopRange.fromValue(value)

    @TypeConverter
    fun topRangeToString(topRange: WallhavenTopRange) = topRange.value

    @TypeConverter
    fun fromIntSizeStr(value: String): IntSize {
        val parts = value
            .split("x")
            .map { s -> s.trimAll() }
            .filter { s -> s.isNotBlank() }
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

    @TypeConverter
    fun fromWallpaperTargetSetStr(value: String) = value
        .split(",")
        .filter { it.isNotBlank() }
        .sorted()
        .map { WallpaperTarget.valueOf(it) }
        .toSet()

    @TypeConverter
    fun wallpaperTargetSetToString(targets: Set<WallpaperTarget>) = targets
        .map { it.name }
        .sorted()
        .joinToString(",")
}
