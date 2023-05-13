package com.ammar.havenwalls.data.repository.utils

import com.ammar.havenwalls.data.network.model.NetworkTag
import com.ammar.havenwalls.data.repository.utils.TagsDocumentParser.parsePopularTags
import kotlinx.datetime.Instant
import org.jsoup.Jsoup
import org.junit.Test
import kotlin.test.assertContentEquals

class TagsDocumentParserTest {
    @Test
    fun `parse popular tags from document`() {
        val docInputStream = this.javaClass.classLoader?.getResourceAsStream("popular_tags.html")
            ?: throw RuntimeException("Missing html file!")
        val doc = Jsoup.parse(docInputStream, Charsets.UTF_8.displayName(), "https://wallhaven.cc/")
        val tags = parsePopularTags(doc)
        assertContentEquals(
            tags.subList(0, 3),
            listOf(
                NetworkTag(
                    id = 37,
                    name = "nature",
                    alias = "",
                    category_id = 5,
                    category = "Nature",
                    purity = "sfw",
                    created_at = Instant.parse("2014-02-02T19:24:56+00:00"),
                ),
                NetworkTag(
                    id = 711,
                    name = "landscape",
                    alias = "",
                    category_id = 41,
                    category = "Landscapes",
                    purity = "sfw",
                    created_at = Instant.parse("2014-03-04T14:51:14+00:00"),
                ),
                NetworkTag(
                    id = 65348,
                    name = "4K",
                    alias = "",
                    category_id = 2,
                    category = "Art & Design",
                    purity = "sfw",
                    created_at = Instant.parse("2017-08-23T21:06:57+00:00"),
                )
            )
        )
    }
}
