package com.ammar.wallflow.data.db.manualmigrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
