package com.ammar.wallflow.model.search

import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.wallhaven.WallhavenAvatar
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import com.ammar.wallflow.utils.TestClock
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.Instant
import org.junit.Test

class SearchMetaTest {
    @Test
    fun `should convert SearchMeta to string`() {
        val createdAt = TestClock(now = Instant.fromEpochSeconds(1696860179))
        var meta: SearchMeta = WallhavenTagSearchMeta(
            tag = WallhavenTag(
                id = 1,
                name = "test",
                alias = emptyList(),
                categoryId = 1,
                category = "test_cat",
                purity = Purity.SFW,
                createdAt = createdAt.now(),
            ),
        )
        var metaStr = "type=WALLHAVEN_TAG&tag=%7B%22" +
            "id%22%3A1%2C%22" +
            "name%22%3A%22test%22%2C%22" +
            "alias%22%3A%5B%5D%2C%22" +
            "categoryId%22%3A1%2C%22" +
            "category%22%3A%22" +
            "test_cat%22%2C%22purity%22%3A%22SFW%22%2C%22" +
            "createdAt%22%3A%222023-10-09T14%3A02%3A59Z%22%7D"
        assertEquals(
            metaStr,
            meta.toQueryString(),
        )

        meta = WallhavenUploaderSearchMeta(
            uploader = WallhavenUploader(
                username = "test",
                group = "group",
                avatar = WallhavenAvatar(
                    large = "large",
                    medium = "medium",
                    small = "small",
                    tiny = "tiny",
                ),
            ),
        )
        metaStr = "type=WALLHAVEN_UPLOADER&uploader=%7B%22" +
            "username%22%3A%22test%22%2C%22" +
            "group%22%3A%22group%22%2C%22" +
            "avatar%22%3A%7B%22" +
            "large%22%3A%22large%22%2C%22" +
            "medium%22%3A%22medium%22%2C%22" +
            "small%22%3A%22small%22%2C%22" +
            "tiny%22%3A%22tiny%22%7D%7D"
        assertEquals(
            metaStr,
            meta.toQueryString(),
        )
    }

    @Test
    fun `should convert string to SearchMeta`() {
        val createdAt = TestClock(now = Instant.fromEpochSeconds(1696860179))
        var metaStr = "type=WALLHAVEN_TAG&tag=%7B%22" +
            "id%22%3A1%2C%22" +
            "name%22%3A%22test%22%2C%22" +
            "alias%22%3A%5B%5D%2C%22" +
            "categoryId%22%3A1%2C%22" +
            "category%22%3A%22" +
            "test_cat%22%2C%22purity%22%3A%22SFW%22%2C%22" +
            "createdAt%22%3A%222023-10-09T14%3A02%3A59Z%22%7D"
        var meta: SearchMeta = WallhavenTagSearchMeta(
            tag = WallhavenTag(
                id = 1,
                name = "test",
                alias = emptyList(),
                categoryId = 1,
                category = "test_cat",
                purity = Purity.SFW,
                createdAt = createdAt.now(),
            ),
        )
        assertEquals(
            meta,
            SearchMeta.fromQueryString(metaStr),
        )

        // removed some chars from above string to cause exception and return null
        metaStr = "type=WALLHAVEN_TAG&tag=%7B%22" +
            "id%22%3A1%2C%22" +
            "name%22%3A%22test%22%2C%22" +
            "alias%22%3A%5B%5D%2C%22" +
            "categoryId%22%32C%22" +
            "category%22%3A%22" +
            "test_cat%22%2C%22purity%22%3A%22SFW%22%2C%22" +
            "createdAt%22%3A%222023-10-09T14%3A02%3A59Z%22%7D"
        assertNull(SearchMeta.fromQueryString(metaStr))

        metaStr = "type=WALLHAVEN_UPLOADER&uploader=%7B%22" +
            "username%22%3A%22test%22%2C%22" +
            "group%22%3A%22group%22%2C%22" +
            "avatar%22%3A%7B%22" +
            "large%22%3A%22large%22%2C%22" +
            "medium%22%3A%22medium%22%2C%22" +
            "small%22%3A%22small%22%2C%22" +
            "tiny%22%3A%22tiny%22%7D%7D"
        meta = WallhavenUploaderSearchMeta(
            uploader = WallhavenUploader(
                username = "test",
                group = "group",
                avatar = WallhavenAvatar(
                    large = "large",
                    medium = "medium",
                    small = "small",
                    tiny = "tiny",
                ),
            ),
        )
        assertEquals(
            meta,
            SearchMeta.fromQueryString(metaStr),
        )
    }
}
