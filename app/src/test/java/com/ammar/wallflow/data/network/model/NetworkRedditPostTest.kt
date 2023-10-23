package com.ammar.wallflow.data.network.model

import com.ammar.wallflow.extensions.htmlUnescaped
import kotlin.test.assertEquals
import org.junit.Test

class NetworkRedditPostTest {
    @Test
    fun `should correctly parse reddit preview url`() {
        val url = "https://preview.redd.it/zyp7kzw3hcub1.jpg?width=1080&amp;crop=smart&amp;" +
            "auto=webp&amp;s=f14b6d74b872e9b9b0606d2cca82eaafd7d18fd8"
        assertEquals(
            url.htmlUnescaped(),
            "https://preview.redd.it/zyp7kzw3hcub1.jpg?width=1080&crop=smart&auto=webp" +
                "&s=f14b6d74b872e9b9b0606d2cca82eaafd7d18fd8",
        )
    }
}
