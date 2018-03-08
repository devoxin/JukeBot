package jukebot.commands

import jukebot.CommandHandler
import jukebot.Database
import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Helpers
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

import java.util.Comparator
import java.util.function.Predicate
import java.util.stream.Stream

@CommandProperties(description = "Displays all commands", aliases = arrayOf("commands"), category = CommandProperties.category.MISC)
class Help : Command {

    override fun execute(e: GuildMessageReceivedEvent, query: String) {
        when (query) {
            "1" -> e.channel.sendMessage(createHelpEmbed(
                    "You can use the **play** command to make JukeBot join your channel, search for the specified song and begin playing.\n`"
                            + "${Database.getPrefix(e.guild.idLong)} play <URL/Search Query>`")).queue()

            "2" -> {
                val controls = StringBuilder()

                filterCommands({ command -> command.properties().category == CommandProperties.category.CONTROLS }).forEach { command ->
                    controls.append("**`").append(Helpers.padRight(" ", command.name().toLowerCase(), 11)).append(":`** ")
                            .append(command.properties().description).append("\n")
                }

                e.channel.sendMessage(createHelpEmbed(controls.toString())).queue()
            }

            "3" -> {
                val media = StringBuilder()

                filterCommands({ command -> command.properties().category == CommandProperties.category.MEDIA }).forEach { command ->
                    media.append("**`").append(Helpers.padRight(" ", command.name().toLowerCase(), 11)).append(":`** ")
                            .append(command.properties().description).append("\n")
                }

                e.channel.sendMessage(createHelpEmbed(media.toString())).queue()
            }

            "4" -> {
                val misc = StringBuilder()

                filterCommands({ command -> command.properties().category == CommandProperties.category.MISC }).forEach { command ->
                    misc.append("**`").append(Helpers.padRight(" ", command.name().toLowerCase(), 11)).append(":`** ")
                            .append(command.properties().description).append("\n")
                }

                e.channel.sendMessage(createHelpEmbed(misc.toString())).queue()
            }

            else -> e.channel.sendMessage(EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Help Categories")
                    .setDescription("`1.` Getting Started\n`2.` Controls\n`3.` Media\n`4.` Miscellaneous\n\nUse `${Database.getPrefix(e.guild.idLong)}help <number>` to select a category")
                    .build()
            ).queue()
        }
    }

    private fun filterCommands(filter: (Command) -> Boolean): Stream<Command> {
        return CommandHandler.commands
                .values
                .filter(filter)
                .stream()
    }

    private fun createHelpEmbed(description: String): MessageEmbed {
        return EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setDescription("[View more information here](http://jukebot.xyz/documentation)\n$description")
                .build()
    }

}