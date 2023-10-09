package com.ammar.wallflow.model.search

import com.ammar.wallflow.extensions.fromQueryString
import com.ammar.wallflow.extensions.toQueryString
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class SearchMetaType {
    WALLHAVEN_TAG,
    WALLHAVEN_UPLOADER,
}

@Serializable
sealed class SearchMeta {
    abstract val type: SearchMetaType
    abstract fun toQueryString(): String

    companion object {
        fun fromQueryString(string: String): SearchMeta? {
            val split = string.split("&")
            // there has to be atleast 2 parts, one for type and other meta properties
            if (split.size < 2) {
                return null
            }
            val typeStr = split[0].substringAfter("=")
            return try {
                when (SearchMetaType.valueOf(typeStr)) {
                    SearchMetaType.WALLHAVEN_TAG -> WallhavenTagSearchMeta.fromQueryString(string)
                    SearchMetaType.WALLHAVEN_UPLOADER ->
                        WallhavenUploaderSearchMeta.fromQueryString(string)
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

// have to register all subclasses in the same package

@Serializable
data class WallhavenTagSearchMeta(
    val tag: WallhavenTag,
) : SearchMeta() {
    override val type = SearchMetaType.WALLHAVEN_TAG

    override fun toQueryString() = mapOf(
        "type" to SearchMetaType.WALLHAVEN_TAG.name,
        "tag" to Json.encodeToString(tag),
    ).toQueryString()

    companion object {
        fun fromQueryString(string: String) = try {
            WallhavenTagSearchMeta(
                tag = Json.decodeFromString(
                    string.fromQueryString()["tag"]!!, // will raise exception if string incorrect
                ),
            )
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class WallhavenUploaderSearchMeta(
    val uploader: WallhavenUploader,
) : SearchMeta() {
    override val type = SearchMetaType.WALLHAVEN_UPLOADER

    override fun toQueryString() = mapOf(
        "type" to SearchMetaType.WALLHAVEN_UPLOADER.name,
        "uploader" to Json.encodeToString(uploader),
    ).toQueryString()

    companion object {
        fun fromQueryString(string: String) = try {
            WallhavenUploaderSearchMeta(
                uploader = Json.decodeFromString(
                    // will raise exception if string incorrect
                    string.fromQueryString()["uploader"]!!,
                ),
            )
        } catch (e: Exception) {
            null
        }
    }
}
