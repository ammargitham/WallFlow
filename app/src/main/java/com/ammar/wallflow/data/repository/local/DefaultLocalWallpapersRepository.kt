package com.ammar.wallflow.data.repository.local

import android.content.Context
import android.net.Uri
import androidx.compose.ui.unit.IntSize
import androidx.exifinterface.media.ExifInterface
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.repository.local.LocalWallpapersRepository.Companion.SUPPORTED_MIME_TYPES
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.extensions.deepListFiles
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.local.LocalWallpaper
import com.lazygeniouz.dfc.file.DocumentFileCompat
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class DefaultLocalWallpapersRepository @Inject constructor(
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
) : LocalWallpapersRepository {
    override fun wallpapersPager(
        context: Context,
        uris: Collection<Uri>,
    ) = flow {
        emit(
            PagingData.from(
                getAllLocalWallpapers(context, uris),
                sourceLoadStates = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = true),
                    prepend = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.NotLoading(endOfPaginationReached = true),
                ),
            ),
        )
    }.flowOn(ioDispatcher)

    private fun getAllLocalWallpapers(
        context: Context,
        uris: Collection<Uri>,
    ) = uris.fold(mutableListOf<Wallpaper>()) { acc, uri ->
        val files = DocumentFileCompat.fromTreeUri(
            context = context,
            uri = uri,
        )
            ?.deepListFiles()
            ?.filter { it.getType() in SUPPORTED_MIME_TYPES }
            ?.distinctBy { it.uri }
            ?.map { dfcToLocalWallpaper(context, it) }
            ?: emptyList()
        acc.addAll(files)
        acc
    }

    override fun wallpaper(
        context: Context,
        wallpaperUriStr: String,
    ): Flow<Resource<LocalWallpaper?>> {
        val uri = Uri.parse(wallpaperUriStr)
        val doc = DocumentFileCompat.fromSingleUri(context, uri)
            ?: return flowOf(
                Resource.Error(
                    IllegalArgumentException("Invalid or non-existing uri"),
                ),
            )
        return flowOf(Resource.Success(dfcToLocalWallpaper(context, doc)))
    }

    override suspend fun getRandom(
        context: Context,
        uris: Collection<Uri>,
    ) = withContext(ioDispatcher) {
        getAllLocalWallpapers(context, uris).randomOrNull()
    }

    private fun dfcToLocalWallpaper(
        context: Context,
        it: DocumentFileCompat,
    ): LocalWallpaper {
        val resolution = try {
            context.contentResolver.openInputStream(it.uri)?.use { openInputStream ->
                val exifInterface = ExifInterface(openInputStream)
                var height = exifInterface.getAttributeInt(
                    ExifInterface.TAG_IMAGE_LENGTH,
                    -1,
                )
                if (height <= 0) {
                    height = 500
                }
                var width = exifInterface.getAttributeInt(
                    ExifInterface.TAG_IMAGE_WIDTH,
                    -1,
                )
                if (width <= 0) {
                    width = 500
                }
                IntSize(width, height)
            } ?: IntSize(500, 500)
        } catch (e: Exception) {
            IntSize(500, 500)
        }
        return LocalWallpaper(
            id = it.uri.toString(),
            data = it.uri,
            fileSize = it.length,
            resolution = resolution,
            mimeType = it.getType(),
            name = it.name,
        )
    }
}
