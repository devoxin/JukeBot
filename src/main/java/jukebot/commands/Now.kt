package jukebot.commands

import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.toTimeString

@CommandProperties(description = "Displays the currently playing track", aliases = ["n", "np"], category = CommandProperties.category.MEDIA)
class Now : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {

        val player = JukeBot.getPlayer(context.guild.audioManager)

        if (!player.isPlaying) {
            context.embed("Not Playing", "Nothing is currently playing.")
            return
        }

        val current = player.player.playingTrack
        val duration = if (current.info.isStream) "LIVE" else current.duration.toTimeString()

        val requesterId = current.userData.toString().toLong()
        val requester = JukeBot.shardManager.getUserById(requesterId)
        val requesterInfo = if (requester != null) "• Queued by ${requester.asTag}" else ""

        val playbackSettings = "Shuffle: ${if (player.isShuffleEnabled) "On" else "Off"}" +
                " • Repeat: ${player.repeatString} $requesterInfo"

        val isYouTubeTrack = current.sourceManager.sourceName == "youtube"
        val trackMarker = if (isYouTubeTrack) {
            "[(${current.position.toTimeString()}/$duration)](${current.info.uri}&t=${current.position / 1000}s)"
        } else {
            "${current.position.toTimeString()}/$duration"
        }

        context.embed {
            setTitle("Now Playing")
            setDescription("**[${current.info.title}](${current.info.uri})**\n$trackMarker")
            //setThumbnail(current.info.artworkUri)
            setFooter(playbackSettings, null)
        }

    }
}
