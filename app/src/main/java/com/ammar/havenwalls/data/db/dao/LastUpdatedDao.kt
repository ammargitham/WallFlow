package com.ammar.havenwalls.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.havenwalls.data.db.entity.LastUpdatedEntity

@Dao
interface LastUpdatedDao {
    @Query("SELECT * FROM last_updated WHERE `key` = :key")
    suspend fun getByKey(key: String): LastUpdatedEntity?

    @Upsert
    suspend fun upsert(vararg lastUpdate: LastUpdatedEntity)
}
