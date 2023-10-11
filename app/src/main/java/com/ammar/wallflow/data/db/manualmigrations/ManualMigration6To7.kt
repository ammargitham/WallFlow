package com.ammar.wallflow.data.db.manualmigrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ammar.wallflow.model.search.migrateWallhavenFiltersQSToWallhavenSearchJson

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val currentQSMap = mutableMapOf<Long, String>()
        db.query(
            // language=sql
            "SELECT * FROM wallhaven_search_query",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                currentQSMap[cursor.getLong(0)] = cursor.getString(1)
            }
        }
        val updatedMap = currentQSMap.mapValues {
            migrateWallhavenFiltersQSToWallhavenSearchJson(it.value)
        }
        updatedMap.forEach { (id, queryString) ->
            // language=sql
            db.execSQL(
                """
                    UPDATE wallhaven_search_query
                    SET query_string = '$queryString'
                    WHERE ID = $id
                """.trimIndent(),
            )
        }
    }
}
