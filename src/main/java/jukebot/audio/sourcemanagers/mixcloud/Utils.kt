package jukebot.audio.sourcemanagers.mixcloud

object Utils {
    fun cycle(i: String): Sequence<Char> = sequence {
        var index = -1
        while (true) {
            ++index
            yield(i[index % i.length])
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
}
