package com.ammar.wallflow.data.db.manualmigrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ammar.wallflow.model.search.migrateWallhavenFiltersQSToWallhavenFiltersJson
import com.ammar.wallflow.model.search.migrateWallhavenFiltersQSToWallhavenSearchJson

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

        // alter search_query_remote_keys next_page_number(INTEGER) to next_page(TEXT)
        db.execSQL(
            // language=sql
            """
                ALTER TABLE search_query_remote_keys
                ADD COLUMN next_page TEXT
            """.trimIndent(),
        )
        db.execSQL(
            // language=sql
            """
                UPDATE search_query_remote_keys
                SET next_page = CAST(next_page_number AS TEXT)
            """.trimIndent(),
        )
        db.execSQL(
            // language=sql
            """
                ALTER TABLE search_query_remote_keys
                DROP COLUMN next_page_number
            """.trimIndent(),
        )

        // create table rate_limits
        db.execSQL(
            // language=sql
            """
                CREATE TABLE IF NOT EXISTS `rate_limits` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `source` TEXT NOT NULL,
                    `limit` INTEGER,
                    `remaining` INTEGER,
                    `reset` INTEGER
                )
            """.trimIndent(),
        )
        db.execSQL(
            // language=sql
            """
                CREATE UNIQUE INDEX IF NOT EXISTS `index_rate_limits_source`
                ON `rate_limits` (`source`)
            """.trimIndent(),
        )

        // migrate search_query
        var currentMap = mutableMapOf<Long, String>()
        db.query(
            // language=sql
            "SELECT * FROM search_query",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                currentMap[cursor.getLong(0)] = cursor.getString(
                    cursor.getColumnIndexOrThrow("query_string"),
                )
            }
        }
        var updatedMap = currentMap.mapValues {
            migrateWallhavenFiltersQSToWallhavenSearchJson(it.value)
        }
        updatedMap.forEach { (id, queryString) ->
            // language=sql
            db.execSQL(
                """
            UPDATE search_query
            SET query_string = '$queryString'
            WHERE id = $id
        """.trimIndent(),
            )
        }

        // migrate saved_searches filters
        currentMap = mutableMapOf()
        db.query(
            // language=sql
            "SELECT * FROM saved_searches",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                currentMap[cursor.getLong(0)] = cursor.getString(
                    cursor.getColumnIndexOrThrow("filters"),
                )
            }
        }
        updatedMap = currentMap.mapValues {
            migrateWallhavenFiltersQSToWallhavenFiltersJson(it.value)
        }
        updatedMap.forEach { (id, queryString) ->
            // language=sql
            db.execSQL(
                """
            UPDATE saved_searches
            SET filters = '$queryString'
            WHERE id = $id
        """.trimIndent(),
            )
        }

        // migrate search_history filters
        currentMap = mutableMapOf()
        db.query(
            // language=sql
            "SELECT * FROM search_history",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                currentMap[cursor.getLong(0)] = cursor.getString(
                    cursor.getColumnIndexOrThrow("filters"),
                )
            }
        }
        updatedMap = currentMap.mapValues {
            migrateWallhavenFiltersQSToWallhavenFiltersJson(it.value)
        }
        updatedMap.forEach { (id, queryString) ->
            // language=sql
            db.execSQL(
                """
            UPDATE search_history
            SET filters = '$queryString'
            WHERE id = $id
        """.trimIndent(),
            )
        }
    }
}
