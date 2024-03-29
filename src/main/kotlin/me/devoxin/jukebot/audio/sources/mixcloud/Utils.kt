package me.devoxin.jukebot.audio.sources.mixcloud

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*

object Utils {
    private fun cycle(i: String): Sequence<Char> = sequence {
        var index = -1
        while (true) {
            yield(i[++index % i.length])
        }
    }

    private fun decryptXor(key: String, cipher: String): String {
        return cipher.asIterable()
            .zip(cycle(key).asIterable())
            .map { (ch, k) ->
                (ch.toString().codePointAt(0) xor k.toString().codePointAt(0)).toChar()
            }
            .joinToString("")
    }

    fun decryptUrl(key: String, url: String): String {
        val xorUrl = String(Base64.getDecoder().decode(url))
        return decryptXor(key, xorUrl)
    }

    internal fun String.urlEncoded() = URLEncoder.encode(this, Charsets.UTF_8)
    internal fun String.urlDecoded() = URLDecoder.decode(this, Charsets.UTF_8)
}
