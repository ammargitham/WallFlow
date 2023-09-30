package com.ammar.wallflow.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ammar.wallflow.data.db.automigrationspecs.AutoMigration3To4Spec
import com.ammar.wallflow.data.db.di.ManualMigrations.MIGRATION_1_2
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDbName = "migration-test"
    private val allManualMigrations = arrayOf(
        MIGRATION_1_2,
    )

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java,
        specs = listOf(
            AutoMigration3To4Spec(),
        ),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        helper.createDatabase(testDbName, 1).apply {
            close()
        }
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            testDbName,
        ).addMigrations(*allManualMigrations).build().apply {
            openHelper.writableDatabase.close()
        }
    }
}
