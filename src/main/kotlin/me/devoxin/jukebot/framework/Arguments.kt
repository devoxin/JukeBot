package me.devoxin.jukebot.framework

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping

interface Arguments {
    val isEmpty: Boolean

    fun hasNext(name: String): Boolean

    fun <T> next(name: String, resolver: ArgumentResolver<T>): T?

    fun gatherNext(name: String): String

    class SlashArguments(private val event: SlashCommandInteractionEvent) : Arguments {
        override val isEmpty: Boolean
            get() = event.options.isEmpty()

        override fun hasNext(name: String) = event.getOption(name) != null

        override fun <T> next(name: String, resolver: ArgumentResolver<T>): T? = try {
            event.getOption(name, null, resolver.optionResolver)
        } catch (t: Throwable) {
            null
        }

        override fun gatherNext(name: String): String {
            return event.getOption(name, OptionMapping::getAsString)
                ?: ""
        }
    }

    class MessageArguments(private val message: Message, private val args: List<String>) : Arguments {
        private var index = 0

        override val isEmpty: Boolean
            get() = args.isEmpty()

        override fun hasNext(name: String) = index >= 0 && index < args.size

        override fun <T> next(name: String, resolver: ArgumentResolver<T>): T? {
            if (index >= args.size) {
                return null
            }

            return try {
                resolver.parser(message, args[index]).also { index++ }
            } catch (t: Throwable) {
                null
            }
        }

        override fun gatherNext(name: String) = args.drop(index).joinToString(" ")
    }
}
