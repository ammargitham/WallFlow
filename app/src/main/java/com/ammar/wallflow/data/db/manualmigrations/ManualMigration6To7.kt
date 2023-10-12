package com.ammar.wallflow.data.db.manualmigrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ammar.wallflow.model.search.migrateWallhavenFiltersQSToWallhavenFiltersJson
import com.ammar.wallflow.model.search.migrateWallhavenFiltersQSToWallhavenSearchJson

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // migrate wallhaven_search_query
        var currentMap = mutableMapOf<Long, String>()
        db.query(
            // language=sql
            "SELECT * FROM wallhaven_search_query",
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
                    UPDATE wallhaven_search_query
                    SET query_string = '$queryString'
                    WHERE id = $id
                """.trimIndent(),
            )
        }

        // migrate wallhaven_saved_searches filters
        currentMap = mutableMapOf()
        db.query(
            // language=sql
            "SELECT * FROM wallhaven_saved_searches",
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
                    UPDATE wallhaven_saved_searches
                    SET filters = '$queryString'
                    WHERE id = $id
                """.trimIndent(),
            )
        }

        // migrate wallhaven_search_history filters
        currentMap = mutableMapOf()
        db.query(
            // language=sql
            "SELECT * FROM wallhaven_search_history",
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
                    UPDATE wallhaven_search_history
                    SET filters = '$queryString'
                    WHERE id = $id
                """.trimIndent(),
            )
        }
    }
}
