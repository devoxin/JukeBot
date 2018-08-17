package jukebot.commands

import jukebot.CommandHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Helpers

@CommandProperties(description = "Displays all commands", aliases = ["commands"], category = CommandProperties.category.MISC)
class Help : Command {

    override fun execute(context: Context) {

        val args: Array<String> = context.args

        if (args[0] == "1") {
            return context.sendEmbed("Getting Started",
                    "You can use the **play** command to make JukeBot join your channel, search for the specified song and begin playing.\n`"
                            + "${context.prefix} play <URL/Search Query>`")
        }

        val menu = Helpers.parseNumber(args[0], 0)

        if (menu <= 0 || (menu - 1) > CommandProperties.category.values().size) {
            val cmd: Command = CommandHandler.commands[args[0]]
                    ?: return context.sendEmbed("Help Categories",
                            "`1.` Getting Started\n`2.` Controls\n`3.` Media\n`4.` Miscellaneous\n\nUse `${context.prefix}help <number>` to select a category")

            val aliases: String = cmd.properties().aliases.joinToString(", ")
            context.sendEmbed("Help for **${cmd.name()}**",
                    "**Aliases:** ${if (aliases.isEmpty()) "None" else aliases}\n**Description:** ${cmd.properties().description}")
        } else {
            val category = CommandProperties.category.values()[menu - 2]
            val builder = StringBuilder()

            filterCommands { command -> command.properties().category == category }.forEach { command ->
                builder.append("**`").append(String.format("%-11s", command.name().toLowerCase()).replace(" ", " \u200B")).append(":`** ")
                        .append(command.properties().description).append("\n")
            }

            context.sendEmbed("Help for **$category**",
                    "[View more information here](https://jukebot.xyz/documentation)\n$builder",
                    "You can use ${context.prefix}help <command> to view additional command information")
        }

    }

    private fun filterCommands(filter: (Command) -> Boolean): List<Command> {
        return CommandHandler.commands
                .values
                .filter(filter)
                .sortedBy { it.name() }
    }

}
