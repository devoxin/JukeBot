package jukebot.commands

import jukebot.Database
import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Developer menu", developerOnly = true)
class Dev : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        when (context.args.firstOrNull()) {
            "preload" -> {
                if (JukeBot.isSelfHosted) {
                    return context.embed("Command Unavailable", "This command is unavailable on self-hosted JukeBot.")
                }

                if (context.args.size < 2) {
                    return context.embed("Missing Required Arg", "You need to specify `key`")
                }

                JukeBot.patreonApi.setAccessToken(context.args[1])
                context.message.addReaction("\uD83D\uDC4C").queue()
            }
            "block" -> {
                if (context.args.size < 2) {
                    return context.embed("Missing Required Arg", "You need to specify `userId`")
                }

                Database.blockUser(context.args[1].toLong())
                context.embed("User Blocked", "${context.args[1]} is now blocked from using JukeBot.")
            }
            "unblock" -> {
                if (context.args.size < 2) {
                    return context.embed("Missing Required Arg", "You need to specify `userId`")
                }

                Database.unblockUser(context.args[1].toLong())
                context.embed("User Unblocked", "${context.args[1]} can now use JukeBot.")
            }
            else -> {
                context.embed("Dev Subcommands", "`->` preload <key>\n`->` block <userId>\n`->` unblock <userId>")
            }
        }
    }

}
