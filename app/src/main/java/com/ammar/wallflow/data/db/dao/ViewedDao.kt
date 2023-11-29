package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.ViewedEntity
import com.ammar.wallflow.model.Source
import kotlinx.coroutines.flow.Flow

@Dao
interface ViewedDao {
    @Query("SELECT * FROM viewed ORDER BY last_viewed_on DESC")
    fun observeAll(): Flow<List<ViewedEntity>>

    @Query("SELECT * FROM viewed ORDER BY last_viewed_on DESC")
    suspend fun getAll(): List<ViewedEntity>

    @Query("SELECT * FROM viewed WHERE source_id = :sourceId AND source = :source")
    suspend fun getBySourceIdAndSource(sourceId: String, source: Source): ViewedEntity?

    @Insert
    suspend fun insertAll(entities: Collection<ViewedEntity>)

    @Upsert
    suspend fun upsert(viewedEntity: ViewedEntity)

    @Query("DELETE FROM viewed")
    suspend fun deleteAll()
}
