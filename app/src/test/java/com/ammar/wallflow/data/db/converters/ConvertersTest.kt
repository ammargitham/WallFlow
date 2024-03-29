package com.ammar.wallflow.data.db.converters

import kotlin.test.assertEquals
import org.junit.Test

class ConvertersTest {
    @Test
    fun `convert list of strings to json string`() {
        val json = Converters.stringListToJson(
            listOf(
                "test1",
                "test2",
                "test3",
            ),
        )
        assertEquals("[\"test1\",\"test2\",\"test3\"]", json)
    }

    @Test
    fun `convert json string to list of strings`() {
        val list = Converters.fromJsonToStringList("[\"test1\",\"test2\",\"test3\"]")
        assertEquals(
            listOf(
                "test1",
                "test2",
                "test3",
            ),
            list,
        )
    }
}
