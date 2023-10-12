package com.ammar.wallflow.model.search

import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// have to register all subclasses in the same package

@Serializable
sealed class SearchMeta

@Serializable
sealed class WallhavenSearchMeta : SearchMeta()

@Serializable
@SerialName("WallhavenTagSearchMeta")
data class WallhavenTagSearchMeta(
    val tag: WallhavenTag,
) : WallhavenSearchMeta()

@Serializable
@SerialName("WallhavenUploaderSearchMeta")
data class WallhavenUploaderSearchMeta(
    val uploader: WallhavenUploader,
) : WallhavenSearchMeta()
