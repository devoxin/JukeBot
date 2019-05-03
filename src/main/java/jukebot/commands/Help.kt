package jukebot.commands

import jukebot.CommandHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.toTitleCase

@CommandProperties(description = "Displays all commands", aliases = ["commands"], category = CommandProperties.category.MISC)
class Help : Command(ExecutionType.STANDARD) {

    private val categories = CommandProperties.category.values()
            .mapIndexed { i, e -> "`${i + 1}.` ${e.toString().toLowerCase().toTitleCase()}" }
            .joinToString("\n")

    override fun execute(context: Context) {
        val menu = context.args[0].toIntOrNull() ?: 0

        if (menu <= 0 || menu > CommandProperties.category.values().size) {
            val cmd = CommandHandler.commands
                    .filter { it.key == context.args[0] || it.value.properties().aliases.contains(context.args[0]) }
                    .values
                    .firstOrNull()
                    ?: return sendDefaultHelp(context)

            sendCommandHelp(context, cmd)
        } else {
            val category = CommandProperties.category.values()[menu - 1]
            val builder = StringBuilder()

            for (cmd in commandsByCategory(category)) {
                builder.append("**`")
                        .append(String.format("%-11s", cmd.name().toLowerCase()).replace(" ", " \u200B"))
                        .append(":`** ")
                        .append(cmd.properties().description)
                        .append("\n")
            }

            context.embed {
                setTitle("Help for **$category**")
                setDescription("[View more information here](https://jukebot.serux.pro/documentation)\n$builder")
                setFooter("You can use ${context.prefix}help <command> to view additional command information", null)
            }
        }
    }

    fun sendDefaultHelp(ctx: Context) {
        ctx.embed {
            setColor(ctx.embedColor)
            setTitle("JukeBot Help Menu")
            setDescription("Get started by joining a voicechannel and sending `${ctx.prefix}play <query>`!")
            addField("Categories", categories, true)
            addField("Links", "[Discord](https://discord.gg/xvtH2Yn) | [Website](https://jukebot.serux.pro)", true)
            setFooter("Select a category with ${ctx.prefix}help <number>", null)
        }
    }

    fun sendCommandHelp(context: Context, cmd: Command) {
        val aliases = cmd.properties().aliases
        val aliasString = if (aliases.isEmpty()) "None" else aliases.joinToString(", ")

        context.embed("Help for **${cmd.name()}**",
                "**Aliases:** $aliasString\n**Description:** ${cmd.properties().description}")
    }

    private fun commandsByCategory(category: CommandProperties.category) =
            CommandHandler.commands
                    .values
                    .filter { it.properties().category == category }
                    .sortedBy { it.name() }

}
