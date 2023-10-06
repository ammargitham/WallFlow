package com.ammar.wallflow.data.db.manualmigrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            // language=sql
            """
                    CREATE TABLE favorites (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        source_id TEXT NOT NULL,
                        source TEXT NOT NULL,
                        favorited_on INTEGER NOT NULL
                    )
            """.trimIndent(),
        )
        db.execSQL(
            // language=sql
            """
                    CREATE UNIQUE INDEX index_favorites_source_id_source
                    ON favorites (source_id, source)
            """.trimIndent(),
        )
        db.execSQL(
            // language=sql
            """
                    CREATE TABLE IF NOT EXISTS auto_wallpaper_history_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        source_id TEXT NOT NULL,
                        source TEXT NOT NULL,
                        set_on INTEGER NOT NULL
                    )
            """.trimIndent(),
        )
        db.execSQL(
            // language=sql
            """
                    INSERT INTO auto_wallpaper_history_temp(
                        id,
                        source_id,
                        source,
                        set_on
                    )
                    SELECT
                        id,
                        wallhaven_id,
                        'WALLHAVEN',
                        set_on
                    FROM auto_wallpaper_history
            """.trimIndent(),
        )
        // language=sql
        db.execSQL("DROP TABLE auto_wallpaper_history")
        db.execSQL(
            // language=sql
            "ALTER TABLE auto_wallpaper_history_temp RENAME TO auto_wallpaper_history",
        )
        db.execSQL(
            // language=sql
            """
                    CREATE UNIQUE INDEX index_auto_wallpaper_history_source_id
                    ON auto_wallpaper_history (source_id)
            """.trimIndent(),
        )
    }
}
