package jukebot.commands.controls

import jukebot.framework.*

@CommandProperties(description = "Configure track announcements", category = CommandCategory.CONTROLS)
@CommandChecks.Dj(alone = false)
class Announce : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        when (context.args.firstOrNull()?.lowercase()) {
            "here" -> {
                player.channelId = context.channel.idLong
                player.shouldAnnounce = true
                context.embed("Track Announcements", "This channel will now be used to post track announcements")
            }
            "off" -> {
                player.shouldAnnounce = false
                context.embed("Track Announcements", "Track announcements are now disabled for this server")
            }
            else -> context.embed(
                "Track Announcements",
                "`here` - Uses the current channel for track announcements\n`off` - Disables track announcements"
            )
        }
    }
}
