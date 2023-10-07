package com.ammar.wallflow.data.repository.utils

import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenTag
import com.ammar.wallflow.data.repository.utils.WallhavenTagsDocumentParser.parsePopularTags
import kotlin.test.assertContentEquals
import kotlinx.datetime.Instant
import org.jsoup.Jsoup
import org.junit.Test

class WallhavenTagsDocumentParserTest {
    @Test
    fun `parse popular tags from document`() {
        val docInputStream = this.javaClass.classLoader?.getResourceAsStream("popular_tags.html")
            ?: throw RuntimeException("Missing html file!")
        val doc = Jsoup.parse(docInputStream, Charsets.UTF_8.displayName(), "https://wallhaven.cc/")
        val tags = parsePopularTags(doc)
        assertContentEquals(
            tags.subList(0, 3),
            listOf(
                NetworkWallhavenTag(
                    id = 37,
                    name = "nature",
                    alias = "",
                    category_id = 5,
                    category = "Nature",
                    purity = "sfw",
                    created_at = Instant.parse("2014-02-02T19:24:56+00:00"),
                ),
                NetworkWallhavenTag(
                    id = 711,
                    name = "landscape",
                    alias = "",
                    category_id = 41,
                    category = "Landscapes",
                    purity = "sfw",
                    created_at = Instant.parse("2014-03-04T14:51:14+00:00"),
                ),
                NetworkWallhavenTag(
                    id = 65348,
                    name = "4K",
                    alias = "",
                    category_id = 2,
                    category = "Art & Design",
                    purity = "sfw",
                    created_at = Instant.parse("2017-08-23T21:06:57+00:00"),
                ),
            ),
        )
    }
}
