package com.ammar.wallflow.data.db.dao.wallhaven

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenUploaderEntity

@Dao
interface WallhavenUploadersDao {
    @Query("SELECT * FROM wallhaven_uploaders")
    suspend fun getAll(): List<WallhavenUploaderEntity>

    @Query("SELECT * FROM wallhaven_uploaders WHERE username = :username")
    suspend fun getByUsername(username: String): WallhavenUploaderEntity?

    @Query("SELECT * FROM wallhaven_uploaders WHERE username in (:usernames)")
    suspend fun getByUsernames(usernames: Collection<String>): List<WallhavenUploaderEntity>

    @Query("SELECT id FROM wallhaven_uploaders WHERE username = :username")
    suspend fun getIdByUsername(username: String): Long?

    @Query("DELETE FROM wallhaven_uploaders")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(vararg uploader: WallhavenUploaderEntity): List<Long>

    @Insert
    suspend fun insert(uploaders: Collection<WallhavenUploaderEntity>): List<Long>
}
