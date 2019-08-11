package jukebot.commands

import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.utils.toTimeString

@CommandProperties(description = "Displays the currently playing track", aliases = ["n", "np"], category = CommandCategory.QUEUE)
class Now : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val player = JukeBot.getPlayer(context.guild.idLong)

        if (!player.isPlaying) {
            context.embed("Not Playing", "Nothing is currently playing.")
            return
        }

        val current = player.player.playingTrack
        val duration = if (current.info.isStream) "LIVE" else current.duration.toTimeString()

        val requesterId = current.userData.toString().toLong()
        val requester = JukeBot.shardManager.getUserById(requesterId)
        val requesterInfo = if (requester != null) "• Queued by ${requester.asTag}" else ""

        val playbackSettings = "Shuffle: ${if (player.shuffle) "On" else "Off"}" +
                " • Repeat: ${player.repeat.humanized()} $requesterInfo"

        val isYouTubeTrack = current.sourceManager.sourceName == "youtube"
        val trackMarker = if (isYouTubeTrack) {
            "[(${current.position.toTimeString()}/$duration)](${current.info.uri}&t=${current.position / 1000}s)"
        } else {
            "${current.position.toTimeString()}/$duration"
        }

        context.embed {
            setTitle("Now Playing")
            setDescription("**[${current.info.title}](${current.info.uri})**\n$trackMarker")
            setFooter(playbackSettings, null)
        }
    }
}
