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
    val embedColour = decodeColor(getString("color", "")) ?: Color.decode("#1E90FF")
    val nsfwEnabled = getBoolean("nsfw")

    fun hasKey(key: String): Boolean {
        val value = getString(key)
        return value != null && value.isNotEmpty()
    }

    fun getString(key: String): String? {
        return _conf.getProperty(key, null)
    }

    fun getString(key: String, default: String): String {
        return _conf.getProperty(key, default)
    }

    fun getBoolean(key: String): Boolean {
        return getString(key)?.toBoolean() ?: false
    }

}
