package com.ammar.wallflow.workers

import android.content.Context
import android.net.Uri
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.ammar.wallflow.data.db.dao.AutoWallpaperHistoryDao
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.ObjectDetectionModelDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenSavedSearchDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSavedSearchEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperTagsEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallpaperWithUploaderAndTags
import com.ammar.wallflow.data.network.WallhavenNetworkDataSource
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpaperResponse
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpapersResponse
import com.ammar.wallflow.data.repository.local.LocalWallpapersRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.local.LocalWallpaper
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.ui.screens.local.LocalSort
import kotlinx.coroutines.flow.Flow
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document

internal open class FakeSavedSearchDao : WallhavenSavedSearchDao {
    override fun observeAll(): Flow<List<WallhavenSavedSearchEntity>> {
        throw RuntimeException()
    }

    override suspend fun getAll(): List<WallhavenSavedSearchEntity> {
        throw RuntimeException()
    }

    override suspend fun getAllByNames(
        names: Collection<String>,
    ): List<WallhavenSavedSearchEntity> {
        throw RuntimeException()
    }

    override suspend fun getById(id: Long): WallhavenSavedSearchEntity? {
        throw RuntimeException()
    }

    override suspend fun getByName(name: String): WallhavenSavedSearchEntity? {
        throw RuntimeException()
    }

    override suspend fun upsert(savedSearch: WallhavenSavedSearchEntity) {
        throw RuntimeException()
    }

    override suspend fun upsert(savedSearchDaos: Collection<WallhavenSavedSearchEntity>) {
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
}
