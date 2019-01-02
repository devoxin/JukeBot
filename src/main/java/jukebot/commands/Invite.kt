package jukebot.commands

import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context

@CommandProperties(description = "Displays the bot's invite URL", category = CommandProperties.category.MISC)
class Invite : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        context.embed("Invite Links",
                "[**Add JukeBot**](https://discordapp.com/oauth2/authorize?permissions=36793345&scope=bot&client_id=249303797371895820)\n" +
                        "[**Get Support**](https://discord.gg/xvtH2Yn)")
    }
}
