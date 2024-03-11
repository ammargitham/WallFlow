package com.ammar.wallflow.extensions

import android.net.Uri
import com.ammar.wallflow.VALID_FILE_NAME_REGEX
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.regex.Pattern
import org.jsoup.parser.Parser

val Any.TAG: String
    get() {
        return if (!javaClass.isAnonymousClass) {
            val name = javaClass.simpleName
            if (name.length <= 23) name else name.substring(0, 23) // first 23 chars
        } else {
            val name = javaClass.name
            if (name.length <= 23) {
                name
            } else {
                name.substring(
                    name.length - 23,
                    name.length,
                ) // last 23 chars
            }
        }
    }

fun String.trimAll() = this.trim { it <= ' ' }

fun String.urlEncoded(): String = URLEncoder.encode(this, "UTF-8")

fun String.urlDecoded(): String = URLDecoder.decode(this, "UTF-8")

fun String.quoteIfSpaced() = if (this.contains(" ")) "\"$this\"" else this

// https://stackoverflow.com/a/11576046/1436766
fun String.getFileNameFromUrl(): String {
    try {
        val resource = URL(this)
        val host = resource.host
        if (host.isNotEmpty() && this.endsWith(host)) {
            // handle ...example.com
            return ""
        }
    } catch (e: MalformedURLException) {
        return ""
    }
    val startIndex = lastIndexOf('/') + 1

    // find end index for ?
    var lastQMPos = lastIndexOf('?')
    if (lastQMPos == -1) {
        lastQMPos = length
    }

    // find end index for #
    var lastHashPos = lastIndexOf('#')
    if (lastHashPos == -1) {
        lastHashPos = length
    }

    // calculate the end index
    val endIndex = lastQMPos.coerceAtMost(lastHashPos)
    return substring(startIndex, endIndex)
}

fun String.capitalise() = this
    .split(" ")
    .joinToString(" ") {
        it.replaceFirstChar { c -> c.uppercaseChar() }
    }

fun String.fromQueryString() = this
    .split("&")
    .map { it.split(Pattern.compile("="), 2) }
    .associate {
        Pair(
            it[0].urlDecoded(),
            if (it.size > 1) it[1].urlDecoded() else null,
        )
    }

fun String.htmlUnescaped(): String = Parser.unescapeEntities(this, false)

fun String.isValidFileName() = VALID_FILE_NAME_REGEX.matches(this)

fun String.toUriOrNull() = try {
    Uri.parse(this)
} catch (e: Exception) {
    null
}
