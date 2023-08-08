/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ammar.wallflow.utils

import android.util.Base64
import com.ammar.wallflow.extensions.trimAll
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.BitSet
import java.util.Objects

/**
 * Representation of the Content-Disposition type and parameters as defined in RFC 6266.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sergey Tsypanov
 * @see [RFC 6266](https://tools.ietf.org/html/rfc6266)
 */
class ContentDisposition private constructor(
    /**
     * Return the disposition type.
     *
     * @see .isAttachment
     * @see .isFormData
     * @see .isInline
     */
    val type: String?,
    /**
     * Return the value of the name parameter, or `null` if not defined.
     */
    val name: String?,
    /**
     * Return the value of the filename parameter, possibly decoded
     * from BASE64 encoding based on RFC 2047, or of the filename*
     * parameter, possibly decoded as defined in the RFC 5987.
     */
    val filename: String?,
    /**
     * Return the charset defined in filename* parameter, or `null` if not defined.
     */
    val charset: Charset?,
) {

    // /**
    //  * Return whether the [type][.getType] is &quot;attachment&quot;.
    //  *
    //  * @since 5.3
    //  */
    // val isAttachment: Boolean
    //     get() = type != null && type.equals("attachment", ignoreCase = true)
    //
    // /**
    //  * Return whether the [type][.getType] is &quot;form-data&quot;.
    //  *
    //  * @since 5.3
    //  */
    // val isFormData: Boolean
    //     get() = type != null && type.equals("form-data", ignoreCase = true)
    //
    // /**
    //  * Return whether the [type][.getType] is &quot;inline&quot;.
    //  *
    //  * @since 5.3
    //  */
    // val isInline: Boolean
    //     get() = type != null && type.equals("inline", ignoreCase = true)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ContentDisposition
        return type == that.type && name == that.name && filename == that.filename && charset == that.charset
    }

    override fun hashCode(): Int {
        return Objects.hash(type, name, filename, charset)
    }

    /**
     * Return the header value for this content disposition as defined in RFC 6266.
     *
     * @see .parse
     */
    override fun toString(): String {
        val sb = StringBuilder()
        if (type != null) {
            sb.append(type)
        }
        if (name != null) {
            sb.append("; name=\"")
            sb.append(name).append('\"')
        }
        if (filename != null) {
            if (charset == null || StandardCharsets.US_ASCII == charset) {
                sb.append("; filename=\"")
                sb.append(encodeQuotedPairs(filename)).append('\"')
            } else {
                sb.append("; filename=\"")
                sb.append(encodeQuotedPrintableFilename(filename, charset)).append('\"')
                sb.append("; filename*=")
                sb.append(encodeRfc5987Filename(filename, charset))
            }
        }
        return sb.toString()
    }

    // /**
    //  * A mutable builder for `ContentDisposition`.
    //  */
    // interface Builder {
    //     /**
    //      * Set the value of the name parameter.
    //      */
    //     fun name(name: String?): Builder
    //
    //     /**
    //      * Set the value of the filename parameter. The given
    //      * filename will be formatted as quoted-string, as defined in RFC 2616,
    //      * section 2.2, and any quote characters within the filename value will
    //      * be escaped with a backslash, e.g. `"foo\"bar.txt"` becomes
    //      * `"foo\\\"bar.txt"`.
    //      */
    //     fun filename(filename: String): Builder
    //
    //     /**
    //      * Set the value of the `filename` that will be encoded as
    //      * defined in RFC 5987. Only the US-ASCII, UTF-8, and ISO-8859-1
    //      * charsets are supported.
    //      *
    //      * **Note:** Do not use this for a
    //      * `"multipart/form-data"` request since
    //      * [RFC 7578, Section 4.2](https://tools.ietf.org/html/rfc7578#section-4.2)
    //      * and also RFC 5987 mention it does not apply to multipart requests.
    //      */
    //     fun filename(filename: String, charset: Charset?): Builder
    //
    //     /**
    //      * Build the content disposition.
    //      */
    //     fun build(): ContentDisposition
    // }

    // private class BuilderImpl(type: String) : Builder {
    //     private val type: String
    //     private var name: String? = null
    //     private var filename: String? = null
    //     private var charset: Charset? = null
    //
    //     init {
    //         require(type.isNotEmpty()) { "'type' must not be not empty" }
    //         this.type = type
    //     }
    //
    //     override fun name(name: String?): Builder {
    //         this.name = name
    //         return this
    //     }
    //
    //     override fun filename(filename: String) = filename(filename, null)
    //
    //     override fun filename(filename: String, charset: Charset?): Builder {
    //         require(filename.isNotEmpty()) { "No filename" }
    //         this.filename = filename
    //         this.charset = charset
    //         return this
    //     }
    //
    //     override fun build(): ContentDisposition {
    //         return ContentDisposition(
    //             type,
    //             name,
    //             filename,
    //             charset
    //         )
    //     }
    // }

    companion object {
        private val BASE64_ENCODED_PATTERN =
            "=\\?([0-9a-zA-Z-_]+)\\?B\\?([+/0-9a-zA-Z]+=*)\\?=".toPattern()
        private val QUOTED_PRINTABLE_ENCODED_PATTERN =
            "=\\?([0-9a-zA-Z-_]+)\\?Q\\?([!->@-~]+)\\?=".toPattern() // Printable ASCII other than "?" or SPACE
        private const val INVALID_HEADER_FIELD_PARAMETER_FORMAT =
            "Invalid header field parameter format (as defined in RFC 5987)"
        private val PRINTABLE = BitSet(256)

        // /**
        //  * The default buffer size used when copying bytes.
        //  */
        // const val BUFFER_SIZE = 8192

        init {
            // RFC 2045, Section 6.7, and RFC 2047, Section 4.2
            for (i in 33..126) {
                PRINTABLE.set(i)
            }
            PRINTABLE[61] = false // =
            PRINTABLE[63] = false // ?
            PRINTABLE[95] = false // _
        }

        // /**
        //  * Return a builder for a `ContentDisposition` of type &quot;attachment&quot;.
        //  *
        //  * @since 5.3
        //  */
        // fun attachment(): Builder {
        //     return builder("attachment")
        // }
        //
        // /**
        //  * Return a builder for a `ContentDisposition` of type &quot;form-data&quot;.
        //  *
        //  * @since 5.3
        //  */
        // fun formData(): Builder {
        //     return builder("form-data")
        // }
        //
        // /**
        //  * Return a builder for a `ContentDisposition` of type &quot;inline&quot;.
        //  *
        //  * @since 5.3
        //  */
        // fun inline(): Builder {
        //     return builder("inline")
        // }
        //
        // /**
        //  * Return a builder for a `ContentDisposition`.
        //  *
        //  * @param type the disposition type like for example inline,
        //  * attachment, or form-data
        //  * @return the builder
        //  */
        // private fun builder(type: String): Builder {
        //     return BuilderImpl(type)
        // }
        //
        // /**
        //  * Return an empty content disposition.
        //  */
        // fun empty(): ContentDisposition {
        //     return ContentDisposition("", null, null, null)
        // }

        /**
         * Parse a Content-Disposition header value as defined in RFC 2183.
         *
         * @param contentDisposition the Content-Disposition header value
         * @return the parsed content disposition
         * @see .toString
         */
        fun parse(contentDisposition: String): ContentDisposition {
            val parts = tokenize(contentDisposition)
            val type = parts[0]
            var name: String? = null
            var filename: String? = null
            var charset: Charset? = null
            for (i in 1 until parts.size) {
                val part = parts[i]
                val eqIndex = part.indexOf('=')
                if (eqIndex == -1) {
                    throw IllegalArgumentException("Invalid content disposition format")
                }
                val attribute = part.substring(0, eqIndex)
                val isQuoted = part.startsWith("\"", eqIndex + 1) && part.endsWith("\"")
                val value = when {
                    isQuoted -> part.substring(eqIndex + 2, part.length - 1)
                    else -> part.substring(eqIndex + 1)
                }
                when {
                    attribute == "name" -> name = value
                    attribute == "filename*" -> {
                        val pair = decodeFileNameStar(value)
                        charset = pair.first
                        filename = pair.second
                    }
                    attribute == "filename" && filename == null -> {
                        val pair = decodeFileName(value, charset)
                        charset = pair.first
                        filename = pair.second
                    }
                }
            }
            return ContentDisposition(type, name, filename, charset)
        }

        private fun decodeFileNameStar(value: String): Pair<Charset?, String?> {
            var charset: Charset? = null
            val idx1 = value.indexOf('\'')
            val idx2 = value.indexOf('\'', idx1 + 1)
            val filename = when {
                idx1 != -1 && idx2 != -1 -> {
                    charset = Charset.forName(value.substring(0, idx1).trimAll())
                    require(StandardCharsets.UTF_8 == charset || StandardCharsets.ISO_8859_1 == charset) {
                        "Charset must be UTF-8 or ISO-8859-1"
                    }
                    decodeRfc5987Filename(value.substring(idx2 + 1), charset)
                }
                else -> decodeRfc5987Filename(value, StandardCharsets.US_ASCII)
            }
            return Pair(charset, filename)
        }

        private fun decodeFileName(
            value: String,
            charset: Charset?,
        ): Pair<Charset?, String?> {
            var charset1 = charset
            val filename = when {
                value.startsWith("=?") -> {
                    var matcher = BASE64_ENCODED_PATTERN.matcher(value)
                    if (matcher.find()) {
                        val builder = StringBuilder()
                        do {
                            charset1 = Charset.forName(matcher.group(1))
                            val fName = matcher.group(2) ?: ""
                            val decoded = Base64.decode(fName, Base64.DEFAULT)
                            builder.append(String(decoded, charset1))
                        } while (matcher.find())
                        builder.toString()
                    } else {
                        matcher = QUOTED_PRINTABLE_ENCODED_PATTERN.matcher(value)
                        if (matcher.find()) {
                            val builder = StringBuilder()
                            do {
                                charset1 = Charset.forName(matcher.group(1))
                                val fName = matcher.group(2) ?: ""
                                val decoded = decodeQuotedPrintableFilename(fName, charset1)
                                builder.append(decoded)
                            } while (matcher.find())
                            builder.toString()
                        } else {
                            value
                        }
                    }
                }
                value.indexOf('\\') != -1 -> decodeQuotedPairs(value)
                else -> value
            }
            return Pair(charset1, filename)
        }

        private fun tokenize(headerValue: String): List<String> {
            var index = headerValue.indexOf(';')
            val type = (
                if (index >= 0) {
                    headerValue.substring(
                        0,
                        index,
                    )
                } else {
                    headerValue
                }
                ).trim { it <= ' ' }
            require(type.isNotEmpty()) { "Content-Disposition header must not be empty" }
            val parts: MutableList<String> = ArrayList()
            parts.add(type)
            if (index >= 0) {
                do {
                    var nextIndex = index + 1
                    var quoted = false
                    var escaped = false
                    while (nextIndex < headerValue.length) {
                        val ch = headerValue[nextIndex]
                        if (ch == ';') {
                            if (!quoted) {
                                break
                            }
                        } else if (!escaped && ch == '"') {
                            quoted = !quoted
                        }
                        escaped = !escaped && ch == '\\'
                        nextIndex++
                    }
                    val part = headerValue.substring(index + 1, nextIndex).trim { it <= ' ' }
                    if (part.isNotEmpty()) {
                        parts.add(part)
                    }
                    index = nextIndex
                } while (index < headerValue.length)
            }
            return parts
        }

        /**
         * Decode the given header field param as described in RFC 5987.
         *
         * Only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported.
         *
         * @param filename the filename
         * @param charset  the charset for the filename
         * @return the encoded header field param
         * @see [RFC 5987](https://tools.ietf.org/html/rfc5987)
         */
        private fun decodeRfc5987Filename(filename: String, charset: Charset) =
            ByteArrayOutputStream().use {
                val value = filename.toByteArray(charset)
                var index = 0
                while (index < value.size) {
                    val b = value[index]
                    if (isRFC5987AttrChar(b)) {
                        it.write(Char(b.toUShort()).code)
                        index++
                    } else if (b == '%'.code.toByte() && index < value.size - 2) {
                        val array = charArrayOf(
                            Char(value[index + 1].toUShort()),
                            Char(value[index + 2].toUShort()),
                        )
                        try {
                            it.write(String(array).toInt(16))
                        } catch (ex: NumberFormatException) {
                            throw IllegalArgumentException(
                                INVALID_HEADER_FIELD_PARAMETER_FORMAT,
                                ex,
                            )
                        }
                        index += 3
                    } else {
                        throw IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT)
                    }
                }
                copyToString(it, charset)
            }

        private fun isRFC5987AttrChar(c: Byte) = c >= '0'.code.toByte() &&
            c <= '9'.code.toByte() ||
            c >= 'a'.code.toByte() &&
            c <= 'z'.code.toByte() ||
            c >= 'A'.code.toByte() &&
            c <= 'Z'.code.toByte() ||
            c == '!'.code.toByte() ||
            c == '#'.code.toByte() ||
            c == '$'.code.toByte() ||
            c == '&'.code.toByte() ||
            c == '+'.code.toByte() ||
            c == '-'.code.toByte() ||
            c == '.'.code.toByte() ||
            c == '^'.code.toByte() ||
            c == '_'.code.toByte() ||
            c == '`'.code.toByte() ||
            c == '|'.code.toByte() ||
            c == '~'.code.toByte()

        /**
         * Decode the given header field param as described in RFC 2047.
         *
         * @param filename the filename
         * @param charset  the charset for the filename
         * @return the decoded header field param
         * @see [RFC 2047](https://tools.ietf.org/html/rfc2047)
         */
        private fun decodeQuotedPrintableFilename(filename: String, charset: Charset?) =
            ByteArrayOutputStream().use {
                val value = filename.toByteArray(StandardCharsets.US_ASCII)
                var index = 0
                while (index < value.size) {
                    val b = value[index]
                    if (b == '_'.code.toByte()) { // RFC 2047, section 4.2, rule (2)
                        it.write(' '.code)
                        index++
                    } else if (b == '='.code.toByte() && index < value.size - 2) {
                        val i1 = Char(value[index + 1].toUShort()).digitToIntOrNull(16) ?: -1
                        val i2 = Char(value[index + 2].toUShort()).digitToIntOrNull(16) ?: -1
                        require(i1 != -1 && i2 != -1) {
                            "Not a valid hex sequence: " + filename.substring(index)
                        }
                        it.write(i1 shl 4 or i2)
                        index += 3
                    } else {
                        it.write(b.toInt())
                        index++
                    }
                }
                copyToString(it, charset)
            }

        /**
         * Encode the given header field param as described in RFC 2047.
         *
         * @param filename the filename
         * @param charset  the charset for the filename
         * @return the encoded header field param
         * @see [RFC 2047](https://tools.ietf.org/html/rfc2047)
         */
        internal fun encodeQuotedPrintableFilename(filename: String, charset: Charset): String {
            val source = filename.toByteArray(charset)
            val sb = StringBuilder(source.size shl 1)
            sb.append("=?")
            sb.append(charset.name())
            sb.append("?Q?")
            for (b in source) {
                when {
                    b.toInt() == 32 -> sb.append('_') // RFC 2047, section 4.2, rule (2)
                    isPrintable(b) -> sb.append(Char(b.toUShort()))
                    else -> {
                        sb.append('=')
                        sb.append(hexDigit(b.toInt() shr 4))
                        sb.append(hexDigit(b.toInt()))
                    }
                }
            }
            sb.append("?=")
            return sb.toString()
        }

        private fun isPrintable(c: Byte): Boolean {
            var b = c.toInt()
            if (b < 0) {
                b += 256
            }
            return PRINTABLE[b]
        }

        internal fun encodeQuotedPairs(filename: String): String {
            if (filename.indexOf('"') == -1 && filename.indexOf('\\') == -1) {
                return filename
            }
            val sb = StringBuilder()
            for (element in filename) {
                if (element == '"' || element == '\\') {
                    sb.append('\\')
                }
                sb.append(element)
            }
            return sb.toString()
        }

        private fun decodeQuotedPairs(filename: String): String {
            val sb = StringBuilder()
            val length = filename.length
            var i = 0
            while (i < length) {
                val c = filename[i]
                if (filename[i] == '\\' && i + 1 < length) {
                    i++
                    val next = filename[i]
                    if (next != '"' && next != '\\') {
                        sb.append(c)
                    }
                    sb.append(next)
                } else {
                    sb.append(c)
                }
                i++
            }
            return sb.toString()
        }

        /**
         * Encode the given header field param as describe in RFC 5987.
         *
         * @param input   the header field param
         * @param charset the charset of the header field param string,
         * only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported
         * @return the encoded header field param
         * @see [RFC 5987](https://tools.ietf.org/html/rfc5987)
         */
        internal fun encodeRfc5987Filename(input: String, charset: Charset): String {
            require(StandardCharsets.US_ASCII != charset) {
                "ASCII does not require encoding"
            }
            require(StandardCharsets.UTF_8 == charset || StandardCharsets.ISO_8859_1 == charset) {
                "Only UTF-8 and ISO-8859-1 are supported"
            }
            val source = input.toByteArray(charset)
            val sb = StringBuilder(source.size shl 1)
            sb.append(charset.name())
            sb.append("''")
            for (b in source) {
                if (isRFC5987AttrChar(b)) {
                    sb.append(Char(b.toUShort()))
                } else {
                    sb.append('%')
                    val hex1 = hexDigit(b.toInt() shr 4)
                    val hex2 = hexDigit(b.toInt())
                    sb.append(hex1)
                    sb.append(hex2)
                }
            }
            return sb.toString()
        }

        private fun hexDigit(b: Int): Char {
            return Character.forDigit(b and 0xF, 16).uppercaseChar()
        }

        /**
         * Copy the contents of the given [ByteArrayOutputStream] into a [String].
         *
         * This is a more effective equivalent of `new String(baos.toByteArray(), charset)`.
         * @param baos the `ByteArrayOutputStream` to be copied into a String
         * @param charset the [Charset] to use to decode the bytes
         * @return the String that has been copied to (possibly empty)
         */
        private fun copyToString(baos: ByteArrayOutputStream, charset: Charset?) = when {
            charset != null -> baos.toString(charset.name())
            else -> baos.toString()
        }
    }
}
