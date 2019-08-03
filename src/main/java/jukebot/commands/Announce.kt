package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Configure track announcements", category = CommandProperties.category.MEDIA)
class Announce : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        if (!context.isDJ(false)) {
            context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
            return
        }

        val player = context.getAudioPlayer()
        val args = context.args

        when {
            args[0] == "here" -> {
                player.channelId = context.channel.idLong
                player.shouldAnnounce = true
                context.embed("Track Announcements", "This channel will now be used to post track announcements")
            }
            args[0] == "off" -> {
                player.shouldAnnounce = false
                context.embed("Track Announcements", "Track announcements are now disabled for this server")
            }
            else -> context.embed("Track Announcements", "`here` - Uses the current channel for track announcements\n`off` - Disables track announcements")
        }
    }
}