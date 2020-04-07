package jukebot.utils

import java.awt.Color
import java.io.FileReader
import java.util.*

class Config(file: String) {
    private val _conf = Properties()

    init {
        FileReader(file).use {
            _conf.load(it)
        }
    }

    val token = getString("token", "")
    val defaultPrefix = getString("prefix", "$")
    val embedColour: Color = decodeColor(getString("color", "")) ?: Color.decode("#1E90FF")
    val nsfwEnabled = getBoolean("nsfw")
    val ipv6block = getString("ipv6", "")

    fun hasKey(key: String) = getString(key)?.isNotEmpty() ?: false

    fun getString(key: String): String? = _conf.getProperty(key, null)

    fun getString(key: String, default: String): String = _conf.getProperty(key, default)

    fun getBoolean(key: String): Boolean = getString(key)?.toBoolean() ?: false
}
