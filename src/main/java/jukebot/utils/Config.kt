package jukebot.utils

import java.io.FileReader
import java.util.*

class Config(private val file: String) {

    private val _conf: Properties = Properties()

    init {
        FileReader(file).use {
            _conf.load(it)
        }
    }

    public fun hasKey(key: String): Boolean {
        val value = getString(key)
        return value != null && value.isNotEmpty()
    }

    public fun getString(key: String): String? {
        return _conf.getProperty(key, null)
    }

    public fun getString(key: String, default: String): String {
        return _conf.getProperty(key, default)
    }

    public fun getBoolean(key: String): Boolean {
        return getString(key)?.toBoolean() ?: false
    }

}
