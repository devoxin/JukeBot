package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.utils.addFields
import net.dv8tion.jda.api.entities.MessageEmbed

@CommandProperties(aliases = ["donate"], description = "Provides a link to JukeBot's Patreon")
class Patreon : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val fields = arrayOf(
                MessageEmbed.Field("Tier 1 ($1)", "-> Donor Role in [JukeBot's Server](https://discord.gg/xvtH2Yn)\n" +
                        "-> Ability to queue songs up to 5 hours long\n" +
                        "-> Ability to queue up to 1000 songs from a playlist\n" +
                        "-> Ability to queue livestreams", false),
                MessageEmbed.Field("Tier 2 ($2)", "-> Donor Role in [JukeBot's Server](https://discord.gg/xvtH2Yn)\n" +
                        "-> Removed song duration limit\n" +
                        "-> Removed playlist importing limit\n" +
                        "-> Spotify playlist support", false)
        )

        context.embed {
            setTitle("Become a Patron!", "https://patreon.com/Devoxin")
            addFields(fields)
        }
    }

}
