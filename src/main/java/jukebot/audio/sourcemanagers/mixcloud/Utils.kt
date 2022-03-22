package jukebot.audio.sourcemanagers.mixcloud

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*

object Utils {
    fun cycle(i: String): Sequence<Char> = sequence {
        var index = -1
        while (true) {
            yield(i[++index % i.length])
        }
    }

    fun decryptXor(key: String, cipher: String): String {
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

    internal fun String.urlEncoded() = URLEncoder.encode(this, Charset.defaultCharset())
    internal fun String.urlDecoded() = URLDecoder.decode(this, Charset.defaultCharset())
}
