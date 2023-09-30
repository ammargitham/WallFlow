package com.ammar.wallflow.data.db.automigrationspecs

import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec

@RenameTable.Entries(
    RenameTable(fromTableName = "tags", toTableName = "wallhaven_tags"),
    RenameTable(fromTableName = "popular_tags", toTableName = "wallhaven_popular_tags"),
    RenameTable(fromTableName = "uploaders", toTableName = "wallhaven_uploaders"),
    RenameTable(fromTableName = "wallpapers", toTableName = "wallhaven_wallpapers"),
    RenameTable(fromTableName = "wallpaper_tags", toTableName = "wallhaven_wallpaper_tags"),
    RenameTable(
        fromTableName = "search_query_wallpapers",
        toTableName = "wallhaven_search_query_wallpapers",
    ),
)
class AutoMigration3To4Spec : AutoMigrationSpec
