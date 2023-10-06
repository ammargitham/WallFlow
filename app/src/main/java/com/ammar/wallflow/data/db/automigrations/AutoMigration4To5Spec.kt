package com.ammar.wallflow.data.db.automigrations

import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec

@RenameTable.Entries(
    RenameTable(
        fromTableName = "saved_searches",
        toTableName = "wallhaven_saved_searches",
    ),
    RenameTable(
        fromTableName = "search_history",
        toTableName = "wallhaven_search_history",
    ),
    RenameTable(
        fromTableName = "search_query_remote_keys",
        toTableName = "wallhaven_search_query_remote_keys",
    ),
    RenameTable(
        fromTableName = "search_query",
        toTableName = "wallhaven_search_query",
    ),
)
class AutoMigration4To5Spec : AutoMigrationSpec
