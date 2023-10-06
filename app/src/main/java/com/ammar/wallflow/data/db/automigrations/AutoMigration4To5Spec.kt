package com.ammar.wallflow.data.db.automigrations

import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec

@RenameTable(
    fromTableName = "saved_searches",
    toTableName = "wallhaven_saved_searches",
)
@RenameTable(
    fromTableName = "search_history",
    toTableName = "wallhaven_search_history",
)
class AutoMigration4To5Spec : AutoMigrationSpec
