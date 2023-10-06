package com.ammar.wallflow.model

import kotlin.test.assertEquals
import org.junit.Test

class WallhavenSearchQueryTest {
    @Test
    fun `convert tags, tagId, etc to qString`() {
        var searchQuery = WallhavenSearchQuery(
            includedTags = setOf("i1", "i2"),
            excludedTags = setOf("e1", "e2"),
        )
        assertEquals("+i1 +i2 -e1 -e2", searchQuery.getQString())

        searchQuery = WallhavenSearchQuery(
            includedTags = setOf("i 1", "i 2"),
            excludedTags = setOf("e 1", "e 2"),
        )
        assertEquals("+\"i 1\" +\"i 2\" -\"e 1\" -\"e 2\"", searchQuery.getQString())

        searchQuery = WallhavenSearchQuery(
            includedTags = setOf("i1", "i2"),
            excludedTags = setOf("e1", "e2"),
            username = "test",
            tagId = 12L,
            wallpaperId = "xx1234xx",
        )
        assertEquals("+i1 +i2 -e1 -e2 @test id:12 like:xx1234xx", searchQuery.getQString())
    }
}
