package com.ammar.wallflow

import com.ammar.wallflow.model.search.Filters
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

// set default deserializers
private val module = SerializersModule {
    polymorphic(Search::class) {
        defaultDeserializer {
            WallhavenSearch.serializer()
        }
    }
    polymorphic(Filters::class) {
        defaultDeserializer {
            WallhavenFilters.serializer()
        }
    }
}

val json = Json {
    serializersModule = module
}

val safeJson = Json {
    serializersModule = module
    coerceInputValues = true
}
