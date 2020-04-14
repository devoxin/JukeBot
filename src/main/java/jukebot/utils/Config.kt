package jukebot.utils

import java.awt.Color
import java.io.FileReader
import java.util.*

class Config(filePath: String) {
    private val conf = FileReader(filePath).use { fr -> Properties().apply { load(fr) } }

    operator fun contains(key: String) = conf.containsKey(key) && conf.getProperty(key).isNotEmpty()
    operator fun get(key: String, default: String? = null): String = conf.getProperty(key)
        ?: default
        ?: throw IllegalArgumentException("$key is not present in config!")

    fun opt(key: String, default: String? = null): String? = conf.getProperty(key, default)

    val token = get("token")
    val defaultPrefix = get("prefix", "$")
    val embedColour = opt("color")?.toColorOrNull() ?: Color.decode("#1E90FF")
    val nsfwEnabled = opt("nsfw")?.toBoolean() ?: false
    val ipv6block = opt("ipv6")


    companion object {
        fun load(): Config {
            val configPath = System.getenv("jukebot.config")
                ?: "config.properties"

            return Config(configPath)
            // TODO: Perhaps allow loading config from an env, and additionally, a program flag (--config).
            // Also consider switching to a more flexible config like hocon.
        }
    }
}
