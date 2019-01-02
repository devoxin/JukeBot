package jukebot.commands

import jukebot.Database
import jukebot.audio.AudioHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Permissions

@CommandProperties(description = "Vote to skip the track", category = CommandProperties.category.CONTROLS)
class Skip : Command(ExecutionType.REQUIRE_MUTUAL) {

    private val permissions = Permissions()

    override fun execute(context: Context) {

        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            context.embed("Not Playing", "Nothing is currently playing.")
            return
        }

        if (!permissions.ensureMutualVoiceChannel(context.member)) {
            context.embed("No Mutual VoiceChannel", "Join my VoiceChannel to use this command.")
            return
        }

        val totalVotes = player.voteSkip(context.author.idLong)
        val voteThreshold = Database.getSkipThreshold(context.guild.idLong)

        val neededVotes = Math.ceil(context.guild.audioManager.connectedChannel
                .members
                .stream()
                .filter { u -> !u.user.isBot }
                .count() * voteThreshold).toInt()

        if (neededVotes - totalVotes <= 0) {
            player.playNext()
        } else {
            context.embed("Vote Acknowledged", (neededVotes - totalVotes).toString() + " votes needed to skip.")
        }
    }
}