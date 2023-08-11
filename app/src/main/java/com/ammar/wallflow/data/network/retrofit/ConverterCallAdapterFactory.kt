package com.ammar.wallflow.data.network.retrofit

import java.io.IOException
import java.lang.reflect.Type
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

open class ConverterCallAdapterFactory(
    private val converterFactory: ResponseBodyConverter.Factory,
) : CallAdapter.Factory() {
    interface ResponseBodyConverter<T> {
        @Throws(IOException::class)
        fun convert(request: Request?, body: ResponseBody?): T
        interface Factory {
            fun responseBodyConverter(
                type: Type?,
                annotations: Array<Annotation>?,
                retrofit: Retrofit?,
            ): ResponseBodyConverter<*>?
        }
    }

    override fun get(
        returnType: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *> {
        @Suppress("UNCHECKED_CAST")
        val delegate = retrofit.nextCallAdapter(
            this,
            returnType,
            annotations,
        ) as CallAdapter<Any, *>

        @Suppress("UNCHECKED_CAST")
        val converter = converterFactory.responseBodyConverter(
            delegate.responseType(),
            annotations,
            retrofit,
        ) as ResponseBodyConverter<Any>
        return object : CallAdapter<ResponseBody?, Any> {
            override fun responseType(): Type {
                return ResponseBody::class.java
            }

            override fun adapt(call: Call<ResponseBody?>): Any {
                return delegate.adapt(ConverterCall(call, converter))
            }
        }
    }

    private class ConverterCall<T>(
        private val delegate: Call<ResponseBody?>,
        val converter: ResponseBodyConverter<T>,
    ) :
        Call<T> {
        @Throws(IOException::class)
        override fun execute(): Response<T> {
            val response = delegate.execute()
            val raw = response.raw()
            if (raw.isSuccessful) {
                return Response.success(converter.convert(delegate.request(), response.body()), raw)
            }
            val body = raw.body ?: throw IOException("Body is null")
            return Response.error(body, raw)
        }

        override fun enqueue(callback: Callback<T>) {
            delegate.enqueue(
                object : Callback<ResponseBody?> {
                    override fun onResponse(
                        call: Call<ResponseBody?>,
                        response: Response<ResponseBody?>,
                    ) {
                        val raw = response.raw()

                        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                        val converted: Response<T> = if (raw.isSuccessful) {
                            try {
                                Response.success(
                                    converter.convert(call.request(), response.body()),
                                    raw,
                                )
                            } catch (e: IOException) {
                                callback.onFailure(this@ConverterCall, e)
                                return
                            }
                        } else {
                            Response.error(raw.body, raw)
                        }
                        callback.onResponse(this@ConverterCall, converted)
                    }

                    override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                        callback.onFailure(this@ConverterCall, t)
                    }
                },
            )
        }

        override fun isExecuted(): Boolean {
            return delegate.isExecuted
        }

        override fun cancel() {
            delegate.cancel()
        }

        override fun isCanceled(): Boolean {
            return delegate.isCanceled
        }

        override fun clone(): Call<T> {
            return ConverterCall(delegate.clone(), converter)
        }

        override fun request(): Request {
            return delegate.request()
        }

        override fun timeout(): Timeout {
            return delegate.timeout()
        }
    }
}
