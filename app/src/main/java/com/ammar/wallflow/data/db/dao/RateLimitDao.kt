package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.RateLimitEntity
import com.ammar.wallflow.model.Source

@Dao
interface RateLimitDao {
    @Query("SELECT * FROM rate_limits WHERE source = :source")
    suspend fun getBySource(source: Source): RateLimitEntity?

    @Upsert
    fun upsert(entity: RateLimitEntity)
}
