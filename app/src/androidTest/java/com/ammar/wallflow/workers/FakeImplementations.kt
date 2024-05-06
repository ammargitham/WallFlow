package com.ammar.wallflow.workers

import android.content.Context
import android.net.Uri
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.ammar.wallflow.data.db.dao.AutoWallpaperHistoryDao
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.LightDarkDao
import com.ammar.wallflow.data.db.dao.ObjectDetectionModelDao
import com.ammar.wallflow.data.db.dao.ViewedDao
import com.ammar.wallflow.data.db.dao.search.SavedSearchDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.LightDarkEntity
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import com.ammar.wallflow.data.db.entity.ViewedEntity
import com.ammar.wallflow.data.db.entity.search.SavedSearchEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperTagsEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperUploaderEntity
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallhavenWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallpaperWithUploaderAndTags
import com.ammar.wallflow.data.network.RedditNetworkDataSource
import com.ammar.wallflow.data.network.WallhavenNetworkDataSource
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpaperResponse
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpapersResponse
import com.ammar.wallflow.data.repository.local.LocalWallpapersRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.local.LocalWallpaper
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.ui.screens.local.LocalSort
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice
import kotlinx.coroutines.flow.Flow
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document

internal open class FakeSavedSearchDao : SavedSearchDao {
    override fun observeAll() = throw RuntimeException()

    override suspend fun getAll() = throw RuntimeException()

    override suspend fun getAllByNamesUpTo999Items(
        names: Collection<String>,
    ) = throw RuntimeException()

    override suspend fun getAllByNames(
        names: Collection<String>,
    ) = throw RuntimeException()

    override suspend fun exists(id: Long) = throw RuntimeException()

    override suspend fun exists(name: String) = throw RuntimeException()

    override suspend fun existsExcludingId(
        id: Long,
        name: String,
    ): Boolean = throw RuntimeException()

    override suspend fun getById(id: Long): SavedSearchEntity? = throw RuntimeException()

    override suspend fun getByName(name: String) = throw RuntimeException()

    override suspend fun upsert(savedSearch: SavedSearchEntity) = throw RuntimeException()

    override suspend fun upsert(
        savedSearchDaos: Collection<SavedSearchEntity>,
    ) = throw RuntimeException()

    override suspend fun deleteByName(name: String) = throw RuntimeException()
}

internal open class FakeAutoWallpaperHistoryDao : AutoWallpaperHistoryDao {
    override suspend fun getAll(): List<AutoWallpaperHistoryEntity> {
        throw RuntimeException()
    }

    override suspend fun getAllBySource(
        source: Source,
    ): List<AutoWallpaperHistoryEntity> {
        throw RuntimeException()
    }

    override suspend fun getAllBySourceChoice(sourceChoice: SourceChoice) = throw RuntimeException()

    override suspend fun getAllSourceIdsBySourceChoice(sourceChoice: SourceChoice): List<String> {
        throw RuntimeException()
    }

    override suspend fun getOldestSetOnSourceIdBySourceChoiceAndSourceIdNotIn(
        sourceChoice: SourceChoice,
        excludedSourceIds: Collection<String>,
    ): String? {
        throw RuntimeException()
    }

    override suspend fun getBySourceId(
        sourceId: String,
        source: Source,
    ): AutoWallpaperHistoryEntity? {
        throw RuntimeException()
    }

    override suspend fun upsert(vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity) {
        throw RuntimeException()
    }

    override suspend fun deleteBySourceIdsAndSourceChoice(
        sourceIds: Collection<String>,
        sourceChoice: SourceChoice,
    ) {
        throw RuntimeException()
    }
}

internal open class FakeObjectDetectionModelDao : ObjectDetectionModelDao {
    override fun getAll(): Flow<List<ObjectDetectionModelEntity>> {
        throw RuntimeException()
    }

    override suspend fun getById(id: Long): ObjectDetectionModelEntity? {
        throw RuntimeException()
    }

    override suspend fun getByName(name: String): ObjectDetectionModelEntity? {
        throw RuntimeException()
    }

    override suspend fun nameExists(name: String): Boolean {
        throw RuntimeException()
    }

    override suspend fun nameExistsExcludingId(id: Long, name: String): Boolean {
        throw RuntimeException()
    }

    override suspend fun upsert(vararg objectDetectionModelEntity: ObjectDetectionModelEntity) {
        throw RuntimeException()
    }

    override suspend fun deleteByName(name: String) {
        throw RuntimeException()
    }

    override suspend fun delete(entity: ObjectDetectionModelEntity) {
        throw RuntimeException()
    }
}

internal open class FakeWallhavenNetworkDataSource : WallhavenNetworkDataSource {
    override suspend fun search(
        search: WallhavenSearch,
        page: Int?,
    ): NetworkWallhavenWallpapersResponse {
        throw RuntimeException()
    }

    override suspend fun wallpaper(
        wallpaperWallhavenId: String,
    ): NetworkWallhavenWallpaperResponse {
        throw RuntimeException()
    }

    override suspend fun popularTags(): Document? {
        throw RuntimeException()
    }
}

internal open class FakeRedditNetworkDataSource : RedditNetworkDataSource {
    override suspend fun search(search: RedditSearch, after: String?) = throw RuntimeException()
}

internal open class FakeFavoriteDao : FavoriteDao {
    override fun observeAll(): Flow<List<FavoriteEntity>> {
        throw RuntimeException()
    }

    override suspend fun getAll(): List<FavoriteEntity> {
        throw RuntimeException()
    }

    override fun pagingSource(): PagingSource<Int, FavoriteEntity> {
        throw RuntimeException()
    }

    override suspend fun exists(sourceId: String, source: Source): Boolean {
        throw RuntimeException()
    }

    override fun observeExists(sourceId: String, source: Source) = throw RuntimeException()

    override fun observeCount() = throw RuntimeException()

    override suspend fun getCount(): Int {
        throw RuntimeException()
    }

    override suspend fun getBySourceIdAndType(
        sourceId: String,
        source: Source,
    ): FavoriteEntity? {
        throw RuntimeException()
    }

    override suspend fun getRandom(): FavoriteEntity? {
        throw RuntimeException()
    }

    override suspend fun getFirstFreshExcludingIds(
        excludingIds: Collection<Long>,
    ): FavoriteEntity? = throw RuntimeException()

    override suspend fun getByOldestSetOnAndIdsNotIn(
        excludingIds: Collection<Long>,
    ): FavoriteEntity? = throw RuntimeException()

    override suspend fun insertAll(favoriteEntities: Collection<FavoriteEntity>) {
        throw RuntimeException()
    }

    override suspend fun upsert(favoriteEntity: FavoriteEntity) {
        throw RuntimeException()
    }

    override suspend fun deleteBySourceIdAndSource(sourceId: String, source: Source) {
        throw RuntimeException()
    }

    override suspend fun deleteBySourceIdsAndSource(sourceIds: Collection<String>, source: Source) {
        throw RuntimeException()
    }

    override suspend fun getIdsBySourceIdsAndSource(
        sourceIds: Collection<String>,
        source: Source,
    ): List<Long> {
        throw RuntimeException()
    }

    override suspend fun getCountWhereIdsNotIn(ids: Collection<Long>): Int {
        throw RuntimeException()
    }
}

internal open class FakeWallhavenWallpapersDao : WallhavenWallpapersDao {
    override suspend fun getAll(): List<WallhavenWallpaperEntity> {
        throw RuntimeException()
    }

    override suspend fun getByWallhavenId(wallhavenId: String): WallhavenWallpaperEntity? {
        throw RuntimeException()
    }

    override suspend fun getWithUploaderAndTagsByWallhavenId(
        wallhavenId: String,
    ): WallpaperWithUploaderAndTags? {
        throw RuntimeException()
    }

    override suspend fun getAllWithUploaderAndTagsByWallhavenIds(
        wallhavenIds: Collection<String>,
    ): List<WallpaperWithUploaderAndTags> {
        throw RuntimeException()
    }

    override suspend fun getAllWithUploaderAndTags(): List<WallpaperWithUploaderAndTags> {
        throw RuntimeException()
    }

    override suspend fun getAllByWallhavenIdsUpTo999Items(
        wallhavenIds: Collection<String>,
    ): List<WallhavenWallpaperEntity> {
        throw RuntimeException()
    }

    override suspend fun getByWallhavenIds(
        wallhavenIds: List<String>,
    ): List<WallhavenWallpaperEntity> {
        throw RuntimeException()
    }

    override fun pagingSource(queryString: String): PagingSource<Int, WallhavenWallpaperEntity> {
        throw RuntimeException()
    }

    override suspend fun count(): Int {
        throw RuntimeException()
    }

    override suspend fun deleteAll() {
        throw RuntimeException()
    }

    override suspend fun insert(vararg wallpaper: WallhavenWallpaperEntity): List<Long> {
        throw RuntimeException()
    }

    override suspend fun insert(wallpapers: Collection<WallhavenWallpaperEntity>): List<Long> {
        throw RuntimeException()
    }

    override suspend fun update(vararg wallpaper: WallhavenWallpaperEntity) {
        throw RuntimeException()
    }

    override suspend fun upsert(vararg wallpaper: WallhavenWallpaperEntity): List<Long> {
        throw RuntimeException()
    }

    override suspend fun upsert(wallpapers: Collection<WallhavenWallpaperEntity>) {
        throw RuntimeException()
    }

    override suspend fun getAllUniqueToSearchQueryId(
        searchQueryId: Long,
    ): List<WallhavenWallpaperEntity> {
        throw RuntimeException()
    }

    override suspend fun getAllByWallhavenIds(
        wallhavenIds: Collection<String>,
    ): List<WallhavenWallpaperEntity> {
        throw RuntimeException()
    }

    override suspend fun getAllWallhavenIds(): List<String> {
        throw RuntimeException()
    }

    override suspend fun deleteAllUniqueToSearchQueryId(searchQueryId: Long) {
        throw RuntimeException()
    }

    override suspend fun upsertWallpaperUploaderMappings(
        vararg wallpaperUploader: WallhavenWallpaperUploaderEntity,
    ) {
        throw RuntimeException()
    }

    override suspend fun insertWallpaperTagMappings(
        vararg wallpaperTag: WallhavenWallpaperTagsEntity,
    ) {
        throw RuntimeException()
    }

    override suspend fun insertWallpaperTagMappings(
        wallpaperTags: Collection<WallhavenWallpaperTagsEntity>,
    ) {
        throw RuntimeException()
    }

    override suspend fun deleteWallpaperTagMappings(wallpaperId: Long) {
        throw RuntimeException()
    }
}

internal open class FakeRedditWallpapersDao : RedditWallpapersDao {
    override suspend fun getById(id: Long) = throw RuntimeException()

    override suspend fun getAll() = throw RuntimeException()

    override suspend fun getAllIds() = throw RuntimeException()

    override suspend fun getByPostIds(postIds: Collection<String>) = throw RuntimeException()

    override suspend fun getByRedditId(redditId: String) = throw RuntimeException()

    override suspend fun getByRedditIdsUpTo999Items(
        redditIds: Collection<String>,
    ) = throw RuntimeException()

    override suspend fun getByRedditIds(redditIds: Collection<String>) = throw RuntimeException()

    override suspend fun getAllRedditIds() = throw RuntimeException()

    override suspend fun getAllUniqueToSearchQueryId(searchQueryId: Long) = throw RuntimeException()

    override suspend fun count() = throw RuntimeException()

    override fun pagingSource(queryString: String) = throw RuntimeException()

    override suspend fun insert(
        wallpapers: Collection<RedditWallpaperEntity>,
    ) = throw RuntimeException()

    override suspend fun deleteAllUniqueToSearchQueryId(
        searchQueryId: Long,
    ) = throw RuntimeException()
}

internal val fakeOkHttpClient = object : OkHttpClient() {
    override fun newCall(request: Request): Call {
        // Overriding to throw error if download requested
        throw IllegalStateException("Test called okhttp client!")
    }
}

internal open class FakeLocalWallpapersRepository : LocalWallpapersRepository {
    override fun wallpapersPager(
        context: Context,
        uris: Collection<Uri>,
        sort: LocalSort,
    ): Flow<PagingData<Wallpaper>> {
        throw RuntimeException()
    }

    override fun wallpaper(
        context: Context,
        wallpaperUriStr: String,
    ): Flow<Resource<LocalWallpaper?>> {
        throw RuntimeException()
    }

    override suspend fun getRandom(
        context: Context,
        uris: Collection<Uri>,
    ): Wallpaper? {
        throw RuntimeException()
    }

    override suspend fun getFirstFresh(
        context: Context,
        uris: Collection<Uri>,
        excluding: Collection<Wallpaper>,
    ): Wallpaper? = throw RuntimeException()

    override suspend fun getByOldestSetOn(
        context: Context,
        excluding: Collection<Wallpaper>,
    ): Wallpaper? {
        throw RuntimeException()
    }

    override suspend fun getCountExcludingWallpapers(
        context: Context,
        uris: Collection<Uri>,
        excluding: Collection<Wallpaper>,
    ): Int {
        throw RuntimeException()
    }
}

internal open class FakeViewedDao : ViewedDao {
    override fun observeAll() = throw RuntimeException()

    override suspend fun getAll() = throw RuntimeException()

    override suspend fun getBySourceIdAndSource(
        sourceId: String,
        source: Source,
    ) = throw RuntimeException()

    override suspend fun insertAll(entities: Collection<ViewedEntity>) = throw RuntimeException()

    override suspend fun upsert(viewedEntity: ViewedEntity) = throw RuntimeException()

    override suspend fun deleteAll() = throw RuntimeException()
}

internal open class FakeLightDarkDao : LightDarkDao {
    override fun observeAll() = throw RuntimeException()

    override suspend fun getAll() = throw RuntimeException()

    override suspend fun getBySourceIdAndSource(
        sourceId: String,
        source: Source,
    ) = throw RuntimeException()

    override suspend fun getRandomByTypeFlag(typeFlags: Set<Int>) = throw RuntimeException()

    override suspend fun getFirstFreshByTypeFlagsAndIdNotIn(
        typeFlags: Set<Int>,
        excludingIds: Collection<Long>,
    ): LightDarkEntity? = throw RuntimeException()

    override suspend fun getByOldestSetOnAndTypeFlagsAndIdsNotId(
        typeFlags: Set<Int>,
        excludingIds: Collection<Long>,
    ) = throw RuntimeException()

    override suspend fun getIdsBySourceIdsAndSource(
        sourceIds: Collection<String>,
        source: Source,
    ): List<Long> {
        throw RuntimeException()
    }

    override suspend fun getCountWhereTypeFlagsAndIdsNotIn(
        typeFlags: Set<Int>,
        ids: Collection<Long>,
    ): Int {
        throw RuntimeException()
    }

    // override suspend fun getAllInHistoryByTypeFlags(): List<AutoWallpaperHistoryEntity> {
    //     throw RuntimeException()
    // }

    override fun pagingSource() = throw RuntimeException()

    override fun observeTypeFlags(sourceId: String, source: Source) = throw RuntimeException()

    override fun observeCount() = throw RuntimeException()

    override suspend fun upsert(lightDarkEntity: LightDarkEntity) = throw RuntimeException()

    override suspend fun deleteBySourceIdAndSource(
        sourceId: String,
        source: Source,
    ) = throw RuntimeException()

    override suspend fun insertAll(
        lightDarkEntities: Collection<LightDarkEntity>,
    ) = throw RuntimeException()
}
