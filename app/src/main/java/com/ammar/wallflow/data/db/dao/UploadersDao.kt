package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ammar.wallflow.data.db.entity.UploaderEntity

@Dao
interface UploadersDao {
    @Query("SELECT * FROM uploaders")
    suspend fun getAll(): List<UploaderEntity>

    @Query("SELECT * FROM uploaders WHERE username = :username")
    suspend fun getByUsername(username: String): UploaderEntity?

    @Query("SELECT * FROM uploaders WHERE username in (:usernames)")
    suspend fun getByUsernames(usernames: Collection<String>): List<UploaderEntity>

    @Query("SELECT id FROM uploaders WHERE username = :username")
    suspend fun getIdByUsername(username: String): Long?

    @Query("DELETE FROM uploaders")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(vararg uploader: UploaderEntity): List<Long>

    @Insert
    suspend fun insert(uploaders: Collection<UploaderEntity>): List<Long>
}
