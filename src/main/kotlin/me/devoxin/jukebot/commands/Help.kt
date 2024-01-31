package me.devoxin.jukebot.commands

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.SubCommandFunction
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.extensions.embed
import me.devoxin.jukebot.extensions.plural
import me.devoxin.jukebot.extensions.truncate
import me.devoxin.jukebot.utils.Constants

class Help : Cog {
    override fun name() = "Misc"

    @Command(aliases = ["cmds", "commands"], description = "Displays a list of bot commands.")
    fun help(ctx: Context, categoryOrCommand: String? = null, subcommand: String? = null) {
        val commands = ctx.commandClient.commands
        val categories = commands.values.groupBy { it.category }.toSortedMap()
        val longest = categories.keys.maxOf { it.length }

        if (categoryOrCommand != null) {
            if (categoryOrCommand in categories) {
                return sendCategoryHelp(ctx, categoryOrCommand, categories[categoryOrCommand]!!)
            }

            val command = commands[categoryOrCommand.lowercase()]
                ?: return ctx.embed("Help", "`$categoryOrCommand` is not a valid category or command name.")

            if (subcommand != null) {
                if (subcommand in command.subcommands) {
                    return sendSubcommandHelp(ctx, command, command.subcommands[subcommand.lowercase()]!!)
                }

                return ctx.embed("Help", "`$subcommand` is not a valid sub-command name for `${command.name}`")
            } else {
                return sendCommandHelp(ctx, command)
            }
        }

        val page = buildString {
            appendLine("There are multiple categories to choose from.")
            appendLine("Each category contains different commands, categorised by their functions.")
            appendLine("For example, the `Playback` category will hold commands for playing tracks.")
            appendLine()
            appendLine("**__Categories:__**")

            for ((category, cmds) in categories) {
                appendLine("`${category.padEnd(longest)}` — *${cmds.size.plural("command")}*")
            }

            appendLine()
            appendLine("**__For more information:__**")
            appendLine("`/help <category>` for a list of commands by category (case-sensitive).")
            appendLine("`/help <command>` for a command's syntax and sub-commands.")
            appendLine("`/help <command> <subcommand>` for sub-command syntax.")
        }

        ctx.embed("Help", page)
    }

    private fun sendCategoryHelp(ctx: Context, category: String, categoryCommands: List<CommandFunction>) {
        val longest = categoryCommands.maxOf { it.name.length }

        val page = buildString {
            appendLine("**__Commands__**")

            for (command in categoryCommands.sortedBy { it.name }) {
                appendLine("`${command.name.padEnd(longest)}` - ${command.properties.description.truncate(50)}")
            }
        }

        ctx.embed("Help ($category)", page)
    }

    private fun sendCommandHelp(ctx: Context, command: CommandFunction) {
        val page = buildString {
            appendLine(command.properties.description)
            appendLine()
            appendLine("**General Syntax**")
            append("`/${command.name}")

            if (command.subcommands.isNotEmpty()) {
                val longest = command.subcommands.values.maxOf { it.name.length }

                appendLine(" <subcommand>`")
                appendLine()
                appendLine("**Sub-Commands**")

                for ((name, sc) in command.subcommands) {
                    appendLine("`${name.padEnd(longest)}` - ${sc.properties.description.truncate(50)}")
                }
            } else if (command.arguments.isNotEmpty()) {
                for (argument in command.arguments) {
                    append(" ${argument.format(withType = true)}")
                }

                appendLine("`")
                appendLine()
                appendLine("<> indicates a **required** argument")
                appendLine("[] indicates an **optional** argument")
            } else {
                appendLine("`")
            }

            appendLine()
            appendLine("Need further help? [Join the support server](${Constants.HOME_SERVER})")
        }

        ctx.embed("Help (${command.name})", page)
    }

    private fun sendSubcommandHelp(ctx: Context, command: CommandFunction, subcommand: SubCommandFunction) {
        val page = buildString {
            appendLine(subcommand.properties.description)
            appendLine()
            appendLine("**General Syntax**")
            append("`/${command.name} ${subcommand.name}")

            if (subcommand.arguments.isNotEmpty()) {
                for (argument in subcommand.arguments) {
                    append(" ${argument.format(withType = true)}")
                }

                appendLine("`")
                appendLine()
                appendLine("<> indicates a **required** argument")
                appendLine("[] indicates an **optional** argument")
            } else {
                appendLine("`")
            }

            appendLine()
            appendLine("Need further help? [Join the support server](${Constants.HOME_SERVER})")
        }

        ctx.embed("Help (${command.name} ${subcommand.name})", page)
    }
}
