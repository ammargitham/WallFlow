package com.ammar.wallflow.data.repository.local

import android.content.Context
import android.net.Uri
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.SUPPORTED_MIME_TYPES
import com.ammar.wallflow.data.repository.AutoWallpaperHistoryRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.extensions.deepListFiles
import com.ammar.wallflow.extensions.toLocalWallpaper
import com.ammar.wallflow.extensions.toUriOrNull
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.local.LocalWallpaper
import com.ammar.wallflow.ui.screens.local.LocalSort
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice
import com.lazygeniouz.dfc.file.DocumentFileCompat
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class DefaultLocalWallpapersRepository @Inject constructor(
    private val autoWallpaperHistoryRepository: AutoWallpaperHistoryRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LocalWallpapersRepository {
    override fun wallpapersPager(
        context: Context,
        uris: Collection<Uri>,
        sort: LocalSort,
    ) = flow {
        emit(
            PagingData.from(
                getAllLocalWallpapers(context, uris, sort),
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
        sort: LocalSort = LocalSort.NO_SORT,
    ): List<Wallpaper> = uris.fold(mutableListOf()) { acc, uri ->
        var tempFiles = DocumentFileCompat.fromTreeUri(
            context = context,
            uri = uri,
        )
            ?.deepListFiles()
            ?.filter { it.getType() in SUPPORTED_MIME_TYPES }
            ?.distinctBy { it.uri }
        if (sort != LocalSort.NO_SORT) {
            tempFiles = tempFiles?.sortedWith(
                when (sort) {
                    LocalSort.NAME -> compareBy { it.name }
                    LocalSort.LAST_MODIFIED -> compareBy { it.lastModified }
                    else -> compareBy { null }
                },
            )
        }
        val files = tempFiles
            ?.map { it.toLocalWallpaper(context) }
            ?: emptyList()
        acc.addAll(files)
        acc
    }

    override fun wallpaper(
        context: Context,
        wallpaperUriStr: String,
    ): Flow<Resource<LocalWallpaper?>> {
        val uri = Uri.parse(wallpaperUriStr)
        try {
            val doc = DocumentFileCompat.fromSingleUri(context, uri)
                ?: return flowOf(
                    Resource.Error(
                        IllegalArgumentException("Invalid or non-existing uri"),
                    ),
                )
            return flowOf(Resource.Success(doc.toLocalWallpaper(context)))
        } catch (e: Exception) {
            return flowOf(Resource.Error(e))
        }
    }

    override suspend fun getRandom(
        context: Context,
        uris: Collection<Uri>,
    ) = withContext(ioDispatcher) {
        getAllLocalWallpapers(context, uris).randomOrNull()
    }

    override suspend fun getFirstFresh(
        context: Context,
        uris: Collection<Uri>,
        excluding: Collection<Wallpaper>,
    ) = withContext(ioDispatcher) {
        val wallpapersInUri = getAllLocalWallpapers(
            context = context,
            uris = uris,
            sort = LocalSort.LAST_MODIFIED,
        )
        val historyIds = autoWallpaperHistoryRepository.getAllSourceIdsBySourceChoice(
            SourceChoice.LOCAL,
        )
        val excludedUris = getUris(excluding)
        wallpapersInUri.firstOrNull {
            it.id !in historyIds && it.data !in excludedUris
        }
    }

    override suspend fun getByOldestSetOn(
        context: Context,
        excluding: Collection<Wallpaper>,
    ) = withContext(ioDispatcher) {
        val excludedUriStrings = getUris(excluding).map { it.toString() }
        val oldestId =
            autoWallpaperHistoryRepository.getOldestSetOnSourceIdBySourceChoiceAndSourceIdNotIn(
                sourceChoice = SourceChoice.LOCAL,
                excludedSourceIds = excludedUriStrings,
            ) ?: return@withContext null
        val uri = oldestId.toUriOrNull() ?: return@withContext null
        DocumentFileCompat.fromSingleUri(context, uri)?.toLocalWallpaper(context)
    }

    override suspend fun getCountExcludingWallpapers(
        context: Context,
        uris: Collection<Uri>,
        excluding: Collection<Wallpaper>,
    ) = withContext(ioDispatcher) {
        val excludedUris = getUris(excluding)
        getAllLocalWallpapers(
            context = context,
            uris = uris,
        ).filterNot {
            it.data in excludedUris
        }.size
    }

    private fun getUris(excluding: Collection<Wallpaper>) = excluding
        .filter { it.source == Source.LOCAL }
        .map { it.data }
}
