package me.devoxin.jukebot.commands.misc

import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context
import me.devoxin.jukebot.utils.Constants

@CommandProperties(description = "Displays the bot's invite URL")
class Invite : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        context.embed(
            "Invite Links",
            "[**Add ${Constants.BOT_NAME}**](${Constants.DEFAULT_INVITE_URL})\n" +
                "[**Get Support**](${Constants.HOME_SERVER})"
        )
    }
}
