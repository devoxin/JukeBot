package me.devoxin.jukebot.commands.misc

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context

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

                JukeBot.patreonApi.accessToken = context.args[1]
                context.react("\uD83D\uDC4C")
            }
            "block" -> {
                if (context.args.size < 2) {
                    return context.embed("Missing Required Arg", "You need to specify `userId`")
                }

                Database.setIsBlocked(context.args[1].toLong(), true)
                context.embed("User Blocked", "${context.args[1]} is now blocked from using JukeBot.")
            }
            "unblock" -> {
                if (context.args.size < 2) {
                    return context.embed("Missing Required Arg", "You need to specify `userId`")
                }

                Database.setIsBlocked(context.args[1].toLong(), false)
                context.embed("User Unblocked", "${context.args[1]} can now use JukeBot.")
            }
            else -> {
                context.embed("Dev Subcommands", "`->` preload <key>\n`->` block <userId>\n`->` unblock <userId>")
            }
        }
    }
}
