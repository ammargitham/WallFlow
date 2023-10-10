package com.ammar.wallflow.model.search

import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import kotlinx.serialization.Serializable

@Serializable
sealed class SearchMeta

// have to register all subclasses in the same package

@Serializable
data class WallhavenTagSearchMeta(
    val tag: WallhavenTag,
) : SearchMeta()

@Serializable
data class WallhavenUploaderSearchMeta(
    val uploader: WallhavenUploader,
) : SearchMeta()
