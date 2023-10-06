package com.ammar.wallflow.data.db.automigrationspecs

import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec

@RenameTable(
    fromTableName = "saved_searches",
    toTableName = "wallhaven_saved_searches",
)
class AutoMigration4To5Spec : AutoMigrationSpec
