package com.ammar.wallflow.data.db

import androidx.core.database.getLongOrNull
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ammar.wallflow.data.db.di.allManualMigrations
import com.ammar.wallflow.data.db.manualmigrations.MIGRATION_3_4
import com.ammar.wallflow.data.db.manualmigrations.MIGRATION_5_6
import java.io.IOException
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java,
        specs = emptyList(),
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

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        helper.createDatabase(testDbName, 3).apply {
            execSQL(
                // language=sql
                """
                    INSERT INTO tags
                        ("id", "wallhaven_id", "name", "alias", "category_id", "category", "purity", "created_at")
                    VALUES
                        ('1', '8099', 'Tifa Lockhart', '', '49', 'Fictional Characters', 'sfw', '1411675211000'),
                        ('2', '37', 'nature', '', '5', 'Nature', 'sfw', '1391369096000'),
                        ('3', '65348', '4K', '', '2', 'Art & Design', 'sfw', '1503522417000'),
                        ('4', '323', 'artwork', '', '2', 'Art & Design', 'sfw', '1392110323000'),
                        ('5', '175', 'anime boys', '', '20', 'Characters', 'sfw', '1391907044000'),
                        ('6', '2729', 'sky', '', '5', 'Nature', 'sfw', '1403281238000'),
                        ('7', '328', 'mountains', '', '41', 'Landscapes', 'sfw', '1392135639000'),
                        ('8', '314', 'car', '', '54', 'Cars & Motorcycles', 'sfw', '1392093149000'),
                        ('9', '3834', 'schoolgirl', '', '7', 'People', 'sfw', '1410116773000'),
                        ('10', '141554', 'Kafka (Honkai: Star Rail)', '', '20', 'Characters', 'sfw', '1676084898000');
                """.trimIndent(),
            )
            execSQL(
                // language=sql
                """
                    INSERT INTO popular_tags
                        ("id", "tag_id")
                    VALUES
                        ('201', '1'),
                        ('202', '2'),
                        ('203', '3'),
                        ('204', '4'),
                        ('205', '5'),
                        ('206', '6'),
                        ('207', '7'),
                        ('208', '8'),
                        ('209', '9'),
                        ('210', '10');
                """.trimIndent(),
            )
            execSQL(
                // language=sql
                """
                    INSERT INTO search_query
                        ("id", "query_string", "last_updated_on")
                    VALUES
                        (
                            '1',
                            'includedTags=test&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed=',
                            '12345'
                        ),
                        (
                            '2',
                            'includedTags=test1&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed=',
                            '12345'
                        ),
                        (
                            '3',
                            'includedTags=test2&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed=',
                            '12345'
                        ),
                        (
                            '4',
                            'includedTags=&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed=',
                            '12345'
                        );
                """.trimIndent(),
            )
            execSQL(
                // language=sql
                """
                    INSERT INTO search_query_remote_keys
                        ("id", "search_query_id", "next_page_number")
                    VALUES
                        ('11', '2', '5'),
                        ('12', '1', NULL),
                        ('13', '3', '5');
                """.trimIndent(),
            )
            execSQL(
                // language=sql
                """
                    INSERT INTO saved_searches
                        ("id", "name", "query", "filters")
                    VALUES
                        (
                            '1',
                            'home',
                            'test',
                            'includedTags=&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed='
                        );
                """.trimIndent(),
            )
            execSQL(
                // language=sql
                """
                    INSERT INTO search_history
                        ("id", "query", "filters", "last_updated_on")
                    VALUES
                        (
                            '1',
                            'test',
                            'includedTags=&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed=',
                            '12345'
                        );
                """.trimIndent(),
            )
            close()
        }
        helper.runMigrationsAndValidate(
            testDbName,
            4,
            true,
            MIGRATION_3_4,
        ).use { db ->
            // language=sql
            db.query("SELECT COUNT(*) from wallhaven_popular_tags").use {
                it.moveToFirst()
                val count = it.getInt(0)
                assertEquals(10, count)
            }

            // language=sql
            db.query("SELECT COUNT(*) from wallhaven_tags").use {
                it.moveToFirst()
                val count = it.getInt(0)
                assertEquals(10, count)
            }

            // language=sql
            db.query("SELECT COUNT(*) from search_query_remote_keys").use {
                it.moveToFirst()
                val count = it.getInt(0)
                assertEquals(3, count)
            }

            // language=sql
            db.query("SELECT * from search_query_remote_keys").use {
                it.moveToFirst()
                val id = it.getInt(0)
                assertEquals(11, id)
                val searchQueryId = it.getInt(1)
                assertEquals(2, searchQueryId)
                val nextPageNumber = it.getInt(2)
                assertEquals(5, nextPageNumber)
            }

            db.query(
                // language=sql
                "SELECT * from search_query",
            ).use {
                it.moveToFirst()
                val queryString = it.getString(
                    it.getColumnIndexOrThrow("query_string"),
                )
                assertEquals(
                    "{\"filters\":{\"includedTags\":[\"test\"]," +
                        "\"sorting\":\"TOPLIST\",\"topRange\":\"ONE_DAY\"}}",
                    queryString,
                )
            }

            db.query(
                // language=sql
                "SELECT * from saved_searches",
            ).use {
                it.moveToFirst()
                val filtersStr = it.getString(
                    it.getColumnIndexOrThrow("filters"),
                )
                assertEquals(
                    "{\"sorting\":\"TOPLIST\",\"topRange\":\"ONE_DAY\"}",
                    filtersStr,
                )
            }

            db.query(
                // language=sql
                "SELECT * from search_history",
            ).use {
                it.moveToFirst()
                val filtersStr = it.getString(
                    it.getColumnIndexOrThrow("filters"),
                )
                assertEquals(
                    "{\"sorting\":\"TOPLIST\",\"topRange\":\"ONE_DAY\"}",
                    filtersStr,
                )
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        helper.createDatabase(testDbName, 5).apply {
            execSQL(
                // language=sql
                """
                    INSERT INTO wallhaven_uploaders (
                        "id",
                        "username",
                        "group",
                        "avatar_large",
                        "avatar_medium",
                        "avatar_small",
                        "avatar_tiny"
                    )
                    VALUES
                    (
                        '1',
                        'KyofuRex',
                        'User',
                        'https://wallhaven.cc/images/user/avatar/200/324243_f524fcc35933818cd2becdeda3146e02508e23dd74bd86daaecad170df92b8f6.jpg',
                        'https://wallhaven.cc/images/user/avatar/128/324243_f524fcc35933818cd2becdeda3146e02508e23dd74bd86daaecad170df92b8f6.jpg',
                        'https://wallhaven.cc/images/user/avatar/32/324243_f524fcc35933818cd2becdeda3146e02508e23dd74bd86daaecad170df92b8f6.jpg',
                        'https://wallhaven.cc/images/user/avatar/20/324243_f524fcc35933818cd2becdeda3146e02508e23dd74bd86daaecad170df92b8f6.jpg'
                    ),
                    (
                        '2',
                        'qqh48123456',
                        'User',
                        'https://wallhaven.cc/images/user/avatar/200/default-avatar.jpg',
                        'https://wallhaven.cc/images/user/avatar/128/default-avatar.jpg',
                        'https://wallhaven.cc/images/user/avatar/32/default-avatar.jpg',
                        'https://wallhaven.cc/images/user/avatar/20/default-avatar.jpg'
                    );
                """.trimIndent(),
            )
            execSQL(
                // language=sql
                """
                    INSERT INTO wallhaven_wallpapers (
                        "id",
                        "wallhaven_id",
                        "url",
                        "short_url",
                        "uploader_id",
                        "views",
                        "favorites",
                        "source",
                        "purity",
                        "category",
                        "dimension_x",
                        "dimension_y",
                        "file_size",
                        "file_type",
                        "created_at",
                        "colors",
                        "path",
                        "thumb_large",
                        "thumb_original",
                        "thumb_small"
                    )
                    VALUES
                    (
                        '1',
                        '9dkeow',
                        'https://wallhaven.cc/w/9dkeow',
                        'https://whvn.cc/9dkeow',
                        '1',
                        '527',
                        '11',
                        'https://www.artstation.com/artwork/nY8N34',
                        'sfw',
                        'general',
                        '1920',
                        '1094',
                        '878713',
                        'image/jpeg',
                        '1698683889000',
                        '["#424153","#000000","#999999","#e7d8b1","#cccccc"]',
                        'https://w.wallhaven.cc/full/9d/wallhaven-9dkeow.jpg',
                        'https://th.wallhaven.cc/lg/9d/9dkeow.jpg',
                        'https://th.wallhaven.cc/orig/9d/9dkeow.jpg',
                        'https://th.wallhaven.cc/small/9d/9dkeow.jpg'
                    ),
                    (
                        '2',
                        'kxjrz6',
                        'https://wallhaven.cc/w/kxjrz6',
                        'https://whvn.cc/kxjrz6',
                        '2',
                        '250',
                        '16',
                        'https://twitter.com/umiu/status/1346091299171012609',
                        'sfw',
                        'anime',
                        '4093',
                        '2894',
                        '16605236',
                        'image/png',
                        '1698685975000',
                        '["#999999","#424153","#cccccc","#abbcda","#663399"]',
                        'https://w.wallhaven.cc/full/kx/wallhaven-kxjrz6.png',
                        'https://th.wallhaven.cc/lg/kx/kxjrz6.jpg',
                        'https://th.wallhaven.cc/orig/kx/kxjrz6.jpg',
                        'https://th.wallhaven.cc/small/kx/kxjrz6.jpg'
                    ),
                    (
                        '3',
                        '9dky2w',
                        'https://wallhaven.cc/w/9dky2w',
                        'https://whvn.cc/9dky2w',
                        NULL,
                        '149',
                        '5',
                        'https://www.deviantart.com/rbatinic',
                        'sfw',
                        'general',
                        '3840',
                        '2160',
                        '5485052',
                        'image/jpeg',
                        '1699099314000',
                        '["#999999","#424153","#e7d8b1","#996633","#cccccc"]',
                        'https://w.wallhaven.cc/full/9d/wallhaven-9dky2w.jpg',
                        'https://th.wallhaven.cc/lg/9d/9dky2w.jpg',
                        'https://th.wallhaven.cc/orig/9d/9dky2w.jpg',
                        'https://th.wallhaven.cc/small/9d/9dky2w.jpg'
                    );
                """.trimIndent(),
            )
            close()
        }
        helper.runMigrationsAndValidate(
            testDbName,
            6,
            true,
            MIGRATION_5_6,
        ).use { db ->
            // language=sql
            val idMap = db.query("SELECT * from wallhaven_wallpaper_uploaders").use {
                val wallpaperUploaderMap = mutableMapOf<Long, Long?>()
                while (it.moveToNext()) {
                    val wallpaperIdIndex = it.getColumnIndex("wallpaper_id")
                    val wallpaperId = it.getLong(wallpaperIdIndex)
                    val uploaderIdIndex = it.getColumnIndex("uploader_id")
                    val uploaderId = it.getLongOrNull(uploaderIdIndex)
                    wallpaperUploaderMap[wallpaperId] = uploaderId
                }
                wallpaperUploaderMap
            }
            assertEquals(2, idMap.size)
            assertEquals(1, idMap[1])
            assertEquals(2, idMap[2])
        }
    }
}
