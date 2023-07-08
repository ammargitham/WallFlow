@file:UseSerializers(ColorSerializer::class, IntSizeSerializer::class)

package com.ammar.wallflow.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt
import com.ammar.wallflow.data.db.converters.Converters.fromIntSizeStr
import com.ammar.wallflow.extensions.quoteIfSpaced
import com.ammar.wallflow.extensions.toHexString
import com.ammar.wallflow.extensions.toQueryString
import com.ammar.wallflow.extensions.urlDecoded
import com.ammar.wallflow.model.Ratio.CategoryRatio
import com.ammar.wallflow.model.serializers.ColorSerializer
import com.ammar.wallflow.model.serializers.IntSizeSerializer
import java.util.regex.Pattern
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class SearchQuery(
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
    val username: String? = null,
    val tagId: Long? = null,
    val wallpaperId: String? = null,
    val categories: Set<Category> = defaultCategories,
    val purity: Set<Purity> = defaultPurities,
    val sorting: Sorting = Sorting.DATE_ADDED,
    val order: Order = Order.DESC,
    val topRange: TopRange = TopRange.ONE_MONTH,
    val atleast: IntSize? = null,
    val resolutions: Set<IntSize> = emptySet(),
    val colors: Color? = null,
    val seed: String? = null,
    val ratios: Set<Ratio> = emptySet(),
) {
    private fun toStringMap() = mapOf(
        "includedTags" to includedTags
            .sorted()
            .joinToString(","),
        "excludedTags" to excludedTags
            .sorted()
            .joinToString(","),
        "username" to (username ?: ""),
        "tagId" to (tagId?.toString() ?: ""),
        "wallpaperId" to (wallpaperId ?: ""),
        "categories" to categories
            .sortedBy { it.value }
            .joinToString(",") { it.value },
        "purity" to purity
            .sortedBy { it.purityName }
            .joinToString(",") { it.purityName },
        "sorting" to sorting.value,
        "order" to order.value,
        "topRange" to topRange.value,
        "atleast" to (atleast?.toString() ?: ""),
        "resolutions" to resolutions.sortedBy { it.width }.joinToString(","),
        "ratios" to ratios.map { it.toRatioString() }.sorted().joinToString(","),
        "colors" to (colors?.toHexString() ?: ""),
        "seed" to (seed ?: ""),
    )

    fun getQString(): String {
        return ArrayList<String>().apply {
            val i = includedTags
                .filter { it.isNotBlank() }
                .joinToString(" ") { "+${it.quoteIfSpaced()}" }
            if (i.isNotBlank()) {
                add(i)
            }
            val e = excludedTags
                .filter { it.isNotBlank() }
                .joinToString(" ") { "-${it.quoteIfSpaced()}" }
            if (e.isNotBlank()) {
                add(e)
            }
            username?.run {
                if (this.isNotBlank()) {
                    this@apply.add("@$this")
                }
            }
            tagId?.run {
                this@apply.add("id:$this")
            }
            wallpaperId?.run {
                this@apply.add("like:$this")
            }
        }.joinToString(" ")
    }

    fun toQueryString() = toStringMap().toQueryString()

    companion object {
        val defaultCategories = setOf(Category.GENERAL, Category.ANIME, Category.PEOPLE)
        val defaultPurities = setOf(Purity.SFW)

        fun fromQueryString(string: String): SearchQuery {
            val map = string
                .split("&")
                .map { it.split(Pattern.compile("="), 2) }
                .associate {
                    Pair(
                        it[0].urlDecoded(),
                        if (it.size > 1) it[1].urlDecoded() else null
                    )
                }
            return SearchQuery(
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
                    ?.map { Category.fromValue(it) }
                    ?.toSet()
                    ?: emptySet(),
                purity = map["purity"]
                    ?.split(",")
                    ?.map { Purity.fromName(it) }
                    ?.toSet()
                    ?: emptySet(),
                sorting = map["sorting"]
                    ?.let { Sorting.fromValue(it) }
                    ?: Sorting.DATE_ADDED,
                order = map["order"]
                    ?.let { Order.fromValue(it) }
                    ?: Order.DESC,
                topRange = map["topRange"]
                    ?.let { TopRange.fromValue(it) }
                    ?: TopRange.ONE_MONTH,
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
                            CategoryRatio.Category.LANDSCAPE.categoryName -> {
                                Ratio.fromCategory(CategoryRatio.Category.LANDSCAPE)
                            }
                            CategoryRatio.Category.PORTRAIT.categoryName -> {
                                Ratio.fromCategory(CategoryRatio.Category.PORTRAIT)
                            }
                            else -> {
                                Ratio.fromSize(fromIntSizeStr(it))
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

enum class Category(
    val flag: Int,
    val value: String,
) {
    GENERAL(100, "general"),
    ANIME(10, "anime"),
    PEOPLE(1, "people");

    companion object {
        fun fromValue(value: String) = when (value) {
            "general" -> GENERAL
            "anime" -> ANIME
            else -> PEOPLE
        }
    }
}

fun Set<Category>.toCategoryInt() = this.fold(0) { p, f -> p + f.flag }

enum class Purity(
    val purityName: String,
    val flag: Int,
) {
    SFW("sfw", 100),
    SKETCHY("sketchy", 10),
    NSFW("nsfw", 1);

    companion object {
        fun fromName(name: String) = when (name) {
            "nsfw" -> NSFW
            "sketchy" -> SKETCHY
            else -> SFW
        }
    }
}

fun Set<Purity>.toPurityInt() = this.fold(0) { p, f -> p + f.flag }

enum class Sorting(
    val value: String,
) {
    TOPLIST("toplist"),
    DATE_ADDED("date_added"),
    RELEVANCE("relevance"),
    RANDOM("random"),
    VIEWS("views"),
    FAVORITES("favorites");

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

enum class Order(
    val value: String,
) {
    DESC("desc"),
    ASC("asc");

    companion object {
        fun fromValue(value: String) = if (value == "desc") DESC else ASC
    }
}

enum class TopRange(
    val value: String,
) {
    ONE_DAY("1d"),
    THREE_DAYS("3d"),
    ONE_WEEK("1w"),
    ONE_MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    ONE_YEAR("1y");

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
sealed class Ratio {
    abstract fun toRatioString(): String

    data class CategoryRatio(
        val category: Category,
    ) : Ratio() {
        enum class Category(val categoryName: String) {
            LANDSCAPE("landscape"),
            PORTRAIT("portrait");
        }

        override fun toRatioString() = this.category.categoryName
    }

    data class SizeRatio(
        val size: IntSize,
    ) : Ratio() {
        override fun toRatioString() = this.size.toString()
    }

    companion object {
        fun fromSize(size: IntSize): Ratio = SizeRatio(size)
        fun fromCategory(category: CategoryRatio.Category): Ratio = CategoryRatio(category)
    }
}

val SearchQuerySaver = Saver<SearchQuery, String>(
    save = { it.toQueryString() },
    restore = { SearchQuery.fromQueryString(it) }
)

val MutableStateSearchQuerySaver = Saver<MutableState<SearchQuery>, String>(
    save = { it.value.toQueryString() },
    restore = { mutableStateOf(SearchQuery.fromQueryString(it)) }
)
