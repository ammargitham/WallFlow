@file:UseSerializers(ColorSerializer::class)

package com.ammar.havenwalls.data.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt
import com.ammar.havenwalls.extensions.quoteIfSpaced
import com.ammar.havenwalls.extensions.toHexString
import com.ammar.havenwalls.extensions.toQueryString
import com.ammar.havenwalls.extensions.urlDecoded
import com.ammar.havenwalls.model.serializers.ColorSerializer
import com.mr0xf00.easycrop.AspectRatio
import java.util.regex.Pattern
import kotlinx.serialization.UseSerializers

@kotlinx.serialization.Serializable
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
    val atleast: Resolution? = null,
    val resolutions: Set<Resolution> = emptySet(),
    val colors: Color? = null,
    val seed: String? = null,
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
                        val parts = it.split("x")
                        Resolution(parts[0].toInt(), parts[1].toInt())
                    },
                resolutions = map["resolutions"]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.map {
                        val parts = it.split("x")
                        Resolution(parts[0].toInt(), parts[1].toInt())
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

// TODO Replace Resolution with IntSize
@kotlinx.serialization.Serializable
data class Resolution(
    val width: Int,
    val height: Int,
) {
    val aspectRatio by lazy { if (height != 0) width.toFloat() / height else 0F }
    override fun toString() = "${width}x${height}"

    fun toIntSize() = IntSize(width, height)

    companion object {
        val ZERO = Resolution(0, 0)
    }
}

private fun reduceFraction(x: Int, y: Int): Pair<Int, Int> {
    val d = gcd(x, y)
    return Pair(x / d, y / d)
}

private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

fun Resolution.getCropAspectRatio(): AspectRatio {
    val (x, y) = reduceFraction(width, height)
    return AspectRatio(x, y)
}

val SearchQuerySaver = Saver<SearchQuery, String>(
    save = { it.toQueryString() },
    restore = { SearchQuery.fromQueryString(it) }
)

val MutableStateSearchQuerySaver = Saver<MutableState<SearchQuery>, String>(
    save = { it.value.toQueryString() },
    restore = { mutableStateOf(SearchQuery.fromQueryString(it)) }
)
