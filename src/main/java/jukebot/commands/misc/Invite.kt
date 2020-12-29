package jukebot.commands.misc

import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Displays the bot's invite URL")
class Invite : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        context.embed("Invite Links",
            "[**Add JukeBot**](https://discordapp.com/oauth2/authorize?permissions=36793345&scope=bot&client_id=249303797371895820)\n" +
                "[**Get Support**](https://discord.gg/xvtH2Yn)")
    }
}
