package me.devoxin.jukebot.commands.misc

import me.devoxin.jukebot.framework.*
import me.devoxin.jukebot.handlers.CommandHandler
import me.devoxin.jukebot.utils.Constants
import me.devoxin.jukebot.utils.Helpers

@CommandProperties(description = "Displays all commands", aliases = ["commands", "cmds", "?"])
class Help : Command(ExecutionType.STANDARD) {
    private val categories = CommandCategory.values()
        .mapIndexed { i, e -> "`${i + 1}.` **`${Helpers.pad(e.toTitleCase())}:`** ${e.description}" }
        .joinToString("\n")

    override fun execute(context: Context) {
        val menu = context.args.next("specific", ArgumentResolver.STRING)
            ?: return sendDefaultHelp(context)

        val categoryIndex = menu.toIntOrNull() ?: 0

        if (categoryIndex <= 0 || categoryIndex > CommandCategory.values().size) {
            val cmd = CommandHandler.commands
                .filter { it.key == menu || it.value.properties.aliases.contains(menu) }
                .values
                .firstOrNull()
                ?: return sendDefaultHelp(context)

            sendCommandHelp(context, cmd)
        } else {
            val category = CommandCategory.values()[categoryIndex - 1]
            val builder = StringBuilder()

            for (cmd in commandsByCategory(category)) {
                builder.append("**`")
                    .append(Helpers.pad(cmd.name.lowercase()))
                    .append(":`** ")
                    .append(cmd.properties.description)
                    .append("\n")
            }

            context.embed {
                setTitle("Help for **$category**")
                setDescription("[View more information here](${Constants.WEBSITE}/docs)\n$builder")
                setFooter("You can use ${context.prefix}help <command> to view additional command information", null)
            }
        }
    }

    private fun sendDefaultHelp(ctx: Context) {
        ctx.embed {
            setColor(ctx.embedColor)
            setTitle("${Constants.BOT_NAME} Help Menu")
            setDescription("Get started by joining a voicechannel and sending `${ctx.prefix}play <query>`!")
            addField("Categories", categories, true)
            addField("Links", "[Discord](${Constants.HOME_SERVER}) | [Website](${Constants.WEBSITE})", false)
            setFooter("Select a category with ${ctx.prefix}help <number>", null)
        }
    }

    private fun sendCommandHelp(context: Context, cmd: Command) {
        val aliases = cmd.properties.aliases
        val aliasString = if (aliases.isEmpty()) "None" else aliases.joinToString(", ")

        context.embed(
            "Help for **${cmd.name}**",
            "**Aliases:** $aliasString\n**Description:** ${cmd.properties.description}"
        )
    }

    private fun commandsByCategory(category: CommandCategory) =
        CommandHandler.commands
            .values
            .filter { it.properties.category == category }
            .sortedBy { it.name }
}
