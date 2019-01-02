package jukebot.commands

import jukebot.Database
import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context

@CommandProperties(description = "Developer menu", category = CommandProperties.category.MISC, developerOnly = true)
class Dev : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {

        val args = context.args

        if (args[0] == "preload") {
            if (JukeBot.isSelfHosted) {
                return context.embed("Command Unavailable", "This command is unavailable on self-hosted JukeBot.")
            }
            if (args.size < 2) {
                context.embed("Missing Required Arg", "You need to specify `key`")
            } else {
                JukeBot.createPatreonApi(args[1])
                context.message.addReaction("\uD83D\uDC4C").queue()
            }
        } else if (args[0] == "block") {
            if (args.size < 2) {
                context.embed("Missing Required Arg", "You need to specify `userId`")
            } else {
                Database.blockUser(java.lang.Long.parseLong(args[1]))
                context.embed("User Blocked", "${args[1]} is now blocked from using JukeBot.")
            }
        } else if (args[0] == "unblock") {
            if (args.size < 2) {
                context.embed("Missing Required Arg", "You need to specify `userId`")
            } else {
                Database.unblockUser(java.lang.Long.parseLong(args[1]))
                context.embed("User Unblocked", "${args[1]} can now use JukeBot.")
            }
        } else if (args[0] == "fdc") {
            context.guild.audioManager.closeAudioConnection()
            context.message.addReaction("\uD83D\uDC4C").queue()
        } else {
            context.embed("Dev Subcommands", "`->` preload <key>\n`->` block <userId>\n`->` unblock <userId>\n`->` fdc")
        }
    }

}
