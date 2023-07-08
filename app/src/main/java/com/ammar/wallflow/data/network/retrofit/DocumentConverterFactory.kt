package com.ammar.wallflow.data.network.retrofit

import java.io.IOException
import java.lang.reflect.Type
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Converter
import retrofit2.Retrofit


class DocumentConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *>? {
        if (type == Document::class.java) {
            return DocumentBodyConverter.INSTANCE
        }
        return null
    }

    internal class DocumentBodyConverter : Converter<ResponseBody, Document> {
        @Throws(IOException::class)
        override fun convert(value: ResponseBody): Document {
            return Jsoup.parse(value.string())
        }

        companion object {
            val INSTANCE = DocumentBodyConverter()
        }
    }
}
