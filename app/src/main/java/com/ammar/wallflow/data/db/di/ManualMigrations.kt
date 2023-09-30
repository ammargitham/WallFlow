package com.ammar.wallflow.data.db.di

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ManualMigrations {
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

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                // language=sql
                "ALTER TABLE tags RENAME TO wallhaven_tags",
            )
            db.execSQL(
                // language=sql
                """
                    CREATE TABLE IF NOT EXISTS wallhaven_popular_tags (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tag_id` INTEGER NOT NULL,
                        FOREIGN KEY(`tag_id`) REFERENCES `wallhaven_tags`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent(),
            )
            db.execSQL(
                // language=sql
                """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_wallhaven_popular_tags_tag_id
                    ON wallhaven_popular_tags(tag_id)
                """.trimIndent(),
            )
            db.execSQL(
                // language=sql
                "INSERT INTO wallhaven_popular_tags SELECT * FROM popular_tags",
            )
            db.execSQL(
                // language=sql
                "DROP TABLE popular_tags",
            )
            db.execSQL(
                // language=sql
                "ALTER TABLE uploaders RENAME TO wallhaven_uploaders",
            )
            db.execSQL(
                // language=sql
                "ALTER TABLE wallpapers RENAME TO wallhaven_wallpapers",
            )
            db.execSQL(
                // language=sql
                "ALTER TABLE wallpaper_tags RENAME TO wallhaven_wallpaper_tags",
            )
            db.execSQL(
                // language=sql
                """
                    CREATE TABLE IF NOT EXISTS wallhaven_search_query_wallpapers (
                        `search_query_id` INTEGER NOT NULL,
                        `wallpaper_id` INTEGER NOT NULL,
                        PRIMARY KEY(`search_query_id`, `wallpaper_id`),
                        FOREIGN KEY(`search_query_id`) REFERENCES `search_query`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE ,
                        FOREIGN KEY(`wallpaper_id`) REFERENCES `wallhaven_wallpapers`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent(),
            )
            db.execSQL(
                // language=sql
                """
                    CREATE INDEX IF NOT EXISTS
                    `index_wallhaven_search_query_wallpapers_wallpaper_id`
                    ON `wallhaven_search_query_wallpapers` (`wallpaper_id`)
                """.trimIndent(),
            )
            db.execSQL(
                // language=sql
                """
                    INSERT INTO wallhaven_search_query_wallpapers
                    SELECT * FROM search_query_wallpapers
                """.trimMargin(),
            )
            db.execSQL(
                // language=sql
                "DROP TABLE search_query_wallpapers",
            )
        }
    }
}
