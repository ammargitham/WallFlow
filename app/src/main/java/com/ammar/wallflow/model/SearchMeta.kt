package com.ammar.wallflow.model

import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import kotlinx.serialization.Serializable

@Serializable
sealed class SearchMeta

// have to register all subclasses in the same package

@Serializable
data class WallhavenTagSearchMeta(
    val wallhavenTag: WallhavenTag,
) : SearchMeta()

@Serializable
data class WallhavenUploaderSearchMeta(
    val wallhavenUploader: WallhavenUploader,
) : SearchMeta()
