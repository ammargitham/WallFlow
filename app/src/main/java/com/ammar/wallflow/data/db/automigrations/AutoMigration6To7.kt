package com.ammar.wallflow.data.db.automigrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(
    tableName = "wallhaven_wallpapers",
    columnName = "uploader_id",
)
class AutoMigration6To7 : AutoMigrationSpec
