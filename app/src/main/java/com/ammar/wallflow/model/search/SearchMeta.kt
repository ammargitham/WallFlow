package com.ammar.wallflow.model.search

import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SearchMeta

// have to register all subclasses in the same package

@Serializable
@SerialName("WallhavenTagSearchMeta")
data class WallhavenTagSearchMeta(
    val tag: WallhavenTag,
) : SearchMeta()

@Serializable
@SerialName("WallhavenUploaderSearchMeta")
data class WallhavenUploaderSearchMeta(
    val uploader: WallhavenUploader,
) : SearchMeta()
