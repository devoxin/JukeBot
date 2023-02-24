package me.devoxin.jukebot.commands.misc

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.ArgumentResolver
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context

@CommandProperties(description = "Developer menu", developerOnly = true)
class Dev : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        when (context.args.next("setting", ArgumentResolver.STRING)) {
            "preload" -> {
                if (JukeBot.isSelfHosted) {
                    return context.embed("Command Unavailable", "This command is unavailable on self-hosted JukeBot.")
                }

                val patreonKey = context.args.next("key", ArgumentResolver.STRING)
                    ?: return context.embed("Missing Required Arg", "You need to specify `key`")

                JukeBot.patreonApi.accessToken = patreonKey
                context.react("\uD83D\uDC4C")
            }
            "block" -> {
                val userId = context.args.next("userId", ArgumentResolver.LONG)
                    ?: return context.embed("Missing Required Arg", "You need to specify `userId`")

                Database.setIsBlocked(userId, true)
                context.embed("User Blocked", "$userId is now blocked from using JukeBot.")
            }
            "unblock" -> {
                val userId = context.args.next("userId", ArgumentResolver.LONG)
                    ?: return context.embed("Missing Required Arg", "You need to specify `userId`")

                Database.setIsBlocked(userId, false)
                context.embed("User Unblocked", "$userId can now use JukeBot.")
            }
            else -> context.embed("Dev Subcommands", "`->` preload <key>\n`->` block <userId>\n`->` unblock <userId>")
        }
    }
}
