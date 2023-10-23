@file:UseSerializers(ColorSerializer::class, IntSizeSerializer::class)

package com.ammar.wallflow.model.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt
import com.ammar.wallflow.data.db.converters.Converters.fromIntSizeStr
import com.ammar.wallflow.extensions.urlDecoded
import com.ammar.wallflow.model.Order
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.search.WallhavenRatio.CategoryWallhavenRatio
import com.ammar.wallflow.model.serializers.ColorSerializer
import com.ammar.wallflow.model.serializers.IntSizeSerializer
import java.util.regex.Pattern
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
@SerialName("WallhavenFilters")
data class WallhavenFilters(
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
    val username: String? = null,
    val tagId: Long? = null,
    val wallpaperId: String? = null,
    val categories: Set<WallhavenCategory> = defaultCategories,
    val purity: Set<Purity> = defaultPurities,
    val sorting: WallhavenSorting = WallhavenSorting.DATE_ADDED,
    val order: Order = Order.DESC,
    val topRange: WallhavenTopRange = WallhavenTopRange.ONE_MONTH,
    val atleast: IntSize? = null,
    val resolutions: Set<IntSize> = emptySet(),
    val colors: Color? = null,
    val seed: String? = null,
    val ratios: Set<WallhavenRatio> = emptySet(),
) : Filters() {
    companion object {
        val defaultCategories = setOf(
            WallhavenCategory.GENERAL,
            WallhavenCategory.ANIME,
            WallhavenCategory.PEOPLE,
        )
        val defaultPurities = setOf(Purity.SFW)

        @Deprecated(
            level = DeprecationLevel.WARNING,
            message = "Used only for migrations. Replace with json serialization",
        )
        fun fromQueryString(string: String): WallhavenFilters {
            val map = string
                .split("&")
                .map { it.split(Pattern.compile("="), 2) }
                .associate {
                    Pair(
                        it[0].urlDecoded(),
                        if (it.size > 1) it[1].urlDecoded() else null,
                    )
                }
            return WallhavenFilters(
                includedTags = map["includedTags"]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    ?: emptySet(),
                excludedTags = map["excludedTags"]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    ?: emptySet(),
                username = map["username"]?.ifBlank { null },
                tagId = map["tagId"]?.ifBlank { null }?.toLong(),
                wallpaperId = map["tagId"]?.ifBlank { null },
                categories = map["categories"]
                    ?.split(",")
                    ?.map { WallhavenCategory.fromValue(it) }
                    ?.toSet()
                    ?: emptySet(),
                purity = map["purity"]
                    ?.split(",")
                    ?.map { Purity.fromName(it) }
                    ?.toSet()
                    ?: emptySet(),
                sorting = map["sorting"]
                    ?.let { WallhavenSorting.fromValue(it) }
                    ?: WallhavenSorting.DATE_ADDED,
                order = map["order"]
                    ?.let { Order.fromValue(it) }
                    ?: Order.DESC,
                topRange = map["topRange"]
                    ?.let { WallhavenTopRange.fromValue(it) }
                    ?: WallhavenTopRange.ONE_MONTH,
                atleast = map["atleast"]
                    ?.let {
                        if (it.isBlank()) {
                            return@let null
                        }
                        fromIntSizeStr(it)
                    },
                resolutions = map["resolutions"]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.map { fromIntSizeStr(it) }
                    ?.toSet()
                    ?: emptySet(),
                ratios = map["ratios"]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.map {
                        when (it) {
                            CategoryWallhavenRatio.Category.LANDSCAPE.categoryName -> {
                                WallhavenRatio.fromCategory(
                                    CategoryWallhavenRatio.Category.LANDSCAPE,
                                )
                            }
                            CategoryWallhavenRatio.Category.PORTRAIT.categoryName -> {
                                WallhavenRatio.fromCategory(
                                    CategoryWallhavenRatio.Category.PORTRAIT,
                                )
                            }
                            else -> {
                                WallhavenRatio.fromSize(fromIntSizeStr(it))
                            }
                        }
                    }
                    ?.toSet()
                    ?: emptySet(),
                colors = map["colors"]?.let {
                    if (it.isBlank()) {
                        return@let null
                    }
                    Color(it.toColorInt())
                },
                seed = map["seed"]?.ifBlank { null },
            )
        }
    }
}

enum class WallhavenCategory(
    val flag: Int,
    val value: String,
) {
    GENERAL(100, "general"),
    ANIME(10, "anime"),
    PEOPLE(1, "people"),
    ;

    companion object {
        fun fromValue(value: String) = when (value) {
            "general" -> GENERAL
            "anime" -> ANIME
            else -> PEOPLE
        }
    }
}

fun Set<WallhavenCategory>.toCategoryInt() = this.fold(0) { p, f -> p + f.flag }

enum class WallhavenSorting(
    val value: String,
) {
    TOPLIST("toplist"),
    DATE_ADDED("date_added"),
    RELEVANCE("relevance"),
    RANDOM("random"),
    VIEWS("views"),
    FAVORITES("favorites"),
    ;

    companion object {
        fun fromValue(value: String) = when (value) {
            "date_added" -> DATE_ADDED
            "relevance" -> RELEVANCE
            "random" -> RANDOM
            "views" -> VIEWS
            "favorites" -> FAVORITES
            else -> TOPLIST
        }
    }
}

enum class WallhavenTopRange(
    val value: String,
) {
    ONE_DAY("1d"),
    THREE_DAYS("3d"),
    ONE_WEEK("1w"),
    ONE_MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    ONE_YEAR("1y"),
    ;

    companion object {
        fun fromValue(value: String) = when (value) {
            "1d" -> ONE_DAY
            "3d" -> THREE_DAYS
            "1w" -> ONE_WEEK
            "1M" -> ONE_MONTH
            "3M" -> THREE_MONTHS
            "6M" -> SIX_MONTHS
            else -> ONE_YEAR
        }
    }
}

@Serializable
sealed class WallhavenRatio {
    abstract fun toRatioString(): String

    @Serializable
    data class CategoryWallhavenRatio(
        val category: Category,
    ) : WallhavenRatio() {
        enum class Category(val categoryName: String) {
            LANDSCAPE("landscape"),
            PORTRAIT("portrait"),
        }

        override fun toRatioString() = this.category.categoryName
    }

    @Serializable
    data class SizeWallhavenRatio(
        val size: IntSize,
    ) : WallhavenRatio() {
        override fun toRatioString() = this.size.toString()
    }

    companion object {
        fun fromSize(size: IntSize): WallhavenRatio = SizeWallhavenRatio(size)
        fun fromCategory(category: CategoryWallhavenRatio.Category): WallhavenRatio =
            CategoryWallhavenRatio(category)
    }
}
