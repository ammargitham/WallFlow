package com.ammar.wallflow.workers

import androidx.paging.PagingSource
import com.ammar.wallflow.data.db.dao.AutoWallpaperHistoryDao
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.ObjectDetectionModelDao
import com.ammar.wallflow.data.db.dao.SavedSearchDao
import com.ammar.wallflow.data.db.dao.WallpapersDao
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import com.ammar.wallflow.data.db.entity.SavedSearchEntity
import com.ammar.wallflow.data.db.entity.WallpaperEntity
import com.ammar.wallflow.data.db.entity.WallpaperTagsEntity
import com.ammar.wallflow.data.db.entity.WallpaperWithUploaderAndTags
import com.ammar.wallflow.data.network.WallhavenNetworkDataSource
import com.ammar.wallflow.data.network.model.NetworkResponse
import com.ammar.wallflow.data.network.model.NetworkWallhavenWallpaper
import com.ammar.wallflow.model.SearchQuery
import com.ammar.wallflow.model.Source
import kotlinx.coroutines.flow.Flow
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document

internal open class FakeSavedSearchDao : SavedSearchDao {
    override fun getAll(): Flow<List<SavedSearchEntity>> {
        throw RuntimeException()
    }

    override suspend fun getById(id: Long): SavedSearchEntity? {
        throw RuntimeException()
    }

    override suspend fun getByName(name: String): SavedSearchEntity? {
        throw RuntimeException()
    }

    override suspend fun upsert(savedSearchDao: SavedSearchEntity) {
        throw RuntimeException()
    }

    override suspend fun deleteByName(name: String) {
        throw RuntimeException()
    }
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

    override suspend fun getBySourceId(
        sourceId: String,
        source: Source,
    ): AutoWallpaperHistoryEntity? {
        throw RuntimeException()
    }

    override suspend fun upsert(vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity) {
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
        searchQuery: SearchQuery,
        page: Int?,
    ): NetworkResponse<List<NetworkWallhavenWallpaper>> {
        throw RuntimeException()
    }

    override suspend fun wallpaper(
        wallpaperWallhavenId: String,
    ): NetworkResponse<NetworkWallhavenWallpaper> {
        throw RuntimeException()
    }

    override suspend fun popularTags(): Document? {
        throw RuntimeException()
    }
}

internal open class FakeFavoriteDao : FavoriteDao {
    override fun observeAll(): Flow<List<FavoriteEntity>> {
        throw RuntimeException()
    }

    override fun pagingSource(): PagingSource<Int, FavoriteEntity> {
        throw RuntimeException()
    }

    override suspend fun exists(sourceId: String, source: Source): Boolean {
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

    override suspend fun insertAll(favoriteEntities: Collection<FavoriteEntity>) {
        throw RuntimeException()
    }

    override suspend fun upsert(favoriteEntity: FavoriteEntity) {
        throw RuntimeException()
    }

    override suspend fun deleteBySourceIdAndType(sourceId: String, source: Source) {
        throw RuntimeException()
    }
}

internal open class FakeWallpapersDao : WallpapersDao {
    override suspend fun getAll(): List<WallpaperEntity> {
        throw RuntimeException()
    }

    override suspend fun getByWallhavenId(wallhavenId: String): WallpaperEntity? {
        throw RuntimeException()
    }

    override suspend fun getWithUploaderAndTagsByWallhavenId(
        wallhavenId: String,
    ): WallpaperWithUploaderAndTags? {
        throw RuntimeException()
    }

    override suspend fun getByWallhavenIds(wallhavenIds: List<String>): List<WallpaperEntity> {
        throw RuntimeException()
    }

    override fun pagingSource(queryString: String): PagingSource<Int, WallpaperEntity> {
        throw RuntimeException()
    }

    override suspend fun count(): Int {
        throw RuntimeException()
    }

    override suspend fun deleteAll() {
        throw RuntimeException()
    }

    override suspend fun insert(vararg wallpaper: WallpaperEntity): List<Long> {
        throw RuntimeException()
    }

    override suspend fun insert(wallpapers: Collection<WallpaperEntity>): List<Long> {
        throw RuntimeException()
    }

    override suspend fun update(vararg wallpaper: WallpaperEntity) {
        throw RuntimeException()
    }

    override suspend fun upsert(vararg wallpaper: WallpaperEntity): List<Long> {
        throw RuntimeException()
    }

    override suspend fun getAllUniqueToSearchQueryId(searchQueryId: Long): List<WallpaperEntity> {
        throw RuntimeException()
    }

    override suspend fun deleteAllUniqueToSearchQueryId(searchQueryId: Long) {
        throw RuntimeException()
    }

    override suspend fun insertWallpaperTagMappings(vararg wallpaperTag: WallpaperTagsEntity) {
        throw RuntimeException()
    }

    override suspend fun insertWallpaperTagMappings(
        wallpaperTags: Collection<WallpaperTagsEntity>,
    ) {
        throw RuntimeException()
    }

    override suspend fun deleteWallpaperTagMappings(wallpaperId: Long) {
        throw RuntimeException()
    }
}

internal val fakeOkHttpClient = object : OkHttpClient() {
    override fun newCall(request: Request): Call {
        // Overriding to throw error if download requested
        throw IllegalStateException("Test called okhttp client!")
    }
}
