package jukebot.commands.misc

import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.utils.Constants

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
