package com.ammar.wallflow.data.db.manualmigrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            // language=sql
            """
               CREATE TABLE IF NOT EXISTS `wallhaven_wallpaper_uploaders` (
                   `wallpaper_id` INTEGER NOT NULL,
                   `uploader_id` INTEGER NOT NULL,
                   PRIMARY KEY(`wallpaper_id`, `uploader_id`)
               )
            """.trimIndent(),
        )
        db.execSQL(
            // language=sql
            """
               CREATE INDEX IF NOT EXISTS `index_wallhaven_wallpaper_uploaders_wallpaper_id`
               ON `wallhaven_wallpaper_uploaders` (`wallpaper_id`)
            """.trimIndent(),
        )
        db.execSQL(
            // language=sql
            """
               CREATE INDEX IF NOT EXISTS `index_wallhaven_wallpaper_uploaders_uploader_id`
               ON `wallhaven_wallpaper_uploaders` (`uploader_id`)
            """.trimIndent(),
        )
        db.execSQL(
            // language=sql
            """
               INSERT INTO wallhaven_wallpaper_uploaders (wallpaper_id, uploader_id)
               SELECT id, uploader_id FROM wallhaven_wallpapers ww
               WHERE ww.uploader_id IS NOT NULL
            """.trimIndent(),
        )
    }
}
