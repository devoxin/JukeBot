package me.devoxin.jukebot.commands.queue

import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.*
import me.devoxin.jukebot.utils.Constants
import me.devoxin.jukebot.utils.Helpers
import me.devoxin.jukebot.utils.toTimeString

@CommandProperties(description = "Displays the currently playing track", aliases = ["n", "np"], category = CommandCategory.QUEUE)
@CommandChecks.Playing
class Now : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val player = JukeBot.getPlayer(context.guild.idLong)

        val current = player.player.playingTrack
        val duration = if (current.info.isStream) "LIVE" else current.duration.toTimeString()

        val requesterId = current.userData as Long
        val requesterInfo = JukeBot.shardManager.getUserById(requesterId)?.let { "• Queued by ${it.asTag}" } ?: ""

        val playbackSettings = "Shuffle: ${if (player.shuffle) "On" else "Off"} " +
            "• Repeat: ${player.repeat.humanized()} $requesterInfo"

        val isYouTubeTrack = current.sourceManager.sourceName == "youtube"
        val trackMarker = "${current.position.toTimeString()}/$duration"

        val timeLink = if (isYouTubeTrack) {
            "${current.info.uri}&t=${current.position / 1000}s"
        } else {
            Constants.WEBSITE
        }

        context.embed {
            setTitle(current.info.title, current.info.uri)
            setDescription("${createBar(current.info.length, current.position, timeLink)} ($trackMarker)")
            setFooter(playbackSettings, null)
        }
    }

    private fun createBar(m: Long, v: Long, l: String) = Helpers.createBar(v.toInt(), m.toInt(), 10, link = l)
}
