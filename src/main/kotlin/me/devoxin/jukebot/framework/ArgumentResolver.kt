package me.devoxin.jukebot.framework

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.commands.OptionMapping

class ArgumentResolver<T>(val optionResolver: (OptionMapping) -> T?, val parser: (Message, String) -> T?) {
    companion object {
        val STRING = ArgumentResolver(OptionMapping::getAsString) { _, s -> s }
        val BOOLEAN = ArgumentResolver(OptionMapping::getAsBoolean) { _, s ->
            when (s) {
                "1", "on", "yes", "y", "true" -> true
                "0", "off", "no", "n"," false" -> false
                else -> null
            }
        }

        val USER = ArgumentResolver(OptionMapping::getAsUser) { m, s ->
            s.dropWhile { !it.isDigit() }.takeWhile { it.isDigit() }.toLongOrNull()
                ?.let { m.mentions.users.firstOrNull { u -> u.idLong == it } }
        }

        val INTEGER = ArgumentResolver(OptionMapping::getAsInt) { _, s -> s.toIntOrNull() }
        val LONG = ArgumentResolver(OptionMapping::getAsLong) { _, s -> s.toLongOrNull() }
        val DOUBLE = ArgumentResolver(OptionMapping::getAsDouble) { _, s -> s.toDoubleOrNull() }
        val FLOAT = ArgumentResolver({ it.asDouble.toFloat() }) { _, s -> s.toFloatOrNull() }
    }
}