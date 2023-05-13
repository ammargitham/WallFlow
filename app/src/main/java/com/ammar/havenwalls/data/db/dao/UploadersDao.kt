package com.ammar.havenwalls.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ammar.havenwalls.data.db.entity.UploaderEntity

@Dao
interface UploadersDao {
    @Query("SELECT * FROM uploaders WHERE username = :username")
    suspend fun getByUsername(username: String): UploaderEntity?

    @Query("SELECT id FROM uploaders WHERE username = :username")
    suspend fun getIdByUsername(username: String): Long?

    @Query("DELETE FROM uploaders")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(vararg uploader: UploaderEntity): List<Long>

    @Insert
    suspend fun insert(uploaders: Collection<UploaderEntity>): List<Long>
}
